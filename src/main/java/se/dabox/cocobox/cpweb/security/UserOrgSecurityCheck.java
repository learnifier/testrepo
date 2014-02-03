/*
 * (c) Dabox AB 2012 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.security;

import net.unixdeveloper.druwa.RequestCycle;
import se.dabox.cocosite.security.UserAccountRoleCheck;
import se.dabox.service.client.CacheClients;
import se.dabox.service.login.client.UserAccount;
import se.dabox.service.login.client.UserAccountService;
import se.dabox.service.webutils.cache.WebSessionCacheAdapter;
import se.dabox.service.webutils.login.LoginUserAccountHelper;

/**
 *
 * @author Jerker Klang <jerker.klang@dabox.se>
 */
public class UserOrgSecurityCheck extends WebSessionCacheAdapter<Boolean>{
    private static final UserOrgSecurityCheck INSTANCE = new UserOrgSecurityCheck();

    public static UserOrgSecurityCheck getInstance() {
        return INSTANCE;
    }

    private UserOrgSecurityCheck() {
        super(UserOrgSecurityCheck.class.getName());
    }

    @Override
    protected Boolean createEntry(RequestCycle cycle, String strOrgId) {

        long orgId = Long.parseLong(strOrgId);
        
        //Use an uncached version for security?
        UserAccountService uaService =
                CacheClients.getClient(cycle, UserAccountService.class);

        long userId = LoginUserAccountHelper.getUserId(cycle);
        UserAccount account = uaService.getUserAccount(userId);

        boolean isValid = isCpAdmin(account, orgId) || isBoAdmin(account);
        return isValid;
    }

    private static boolean isCpAdmin(final UserAccount account, final long orgId) {
        return UserAccountRoleCheck.isCpAdmin(account, orgId);
    }

    private static boolean isBoAdmin(final UserAccount account) {
        return UserAccountRoleCheck.isBoAdmin(account);
    }
}
