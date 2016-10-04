/*
 * (c) Dabox AB 2015 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.module.self;

import net.unixdeveloper.druwa.RequestCycle;
import net.unixdeveloper.druwa.RequestTarget;
import net.unixdeveloper.druwa.annotation.DefaultWebAction;
import net.unixdeveloper.druwa.annotation.WebAction;
import net.unixdeveloper.druwa.annotation.mount.WebModuleMountpoint;
import net.unixdeveloper.druwa.request.RedirectUrlRequestTarget;
import se.dabox.cocobox.cpweb.NavigationUtil;
import se.dabox.cocobox.cpweb.module.core.AbstractWebAuthModule;
import se.dabox.service.common.context.DwsRealmHelper;
import se.dabox.service.common.context.RealmNotDetectedException;
import se.dabox.service.login.client.UserAccount;
import se.dabox.service.webutils.login.LoginUserAccountHelper;

/**
 *
 * @author Jerker Klang (jerker.klang@learnifier.com)
 */
@WebModuleMountpoint("/myorg")
public class MyOrgModule extends AbstractWebAuthModule {

    @DefaultWebAction
    @WebAction
    public RequestTarget onRedirect(RequestCycle cycle) {
        String path = cycle.getRequest().getParameter("path");

        UserAccount acc = LoginUserAccountHelper.getUserAccount(cycle);

        Long org = acc.getOrganizationId();

        if (org == null) {
            return NavigationUtil.toMain(cycle);
        }

        StringBuilder sb = new StringBuilder(cycle.getRequest().getContextPath());
        if (path.startsWith("/")) {
            sb.append(path, 0, path.length());
        } else {
            sb.append(path);
        }
        if (!path.endsWith("/")) {
            sb.append('/');
        }

        sb.append(org);

        return new RedirectUrlRequestTarget(sb.toString());
    }

    @WebAction
    public RequestTarget onMfpredirect(RequestCycle cycle) {
        return toPropertyBasedProject(cycle, "trial.myfirstproject");
    }

    @WebAction
    public RequestTarget onDpredirect(RequestCycle cycle) {
        return toPropertyBasedProject(cycle, "trial.demoproject");
    }

    private RequestTarget toPropertyBasedProject(RequestCycle cycle, String confName) throws IllegalArgumentException, RealmNotDetectedException {
        String path = cycle.getRequest().getParameter("path");

        Long projectId = DwsRealmHelper.getRealmConfiguration(cycle).getLongValue(confName);

        if (projectId == null) {
            return NavigationUtil.toMain(cycle);
        }

        StringBuilder sb = new StringBuilder(cycle.getRequest().getContextPath());
        if (path.startsWith("/")) {
            sb.append(path, 0, path.length());
        } else {
            sb.append(path);
        }
        if (!path.endsWith("/")) {
            sb.append('/');
        }

        sb.append(projectId);

        return new RedirectUrlRequestTarget(sb.toString());
    }

}
