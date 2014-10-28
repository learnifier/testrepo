/*
 * (c) Dabox AB 2013 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.module.project.role;

import net.unixdeveloper.druwa.RequestCycle;
import net.unixdeveloper.druwa.RequestTarget;
import net.unixdeveloper.druwa.request.WebModuleRedirectRequestTarget;
import se.dabox.cocobox.cpweb.NavigationUtil;
import se.dabox.cocobox.cpweb.module.mail.RequestTargetGenerator;
import se.dabox.cocobox.cpweb.module.project.ProjectModule;

/**
 *
 * @author Jerker Klang (jerker.klang@dabox.se)
 */
class ProjectRolesRedirectTargetGenerator implements RequestTargetGenerator {
    private static final long serialVersionUID = 1L;

    private final long projectId;

    public ProjectRolesRedirectTargetGenerator(long projectId) {
        this.projectId = projectId;
    }

    @Override
    public RequestTarget generateTarget(RequestCycle cycle) {
        return NavigationUtil.toProjectRoles(cycle, projectId);
    }

    
}
