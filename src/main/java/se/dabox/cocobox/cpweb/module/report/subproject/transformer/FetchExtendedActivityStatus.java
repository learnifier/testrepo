/*
 * (c) Dabox AB 2015 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.module.report.subproject.transformer;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import net.unixdeveloper.druwa.DruwaService;
import net.unixdeveloper.druwa.ServiceRequestCycle;
import se.dabox.cocobox.cpweb.module.report.StatusHolder;
import se.dabox.cocosite.coursedesign.GetDatabankFacadeCommand;
import se.dabox.service.client.CacheClients;
import se.dabox.service.common.ajaxlongrun.Status;
import se.dabox.service.common.ccbc.CocoboxCoordinatorClient;
import se.dabox.service.common.ccbc.ParticipationProgress;
import se.dabox.service.common.ccbc.project.OrgProject;
import se.dabox.service.common.coursedesign.DatabankFacade;
import se.dabox.service.common.coursedesign.activity.MultiPageActivityCourse;
import se.dabox.service.common.coursedesign.activity.MultiPageCourseCddActivityCourseFactory;
import se.dabox.service.common.coursedesign.extstatus.ExtendedStatus;
import se.dabox.service.common.coursedesign.extstatus.ExtendedStatusFactory;
import se.dabox.service.common.coursedesign.project.GetProjectCourseDesignCommand;
import se.dabox.service.common.coursedesign.v1.CourseDesignDefinition;
import se.dabox.util.collections.CollectionsUtil;
import se.dabox.util.collections.Factory;

/**
 *
 * @author Jerker Klang (jerker.klang@learnifier.com)
 */
public class FetchExtendedActivityStatus implements Factory<List<SubprojectParticipant>> {

    private final StatusHolder statusHolder;
    private final Factory<List<SubprojectParticipant>> backend;
    private final CocoboxCoordinatorClient ccbc;
    private final ServiceRequestCycle cycle;

    private long total;
    private long processed;

    public FetchExtendedActivityStatus(StatusHolder statusHolder,
            Factory<List<SubprojectParticipant>> backend) {
        this.statusHolder = statusHolder;
        this.backend = backend;
        cycle = DruwaService.getCurrentCycle();
        ccbc = CacheClients.getClient(cycle, CocoboxCoordinatorClient.class);
    }

    @Override
    public List<SubprojectParticipant> create() {

        List<SubprojectParticipant> list = backend.create();

        total = list.size();

        Map<Long, List<SubprojectParticipant>> projectParticipantMap
                = CollectionsUtil.createMapList(list, SubprojectParticipant::getProjectId);

        projectParticipantMap.forEach(this::processProject);

        return list;
    }

    private void processProject(long projectId, List<SubprojectParticipant> participants) {
        statusHolder.setStatus(new Status("Fetching participant progress", processed, total));
        Map<Long, List<ParticipationProgress>> progress = ccbc.getProjectProgress(projectId);

        OrgProject project = ccbc.getProject(projectId);

        CourseDesignDefinition cdd
                = new GetProjectCourseDesignCommand(cycle, null).setFallbackToStageDesign(true).
                forProjectId(projectId);

        DatabankFacade databank = new GetDatabankFacadeCommand(cycle).setFallbackToStageDatabank(
                true).get(project);

        participants.forEach((subp) -> {
            List<ParticipationProgress> progressData
                    = progress.getOrDefault(subp.getParticipationId(), Collections.emptyList());
            processParticipant(project, databank, cdd, progressData, subp);
        });

        processed += participants.size();
    }

    private void processParticipant(OrgProject project, DatabankFacade databank,
            CourseDesignDefinition cdd, List<ParticipationProgress> progress,
            SubprojectParticipant part) {

        MultiPageActivityCourse actCourse
                = new MultiPageCourseCddActivityCourseFactory().
                setAllowTemporalProgressTracking(true).
                newActivityCourse(project, progress, databank, cdd);

        part.setActivity(CollectionsUtil.transformList(actCourse.getActivityList(), (a) -> {
            ExtendedStatus status = new ExtendedStatusFactory().statusFor(a);
            String name = a.getTitle();

            return new ReportActivityExtendedStatus(name, status);
        }));
    }

}
