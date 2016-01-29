/*
 * (c) Dabox AB 2015 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.module.user;

import net.unixdeveloper.druwa.HttpMethod;
import net.unixdeveloper.druwa.RequestCycle;
import net.unixdeveloper.druwa.RequestTarget;
import net.unixdeveloper.druwa.annotation.WebAction;
import net.unixdeveloper.druwa.annotation.mount.WebModuleMountpoint;
import net.unixdeveloper.druwa.request.RedirectUrlRequestTarget;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.dabox.cocobox.cpweb.NavigationUtil;
import se.dabox.cocobox.cpweb.module.core.AbstractWebAuthModule;
import se.dabox.cocobox.security.permission.CocoboxPermissions;
import se.dabox.cocosite.druwa.DruwaParamHelper;
import se.dabox.cocosite.org.MiniOrgInfo;
import se.dabox.service.client.CacheClients;
import se.dabox.service.common.context.DwsRealmHelper;
import se.dabox.service.login.client.AlreadyExistsException;
import se.dabox.service.login.client.CocoboxUserAccount;
import se.dabox.service.login.client.LoginService;
import se.dabox.service.login.client.NotFoundException;
import se.dabox.service.login.client.UserAccountService;
import se.dabox.service.login.client.autologinlink.AutoLoginLink;
import se.dabox.service.webutils.login.LoginUserAccountHelper;

/**
 *
 * @author Jerker Klang (jerker.klang@dabox.se)
 */
@WebModuleMountpoint("/user.mod")
public class UserModificationModule extends AbstractWebAuthModule {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserModificationModule.class);

    @WebAction(methods = HttpMethod.POST)
    public RequestTarget onActivateAutoLoginLink(RequestCycle cycle, String strOrgId, String strUserId) {
        long userId = Long.valueOf(strUserId);
        
        MiniOrgInfo org = secureGetMiniOrg(cycle, strOrgId);
        long orgId = org.getId();
        checkOrgPermission(cycle, orgId, CocoboxPermissions.BO_CREATE_USER_AUTOLOGINLINK);

        LoginService lsClient = CacheClients.getClient(cycle, LoginService.class);

        try {
            lsClient.activateAutoLoginLink(LoginUserAccountHelper.getCurrentCaller(cycle), userId);
        } catch (AlreadyExistsException are) {
            //Ignore this
        }

        return new RedirectUrlRequestTarget(NavigationUtil.toUserPageUrl(cycle, strOrgId, userId));
    }

    @WebAction(methods = HttpMethod.POST)
    public RequestTarget onDeactivateAutoLoginLink(RequestCycle cycle, String strOrgId, String strUserId) {
        long userId = Long.valueOf(strUserId);

        MiniOrgInfo org = secureGetMiniOrg(cycle, strOrgId);
        long orgId = org.getId();
        checkOrgPermission(cycle, orgId, CocoboxPermissions.BO_CREATE_USER_AUTOLOGINLINK);

        LoginService lsClient = CacheClients.getClient(cycle, LoginService.class);

        try {
            AutoLoginLink link = lsClient.getAutoLoginLink(userId);
            lsClient.deleteAutoLoginLink(LoginUserAccountHelper.getCurrentCaller(cycle), link);
        } catch (NotFoundException are) {
            //Ignore this
        }

        return new RedirectUrlRequestTarget(NavigationUtil.toUserPageUrl(cycle, strOrgId, userId));
    }

    @WebAction(methods = HttpMethod.POST)
    public RequestTarget onProfileSettingsSetting(RequestCycle cycle, String strOrgId, String strUserId) {
        long userId = Long.valueOf(strUserId);
        String mode = DruwaParamHelper.getMandatoryParam(LOGGER, cycle.getRequest(), "mode");

        MiniOrgInfo org = secureGetMiniOrg(cycle, strOrgId);
        long orgId = org.getId();
        checkOrgPermission(cycle, orgId, CocoboxPermissions.BO_EDIT_USER);

        UserAccountService lsClient = CacheClients.getClient(cycle, UserAccountService.class);

        boolean enabled = "enabled".equals(mode);

        boolean defaultSettings = DwsRealmHelper.getRealmConfiguration(cycle).getBooleanValue(
                CocoboxUserAccount.CONF_USERSETTINGS_ALLOWED);

        if (defaultSettings) {
            if (enabled) {
                lsClient.updateUserProfileValue(userId, CocoboxUserAccount.PROFILE_COCOBOX,
                        CocoboxUserAccount.ACCOUNTSETTINGS_ALLOWED, null);
            } else {
                lsClient.updateUserProfileValue(userId, CocoboxUserAccount.PROFILE_COCOBOX,
                        CocoboxUserAccount.ACCOUNTSETTINGS_ALLOWED, "false");
            }            
        } else {
            if (enabled) {
                lsClient.updateUserProfileValue(userId, CocoboxUserAccount.PROFILE_COCOBOX,
                        CocoboxUserAccount.ACCOUNTSETTINGS_ALLOWED, "true");
            } else {
                lsClient.updateUserProfileValue(userId, CocoboxUserAccount.PROFILE_COCOBOX,
                        CocoboxUserAccount.ACCOUNTSETTINGS_ALLOWED, null);
            }
        }

        return new RedirectUrlRequestTarget(NavigationUtil.toUserPageUrl(cycle, strOrgId, userId));
    }
}
