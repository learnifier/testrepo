/*
 * (c) Dabox AB 2013 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.module.project.report;

import java.util.ArrayList;
import java.util.Collections;
import java.util.TreeMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.unixdeveloper.druwa.ServiceRequestCycle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.dabox.cocobox.crisp.runtime.CrispErrorException;
import se.dabox.cocobox.crisp.runtime.CrispException;
import se.dabox.cocosite.coursedesign.GetDatabankFacadeCommand;
import se.dabox.cocosite.infocache.InfoCacheHelper;
import se.dabox.cocosite.user.MiniUserInfo;
import se.dabox.service.client.CacheClients;
import se.dabox.service.common.DwsConstants;
import se.dabox.service.common.ajaxlongrun.Status;
import se.dabox.service.common.ajaxlongrun.StatusSource;
import se.dabox.service.common.ccbc.CocoboxCoordinatorClient;
import se.dabox.service.common.ccbc.NotFoundException;
import se.dabox.service.common.ccbc.ParticipationProgress;
import se.dabox.service.common.ccbc.participation.crisppart.ParticipationCrispProductStatus;
import se.dabox.service.common.ccbc.project.OrgProject;
import se.dabox.service.common.ccbc.project.ProjectParticipation;
import se.dabox.service.common.ccbc.project.material.FetchMode;
import se.dabox.service.common.ccbc.project.material.GetParticipationCrispProductStatusRequest;
import se.dabox.service.common.ccbc.project.material.ProjectMaterialCoordinatorClient;
import se.dabox.service.common.coursedesign.ComponentUtil;
import se.dabox.service.common.coursedesign.DatabankFacade;
import se.dabox.service.common.coursedesign.activity.Activity;
import se.dabox.service.common.coursedesign.activity.ActivityComponent;
import se.dabox.service.common.coursedesign.activity.ActivityCourse;
import se.dabox.service.common.coursedesign.activity.MultiPageCourseCddActivityCourseFactory;
import se.dabox.service.common.coursedesign.extstatus.ExtendedStatusFactory;
import se.dabox.service.common.coursedesign.progress.ProgressType;
import se.dabox.service.common.coursedesign.project.GetProjectCourseDesignCommand;
import se.dabox.service.common.coursedesign.v1.CourseDesignDefinition;
import se.dabox.service.common.json.JsonUtils;
import se.dabox.service.proddir.data.ProductId;
import se.dabox.util.ParamUtil;
import se.dabox.util.collections.CollectionsUtil;
import se.dabox.util.collections.Transformer;

/**
 *
 * @author Jerker Klang (jerker.klang@dabox.se)
 */
class ActivityReportBuilder implements StatusSource {

    private static final Logger LOGGER = LoggerFactory.getLogger(ActivityReportBuilder.class);

    private final ServiceRequestCycle cycle;
    private final OrgProject project;
    private final CourseDesignDefinition cdd;
    private final InfoCacheHelper icHelper;
    private final CocoboxCoordinatorClient ccbcClient;
    private final ProjectMaterialCoordinatorClient pmcClient;
    private final DatabankFacade databankFacade;
    private final ExtendedStatusFactory extendedStatusFactory =
            new ExtendedStatusFactory();

    private List<ProjectParticipation> participants;
    private volatile int completed;
    private volatile String statusMessage;

    private final Transformer<Long,List<ParticipationProgress>> progressFactory;
    private Map<Long, List<ParticipationProgress>> progressData;

    public ActivityReportBuilder(ServiceRequestCycle cycle, OrgProject project, Locale userLocale) {
        ParamUtil.required(cycle, "cycle");
        ParamUtil.required(project, "project");

        this.cycle = cycle;
        this.project = project;

        cdd = new GetProjectCourseDesignCommand(cycle, userLocale).
                setFallbackToStageDesign(true).
                forProject(project);

        icHelper = InfoCacheHelper.getInstance(cycle);

        ccbcClient = CacheClients.getClient(cycle, CocoboxCoordinatorClient.class);
        pmcClient = CacheClients.getClient(cycle, ProjectMaterialCoordinatorClient.class);

        databankFacade = new GetDatabankFacadeCommand(cycle).get(project);

        progressFactory = this::getParticipationProgress;
    }

    byte[] getReport() {
        statusMessage = "Loading participation information";
        loadParticipationData();
        refreshCrispInformation();
        loadProgressInformation();

        List<Map<String, Object>> rows = new ArrayList<>(participants.size());

        completed = 0;
        for (ProjectParticipation participant : participants) {
            statusMessage = "Generating report";
            Map<String, Object> row = getParticipantRowData(participant);
            if (row != null) {
                rows.add(row);
            }
            completed++;
        }

        Map<String, Object> retval = new TreeMap<>();
        retval.put("aaData", rows);

        String jsonString = JsonUtils.encodePretty(retval);
        return jsonString.getBytes(DwsConstants.UTF8_CHARSET);
    }

    private void loadParticipationData() throws NotFoundException {
        participants
                = ccbcClient.listProjectParticipations(project.getProjectId());

        //Prime user info details
        Set<Long> userIds = CollectionsUtil.transform(participants, ProjectParticipation::getUserId);
        icHelper.getMiniUserInfos(userIds);
    }

    private Map<String, Object> getParticipantRowData(ProjectParticipation participant) {
        Map<String, Object> map = new TreeMap<>();
        MiniUserInfo userInfo = icHelper.getMiniUserInfo(participant.getUserId());

        if (userInfo == null) {
            LOGGER.warn("Failed to get user {} from participation {}. Ignoring participation",
                    participant.getUserId(), participant.getParticipationId());
            return null;
        }
        map.put("participationId", participant.getParticipationId());
        map.put("userId", participant.getUserId());
        map.put("displayName", userInfo.getDisplayName());
        map.put("email", userInfo.getEmail());
        map.put("userThumbnail", userInfo.getThumbnail());

        boolean activated = participant.isActivated();

        map.put("activated", activated);

        if (!activated) {
            return map;
        }

        List<ParticipationProgress> progress
                = progressFactory.transform(participant.getParticipationId());

        ActivityCourse activityCourse
                = new MultiPageCourseCddActivityCourseFactory().newActivityCourse(project, progress,
                        databankFacade, cdd);

        List<Map<String,Object>> activities = new ArrayList<>(activityCourse.getActivityCount());

        for (Activity activity : activityCourse.getActivityList()) {
            Map<String,Object> activityMap = getActivityMap(participant, activity);
            if (activityMap != null) {
                activities.add(activityMap);
            }
        }

        map.put("activities", activities);

        return map;
    }

    private Map<String, Object> getActivityMap(ProjectParticipation participant, Activity activity) {
        //Don't report progress for activities that don't have any
        if (activity.getProgressTrackingType() == ProgressType.NONE) {
            return null;
        }

        Map<String,Object> map = new TreeMap<>();

        map.put("index", activity.getIndex());

        map.put("title", getActivityProperty(activity, "title"));

        ActivityComponent pComp = activity.getPrimaryComponent();
        if (pComp != null) {
            ProductId productId = ComponentUtil.
                    getProductId(pComp.getBasetype(), pComp.getSubtype());
            if (productId != null) {
                map.put("productId", productId.getId());
            }
        }

        boolean activityCompleted = activity.isCompleted();
        map.put("completed", activityCompleted);
        map.put("enabled", activity.isEnabled());
        map.put("visible", activity.isVisible());
        map.put("completionStatus", activity.getCompletionStatus());

        ActivityComponent primaryComp = activity.getPrimaryComponent();

        boolean overdue = false;

        int progressPercent  = activityCompleted ? 100 : 0;

        //Only try to fetch progress if we're uncompleted, otherwise assume 100% (since we're complete).
        if (!activityCompleted && primaryComp != null) {
            overdue = activity.isOverdue();

            Integer primaryProgressPercent = getProgressPercent(participant, activity, primaryComp);

            if (primaryProgressPercent != null) {
                progressPercent = primaryProgressPercent;
            }
        }

        map.put("overdue", overdue);
        map.put("progressPercent", progressPercent);
        map.put("extendedStatus", extendedStatusFactory.statusFor(activity));

        return map;
    }

    private Object getActivityProperty(Activity activity, String propertyName) {
        String title = activity.getActivityContainer().getProperties().get(propertyName);

        if (title != null) {
            return title;
        }

        ActivityComponent pcomp = activity.getPrimaryComponent();

        if (pcomp == null) {
            //TODO: Check the container
            return "Primary Component not detected";
        }

        title = pcomp.getProperties().get(propertyName);

        if (title == null) {
            for (ActivityComponent activityComponent : activity.getSecondaryComponents()) {
                title = activityComponent.getProperties().get(propertyName);
                if (title != null) {
                    break;
                }
            }
        }

        return title;
    }

    private Integer getProgressPercent(ProjectParticipation participant, Activity activity,
            ActivityComponent component) {

        if (component.getProgressTrackingType() != ProgressType.INTEGRATION) {
            return null;
        }

        ProductId productId
                = ComponentUtil.getProductId(component.getBasetype(), component.getSubtype());

        if (productId == null) {
            return null;
        }

        GetParticipationCrispProductStatusRequest req
                = new GetParticipationCrispProductStatusRequest(participant.getParticipationId(),
                        productId.getId());
        req.setFetchMode(FetchMode.DIRECT);

        boolean retry = false;

        ParticipationCrispProductStatus status = null;
        try {
            List<ParticipationCrispProductStatus> statuses
                    = pmcClient.getParticipationCrispProductStatus(req);
            status = CollectionsUtil.singleItemOrNull(statuses);
        } catch (NotFoundException nfe) {
            //Participation not available
        } catch(CrispErrorException crex) {
            LOGGER.warn(
                    "Failed to get information. Got a crisp exception. Trying to use cached result. Product: {}, error: {}",
                    crex.getProductId(),
                    crex.getError());
            retry = true;
        } catch(CrispException crex) {
            LOGGER.warn(
                    "Failed to get information. Got a crisp exception. Trying to use cached result. Product: {}",
                    crex.getProductId());
            retry = true;
        }

        if (retry) {
            LOGGER.debug("Trying to fetch local result");
            req.setFetchMode(FetchMode.CACHED);
            List<ParticipationCrispProductStatus> statuses = null;
            try {
                statuses
                        = pmcClient.getParticipationCrispProductStatus(req);
            } catch (CrispException crispException) {
                LOGGER.warn(
                        "Still getting an error while trying to get cached result. Progress will be unknown for product {}",
                        crispException.getProductId());
            } catch (NotFoundException notFoundException) {
                //Participation not available
            }
            status = CollectionsUtil.singleItemOrNull(statuses);
        }

        if (status == null) {
            return null;
        }

        return status.getProgress();
    }

    @Override
    public Status getStatus() {
        if (participants == null) {
            return null;
        }

        return new Status(statusMessage, (long) participants.size(), (long) completed);
    }

    private void refreshCrispInformation() {
        completed = 0;

        for (ProjectParticipation participant : participants) {
            statusMessage = String.format("Fetching status information for %s", icHelper.
                    getUserDisplayName(participant.getUserId()));
            GetParticipationCrispProductStatusRequest req
                    = new GetParticipationCrispProductStatusRequest(participant.getParticipationId());
            req.setFetchMode(FetchMode.DIRECT);
            try {
                pmcClient.getParticipationCrispProductStatus(req);
            } catch (NotFoundException nfe) {
                LOGGER.warn("Participation not available (participation id {})", participant.
                        getParticipationId());
            } catch (Exception ex) {
                LOGGER.warn("Failed to get fresh information for participation {}", participant, ex);
            }
            completed++;
        }
    }

    private void loadProgressInformation() {
        progressData = ccbcClient.getProjectProgress(project.getProjectId());
    }

    private List<ParticipationProgress> getParticipationProgress(long participationId) {
        return progressData.getOrDefault(participationId, Collections.emptyList());
    }
}
