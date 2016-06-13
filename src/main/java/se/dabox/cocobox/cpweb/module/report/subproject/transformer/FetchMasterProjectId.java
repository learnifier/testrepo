/*
 * (c) Dabox AB 2015 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.module.report.subproject.transformer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.unixdeveloper.druwa.DruwaService;
import net.unixdeveloper.druwa.ServiceRequestCycle;
import se.dabox.cocobox.cpweb.module.report.StatusHolder;
import se.dabox.service.client.CacheClients;
import se.dabox.service.common.ajaxlongrun.Status;
import se.dabox.service.common.ccbc.CocoboxCoordinatorClient;
import se.dabox.service.common.ccbc.project.OrgProject;
import se.dabox.util.collections.CollectionsUtil;
import se.dabox.util.collections.Factory;

/**
 *
 * @author Jerker Klang (jerker.klang@learnifier.com)
 */
public class FetchMasterProjectId implements Factory<List<SubprojectParticipant>> {

    private final StatusHolder statusHolder;
    private final Factory<List<SubprojectParticipant>> backend;

    public FetchMasterProjectId(StatusHolder statusHolder,
            Factory<List<SubprojectParticipant>> backend) {
        this.statusHolder = statusHolder;
        this.backend = backend;
    }

    @Override
    public List<SubprojectParticipant> create() {
        List<SubprojectParticipant> list = backend.create();

        Set<Long> projectIds = CollectionsUtil.transform(list, p -> p.getProjectId());
        final ServiceRequestCycle cycle = DruwaService.getCurrentCycle();
        CocoboxCoordinatorClient ccbc
                = CacheClients.getClient(cycle, CocoboxCoordinatorClient.class);

        List<OrgProject> projects = ccbc.getProjects(new ArrayList<>(projectIds));
        Map<Long, OrgProject> map = CollectionsUtil.createMap(projects, OrgProject::getProjectId);

        statusHolder.setStatus(new Status("Getting sub project information"));

        for (SubprojectParticipant part : list) {
            OrgProject project = map.get(part.getProjectId());
            if (project != null) {
                part.setMasterProject(project.getMasterProject());
            }
        }

        return list;
    }

}
