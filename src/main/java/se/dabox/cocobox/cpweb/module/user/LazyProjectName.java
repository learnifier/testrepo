/*
 * (c) Dabox AB 2012 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.module.user;

import net.unixdeveloper.druwa.DruwaService;
import net.unixdeveloper.druwa.ServiceRequestCycle;
import se.dabox.service.common.ccbc.CocoboxCoordinatorClient;
import se.dabox.service.common.ccbc.project.GetProjectAdministrativeName;
import se.dabox.service.common.ccbc.project.OrgProject;
import se.dabox.util.cache.LazyCache;

/**
 *
 * @author Jerker Klang (jerker.klang@dabox.se)
 */
public class LazyProjectName {
    private final LazyCache<Long,String> cache;

    public LazyProjectName(final CocoboxCoordinatorClient ccbc) {
        this.cache = new LazyCache<>((Long pid) -> {
            OrgProject project = ccbc.getProject(pid);

            final ServiceRequestCycle cycle = DruwaService.getCurrentCycle();
            return new GetProjectAdministrativeName(cycle).getName(project);
        });

    }

    public String forProject(long projectId) {
        return cache.get(projectId);
    }

}
