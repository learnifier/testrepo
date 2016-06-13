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
import se.dabox.service.common.ccbc.project.GetProjectAdministrativeName;
import se.dabox.service.common.ccbc.project.OrgProject;
import se.dabox.util.Holder;
import se.dabox.util.collections.BiProcessor;
import se.dabox.util.collections.CollectionsUtil;
import se.dabox.util.collections.Factory;
import se.dabox.util.collections.ListUtil;
import se.dabox.util.collections.Transformer;

/**
 *
 * @author Jerker Klang (jerker.klang@learnifier.com)
 */
public class FetchProjectNames<T> implements Factory<List<T>> {
    private final StatusHolder statusHolder;
    private final Factory<List<T>> factory;
    private final Transformer<T,Long> toProjectIdTransformer;
    private final BiProcessor<T,String> nameSetter;

    public FetchProjectNames(StatusHolder statusHolder,
            Factory<List<T>> factory,
            Transformer<T, Long> toProjectIdTransformer,
            BiProcessor<T, String> nameSetter) {
        this.statusHolder = statusHolder;
        this.factory = factory;
        this.toProjectIdTransformer = toProjectIdTransformer;
        this.nameSetter = nameSetter;
    }



    @Override
    public List<T> create() {
        List<T> list = factory.create();

        Map<Long, OrgProject> projectMap = getProjectMap(list);

        final ServiceRequestCycle cycle = DruwaService.getCurrentCycle();
        final GetProjectAdministrativeName nameFetcher
                = new GetProjectAdministrativeName(cycle);

        statusHolder.setStatus(new Status("Populating project details"));

        for (T t : list) {
            Long projectId = toProjectIdTransformer.transform(t);
            if (projectId == null) {
                continue;
            }

            OrgProject project = projectMap.get(projectId);
            if (project == null) {
                continue;
            }

            String name = nameFetcher.getName(project);
            nameSetter.process(t, name);
        }

        return list;
    }

    private Map<Long, OrgProject> getProjectMap(
            List<? extends T> list) {
        Set<Long> projectIdSet = CollectionsUtil.transformNotNull(list, toProjectIdTransformer);
        List<Long> projectIdList = new ArrayList<>(projectIdSet);
        List<OrgProject> projects = getProjects(projectIdList);
        Map<Long, OrgProject> projectMap
                = CollectionsUtil.createMap(projects, OrgProject::getProjectId);
        return projectMap;
    }

    private List<OrgProject> getProjects(List<Long> projectIdList) {
        final ServiceRequestCycle cycle = DruwaService.getCurrentCycle();
        CocoboxCoordinatorClient ccbc = CacheClients.getClient(cycle, CocoboxCoordinatorClient.class);

        Holder<Long> processedHolder = new Holder<>(0L);

        return ListUtil.processPartitions(100, projectIdList, (subl) -> {
            statusHolder.setStatus(new Status("Fetching projects",
                    processedHolder.getValue(),
                    (long) projectIdList.size()));

            List<OrgProject> projects = ccbc.getProjects(subl);
            processedHolder.setValue(processedHolder.getValue() + projects.size());

            return projects;
        });
    }

}
