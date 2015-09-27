/*
 * (c) Dabox AB 2013 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.module.project.internal;

import net.unixdeveloper.druwa.RequestCycle;
import net.unixdeveloper.druwa.RequestTarget;
import net.unixdeveloper.druwa.annotation.WebAction;
import net.unixdeveloper.druwa.annotation.mount.WebModuleMountpoint;
import net.unixdeveloper.druwa.request.StringRequestTarget;
import se.dabox.cocobox.cpweb.module.project.AbstractProjectWebModule;
import se.dabox.cocobox.security.permission.CocoboxPermissions;
import se.dabox.service.client.CacheClients;
import se.dabox.service.common.ccbc.project.OrgProject;
import se.dabox.service.common.coursedesign.CourseDesign;
import se.dabox.service.common.coursedesign.CourseDesignClient;
import se.dabox.util.collections.ValueUtils;

/**
 * Module that exposes internal information in a way suitable for debugging.
 *
 * @author Jerker Klang (jerker.klang@dabox.se)
 */
@WebModuleMountpoint("/project/internal")
public class ProjectInternalModule extends AbstractProjectWebModule {

    @WebAction
    public RequestTarget onDesign(RequestCycle cycle, String strProjectId) {

        OrgProject project =
                getProject(cycle, strProjectId);

        checkPermission(cycle, project);
        checkProjectPermission(cycle, project, CocoboxPermissions.CP_EDIT_PROJECT_COURSEDESIGN);

        CourseDesignClient cdClient = CacheClients.getClient(cycle, CourseDesignClient.class);

        long designId = ValueUtils.coalesce(project.getStageDesignId(), project.getDesignId());

        CourseDesign design = cdClient.getDesign(designId);

        return new StringRequestTarget("text/xml", design.getDesign());
    }

}
