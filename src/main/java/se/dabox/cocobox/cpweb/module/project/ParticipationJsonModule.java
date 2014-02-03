/*
 * (c) Dabox AB 2012 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.module.project;

import net.unixdeveloper.druwa.RequestCycle;
import net.unixdeveloper.druwa.RequestTarget;
import net.unixdeveloper.druwa.annotation.WebAction;
import net.unixdeveloper.druwa.annotation.mount.WebModuleMountpoint;
import se.dabox.cocobox.cpweb.module.core.AbstractJsonAuthModule;
import se.dabox.cocobox.cpweb.module.project.partdetails.ParticipationDetailsCommand;
import se.dabox.service.common.ccbc.CocoboxCordinatorClient;
import se.dabox.service.common.ccbc.project.OrgProject;
import se.dabox.service.common.ccbc.project.ProjectParticipation;

/**
 *
 * @author Jerker Klang <jerker.klang@dabox.se>
 */
@WebModuleMountpoint("/project.jspart")
public class ParticipationJsonModule extends AbstractJsonAuthModule {

    @WebAction
    public RequestTarget onParticipationDetails(RequestCycle cycle, String strParticipationId) {
        
        long partId = Long.valueOf(strParticipationId);

        CocoboxCordinatorClient ccbc = getCocoboxCordinatorClient(cycle);

        ProjectParticipation participation =
                ccbc.getProjectParticipation(partId);

        OrgProject project = ccbc.getProject(participation.getProjectId());
        checkPermission(cycle, project);

        return new ParticipationDetailsCommand(cycle).
                refreshCrispInformation(true).
                forParticipation(project, participation);
    }

}
