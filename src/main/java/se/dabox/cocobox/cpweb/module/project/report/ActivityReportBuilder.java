/*
 * (c) Dabox AB 2013 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.module.project.report;

import java.util.ArrayList;
import java.util.TreeMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.unixdeveloper.druwa.ServiceRequestCycle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.dabox.cocosite.coursedesign.GetDatabankFacadeCommand;
import se.dabox.cocosite.coursedesign.GetProjectCourseDesignCommand;
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
import se.dabox.service.common.coursedesign.activity.CourseDesignDefinitionActivityCourseFactory;
import se.dabox.service.common.coursedesign.activity.MultiPageCourseCddActivityCourseFactory;
import se.dabox.service.common.coursedesign.progress.ProgressType;
import se.dabox.service.common.coursedesign.v1.CourseDesignDefinition;
import se.dabox.service.common.json.JsonUtils;
import se.dabox.service.proddir.data.ProductId;
import se.dabox.util.ParamUtil;
import se.dabox.util.collections.CollectionsUtil;

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
    
    private List<ProjectParticipation> participants;
    private volatile int completed;

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
    }

    byte[] getReport() {
        participants
                = ccbcClient.listProjectParticipations(project.getProjectId());

        List<Map<String, Object>> rows = new ArrayList<>(participants.size());

        for (ProjectParticipation participant : participants) {
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
                = ccbcClient.getParticipationProgress(participant.getParticipationId());



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

        ActivityComponent primaryComp = activity.getPrimaryComponent();

        boolean overdue = false;

        int progressPercent  = activityCompleted ? 100 : 0;

        if (primaryComp != null) {
            overdue = primaryComp.getDueDateInfo().isOverdue();

            Integer primaryProgressPercent = getProgressPercent(participant, activity, primaryComp);

            if (primaryProgressPercent != null) {
                progressPercent = primaryProgressPercent;
            }
        }

        map.put("overdue", overdue);        
        map.put("progressPercent", progressPercent);

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
        req.setFetchMode(FetchMode.CACHED);

        ParticipationCrispProductStatus status = null;
        try {
            List<ParticipationCrispProductStatus> statuses
                    = pmcClient.getParticipationCrispProductStatus(req);
            status = CollectionsUtil.singleItemOrNull(statuses);
        } catch (NotFoundException nfe) {
            //Participation not available
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

        return new Status((long) participants.size(), (long) completed);
    }
}
