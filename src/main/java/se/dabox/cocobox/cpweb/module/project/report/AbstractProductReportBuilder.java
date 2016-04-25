/*
 * (c) Dabox AB 2013 All Rights Reserved
 */

package se.dabox.cocobox.cpweb.module.project.report;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import net.unixdeveloper.druwa.ServiceRequestCycle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.dabox.cocosite.coursedesign.GetDatabankFacadeCommand;
import se.dabox.cocosite.coursedesign.GetProjectCourseDesignCommand;
import se.dabox.cocosite.infocache.InfoCacheHelper;
import se.dabox.service.client.CacheClients;
import se.dabox.service.common.DwsConstants;
import se.dabox.service.common.ccbc.CocoboxCoordinatorClient;
import se.dabox.service.common.ccbc.ParticipationProgress;
import se.dabox.service.common.ccbc.project.OrgProject;
import se.dabox.service.common.ccbc.project.ProjectParticipation;
import se.dabox.service.common.ccbc.project.ProjectSubtypeConstants;
import se.dabox.service.common.ccbc.project.ProjectTypeCallable;
import se.dabox.service.common.ccbc.project.ProjectTypeUtil;
import se.dabox.service.common.coursedesign.DatabankFacade;
import se.dabox.service.common.coursedesign.v1.CourseDesignDefinition;
import se.dabox.service.common.json.JsonUtils;
import se.dabox.util.ParamUtil;

/**
 *
 * @author Jerker Klang (jerker.klang@dabox.se)
 * @param <P>
 */
public abstract class AbstractProductReportBuilder<P> {
    private static final Charset CHARSET = DwsConstants.UTF8_CHARSET;

    private static final Logger LOGGER =
            LoggerFactory.getLogger(AbstractProductReportBuilder.class);

    protected final ServiceRequestCycle cycle;

    protected final CocoboxCoordinatorClient ccbcClient;
    protected final InfoCacheHelper icHelper;
    protected final LoadingCache<Long, List<ParticipationProgress>> progressCache;

    private volatile long processed;

    public AbstractProductReportBuilder(ServiceRequestCycle cycle) {
        ParamUtil.required(cycle, "cycle");
        this.cycle = cycle;

        ccbcClient = CacheClients.getClient(cycle, CocoboxCoordinatorClient.class);
        icHelper = InfoCacheHelper.getInstance(cycle);

        progressCache = CacheBuilder.newBuilder().maximumSize(20).build(
                CacheLoader.from(ccbcClient::getParticipationProgress));
    }

    public byte[] getReportForProject(OrgProject project) {
        List<ProjectParticipation> participants
                = ccbcClient.listProjectParticipations(project.getProjectId());

        return getReportForParticipants(participants.iterator(), false);
    }

    public byte[] getReportForOrgUnit(long ouId) {
        Iterator<ProjectParticipation> participants = new OrgUnitProjectIterator(cycle, ouId);

        return getReportForParticipants(participants, true);
    }


    public byte[] getReportForParticipants(Iterator<ProjectParticipation> participantIterator,
            final boolean ignoreIdProject) {
        LoadingCache<Long, ProjectData> dataCache = CacheBuilder.newBuilder().maximumSize(100).
                expireAfterAccess(2, TimeUnit.MINUTES).
                build(CacheLoader.from(this::loadProjectData));

        List<P> partData = new ArrayList<>();

        while (participantIterator.hasNext()) {
            processed++;
            ProjectParticipation participant = participantIterator.next();

            if (participant == null) {
                continue;
            }

            ProjectData projData;
            try {
                projData = dataCache.get(participant.getProjectId());
            } catch (ExecutionException ex) {
                LOGGER.error("Failed to get project data for {} (participation {})", participant.
                        getProjectId(), participant);
                throw new IllegalStateException("Failed to get project data", ex);
            }

            if (projData.getProject() == null) {
                continue;
            }

            if (ignoreIdProject && projData.getProject().getSubtype().equals(
                    ProjectSubtypeConstants.IDPROJECT)) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Ignoring participation {} that belongs to idproject {}",
                            participant.getParticipationId(), projData.getProject().getProjectId());
                }
                continue;
            }


            P participationData = getParticipationData(projData, participant);
            if (participationData == null) {
                continue;
            }

            partData.add(participationData);
        }

        Map<String, Object> jsonResponse = new TreeMap<>();
        populateJsonData(jsonResponse, partData);

        String jsonString = JsonUtils.encodePretty(jsonResponse);
        return jsonString.getBytes(CHARSET);
    }

    protected abstract P getParticipationData(ProjectData projData, ProjectParticipation participant);

    protected abstract void populateJsonData(Map<String, Object> jsonResponse, List<P> partData);

    public long getProcessed() {
        return processed;
    }

    private ProjectData loadProjectData(Long projectId) {
        final OrgProject proj = ccbcClient.getProject(projectId);

        return ProjectTypeUtil.call(proj, new ProjectTypeCallable<ProjectData>() {

            @Override
            public ProjectData callMaterialListProject() {
                return new ProjectData(proj, null, null, null);
            }

            @Override
            public ProjectData callDesignedProject() {
                if (proj.getDesignId() == null || proj.getMasterDatabank() == null) {
                    LOGGER.debug(
                            "Ignoring design project {} since it doesn't have a design",
                            projectId);
                    return new ProjectData(null, null, null, null);
                }

                CourseDesignDefinition cdd;
                try {
                    cdd = new GetProjectCourseDesignCommand(cycle, null).
                            forProject(proj);
                } catch (IllegalStateException ex) {
                    LOGGER.
                            warn("Failed to get course definition for project {}: {}",
                                    proj.getProjectId(), ex);
                    return new ProjectData(null, null, null, null);
                }
                DatabankFacade df = new GetDatabankFacadeCommand(cycle).get(proj);

                Map<Long, List<ParticipationProgress>> progressMap
                        = ccbcClient.getProjectProgress(proj.getProjectId());

                return new ProjectData(proj, cdd, df, progressMap);
            }

            @Override
            public ProjectData callSingleProductProject() {
                return callMaterialListProject();
            }
        });
    }

}
