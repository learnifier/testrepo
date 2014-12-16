/*
 * (c) Dabox AB 2013 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.module.project.move;

import java.util.Map;
import net.unixdeveloper.druwa.RequestCycle;
import net.unixdeveloper.druwa.RequestTarget;
import net.unixdeveloper.druwa.annotation.WebAction;
import net.unixdeveloper.druwa.annotation.mount.WebModuleMountpoint;
import net.unixdeveloper.druwa.freemarker.FreemarkerRequestTarget;
import se.dabox.cocobox.cpweb.module.project.AbstractProjectWebModule;
import se.dabox.cocosite.security.CocoboxPermissions;
import se.dabox.service.common.ccbc.CocoboxCoordinatorClient;
import se.dabox.service.common.ccbc.project.OrgProject;
import se.dabox.service.common.ccbc.project.ProjectDetails;
import se.dabox.service.common.ccbc.project.ProjectParticipation;

/**
 *
 * @author Jerker Klang (jerker.klang@dabox.se)
 */
@WebModuleMountpoint("/project.pmove")
public class ParticipantMoveModule extends AbstractProjectWebModule {

    @WebAction
    public RequestTarget onSelectTarget(RequestCycle cycle, String strProjectId,
            String strParticipationId) {

        OrgProject project = getProject(cycle, strProjectId);

        checkPermission(cycle, project);
        checkProjectPermission(cycle, project, CocoboxPermissions.CP_VIEW_PROJECT);
        checkParticipation(cycle, project, strParticipationId);

        Map<String, Object> map = createMap();
        addCommonMapValues(map, project, cycle);

        return new FreemarkerRequestTarget("/project/move/selectTarget.html", map);
    }

    private ProjectParticipation checkParticipation(RequestCycle cycle, ProjectDetails project,
            String strParticipationId) {

        long participationId = Long.valueOf(strParticipationId);

        CocoboxCoordinatorClient ccbc = getCocoboxCordinatorClient(cycle);
        ProjectParticipation part = ccbc.getProjectParticipation(participationId);

        if (part == null) {
            throw new IllegalStateException("Participation not found " + strParticipationId);
        }

        if (part.getProjectId() != project.getProjectId()) {
            throw new IllegalStateException("Invalid participation: " + strParticipationId);
        }

        return part;
    }
}
