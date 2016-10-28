package se.dabox.cocobox.cpweb.module.project;

import net.unixdeveloper.druwa.RequestCycle;
import net.unixdeveloper.druwa.RetargetException;
import net.unixdeveloper.druwa.request.ErrorCodeRequestTarget;
import org.slf4j.Logger;
import se.dabox.cocobox.cpweb.module.core.AbstractJsonAuthModule;
import se.dabox.service.common.ccbc.project.OrgProject;

import javax.servlet.http.HttpServletResponse;

/**
 *
 * @author Magnus Andersson (magnus.andersson@learnifier.com)
 */
public abstract class AbstractProjectJsModule extends AbstractJsonAuthModule {
    protected void checkPermission(RequestCycle cycle, OrgProject project, String strProjectId, Logger log) {
        if (project == null) {
            log.warn("Project {} doesn't exist.", strProjectId);

            ErrorCodeRequestTarget error
                    = new ErrorCodeRequestTarget(HttpServletResponse.SC_NOT_FOUND);

            throw new RetargetException(error);
        } else {
            checkPermission(cycle, project);
        }
    }
}
