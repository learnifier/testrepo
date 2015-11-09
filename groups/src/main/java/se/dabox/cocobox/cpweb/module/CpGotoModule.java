/*
 * (c) Dabox AB 2015 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.module;

import net.unixdeveloper.druwa.RequestCycle;
import net.unixdeveloper.druwa.RequestTarget;
import net.unixdeveloper.druwa.annotation.AroundInvoke;
import net.unixdeveloper.druwa.annotation.WebAction;
import net.unixdeveloper.druwa.annotation.mount.WebModuleMountpoint;
import net.unixdeveloper.druwa.module.InvocationContext;
import net.unixdeveloper.druwa.request.RedirectUrlRequestTarget;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.dabox.cocobox.cpweb.NavigationUtil;
import se.dabox.cocobox.cpweb.module.core.AbstractModule;
import se.dabox.cocosite.druwa.CocoSiteConfKey;
import se.dabox.cocobox.security.permission.CocoboxPermissions;
import se.dabox.cocobox.security.project.ProjectPermissionCheck;
import se.dabox.cocosite.security.CocositeLoginChecker;
import se.dabox.service.common.ccbc.project.OrgProject;
import se.dabox.service.common.context.DwsRealmHelper;
import se.dabox.service.login.client.UserAccount;
import se.dabox.service.webutils.login.LoginUserAccountHelper;
import se.dabox.service.webutils.login.nlogin.AbstractNewLoginChecker;

/**
 * Module used for various goto operations. The gotos are high level actions that this module can
 * determine in detail where the actual target is.
 *
 * @author Jerker Klang (jerker.klang@dabox.se)
 */
@WebModuleMountpoint("/goto")
public class CpGotoModule extends AbstractModule {
    private static final Logger LOGGER =
            LoggerFactory.getLogger(CpGotoModule.class);

    private final AbstractNewLoginChecker loginChecker = new CocositeLoginChecker();

    @AroundInvoke(order = 500)
    public final Object loginCheck(InvocationContext ctx) {
        return loginChecker.loginCheck(ctx);
    }

    //Do not extend from an auth class. We want to be able to run this without security
    @WebAction
    public RequestTarget onProject(RequestCycle cycle, String strProjectId) {
        UserAccount user = LoginUserAccountHelper.getUserAccount(cycle);

        OrgProject project
                = getCocoboxCordinatorClient(cycle).getProject(Long.valueOf(strProjectId));

        if (project == null) {
            LOGGER.debug("Project {} doesn't exist. Redirecting to boweb");

             String boweb = DwsRealmHelper.getRealmConfiguration(cycle).getValue(
                    CocoSiteConfKey.BOWEB_BASEURL);

            return new RedirectUrlRequestTarget(boweb);
        }

        ProjectPermissionCheck ppc = ProjectPermissionCheck.fromCycle(cycle);

        if (ppc.checkPermission(project, CocoboxPermissions.CP_ACCESS) &&
                ppc.checkPermission(project, CocoboxPermissions.CP_VIEW_PROJECT)) {

            LOGGER.debug("Redirecting to client portal page for project {}", strProjectId);

            NavigationUtil.toProjectPage(Long.valueOf(strProjectId));
        } else if (ppc.checkPermission(project, CocoboxPermissions.PRJ_PREVIEW_ON_USERPORTAL)) {
            LOGGER.debug("Redirecting to user portal to active preview for project {}", strProjectId);

            String upweb = DwsRealmHelper.getRealmConfiguration(cycle).getValue(
                    CocoSiteConfKey.UPWEB_BASEURL);

            String url = upweb+"user/preview/"+strProjectId;

            return new RedirectUrlRequestTarget(url);
        } else {
            //LOGGER.warn("Unable to determine where to redirect user {} for project {}. Sending user to userweb.", projectPerms.get)

            String upweb = DwsRealmHelper.getRealmConfiguration(cycle).getValue(
                    CocoSiteConfKey.UPWEB_BASEURL);

            return new RedirectUrlRequestTarget(upweb);
        }

        return NavigationUtil.toProjectPage(Long.valueOf(strProjectId));
    }
}
