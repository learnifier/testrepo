/*
 * (c) Dabox AB 2012 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.module.integration;

import java.util.Date;
import java.util.Map;
import net.unixdeveloper.druwa.RequestCycle;
import net.unixdeveloper.druwa.RequestTarget;
import net.unixdeveloper.druwa.annotation.WebAction;
import net.unixdeveloper.druwa.annotation.mount.WebModuleMountpoint;
import net.unixdeveloper.druwa.freemarker.FreemarkerRequestTarget;
import se.dabox.cocobox.cpweb.module.core.AbstractWebAuthModule;
import se.dabox.cocobox.cpweb.state.ErrorState;
import se.dabox.cocobox.crisp.runtime.CrispErrorException;
import se.dabox.cocobox.crisp.runtime.StandardError;
import se.dabox.service.common.ccbc.project.OrgProject;

/**
 *
 * @author Jerker Klang <jerker.klang@dabox.se>
 */
@WebModuleMountpoint("/integration.error")
public class IntegrationErrorModule extends AbstractWebAuthModule {

    public static final String ATTRIBUTE_ERRORSTATE = "integrationErrorState";

    @WebAction
    public RequestTarget onError(RequestCycle cycle) {
        ErrorState state = (ErrorState) cycle.getAttribute(ATTRIBUTE_ERRORSTATE);

        if (state == null) {
            throw new IllegalStateException("Error state is missing in request cycle");
        }

        OrgProject prj = null;

        if (state.getProjectId() != null) {
            prj = getCocoboxCordinatorClient(cycle).getProject(state.getProjectId());
            checkPermission(cycle, prj);
        }

        Map<String, Object> map = createMap();

        map.put("exception", state.getException());
        map.put("product", state.getProduct());
        map.put("project", prj);
        map.put("now", new Date());

        map.put("org", secureGetMiniOrg(cycle, state.getOrgId()));

        StandardError standardError = getStandardError(state);
        if (standardError != null) {
            map.put("crispStandardError", standardError);
        }

        return new FreemarkerRequestTarget("/integration/errorPage.html", map);
    }

    private static StandardError getStandardError(ErrorState state) {
        if (state.getException() instanceof CrispErrorException) {
            CrispErrorException ceex =
                    (CrispErrorException) state.getException();
            return ceex.getError();
        }

        return null;
    }
}
