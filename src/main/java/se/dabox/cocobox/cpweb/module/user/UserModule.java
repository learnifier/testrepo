/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package se.dabox.cocobox.cpweb.module.user;

import java.util.Locale;
import java.util.Map;
import net.unixdeveloper.druwa.RequestCycle;
import net.unixdeveloper.druwa.RequestTarget;
import net.unixdeveloper.druwa.annotation.DefaultWebAction;
import net.unixdeveloper.druwa.annotation.WebAction;
import net.unixdeveloper.druwa.annotation.mount.WebModuleMountpoint;
import net.unixdeveloper.druwa.freemarker.FreemarkerRequestTarget;
import se.dabox.cocobox.cpweb.module.core.AbstractWebAuthModule;
import se.dabox.cocosite.druwa.CocoSiteConstants;
import se.dabox.cocosite.login.CocositeUserHelper;
import se.dabox.cocosite.org.MiniOrgInfo;
import se.dabox.cocosite.user.MiniUserAccountHelper;
import se.dabox.service.client.CacheClients;
import se.dabox.service.common.ccbc.org.OrgRoleName;
import se.dabox.service.login.client.UserAccount;
import se.dabox.service.login.client.UserAccountService;
import se.dabox.util.collections.ValueUtils;

/**
 *
 * @author borg321
 */
@WebModuleMountpoint("/user")
public class UserModule extends AbstractWebAuthModule {

    public static final String OVERVIEW_ACTION = "overview";

    @DefaultWebAction
    @WebAction
    public RequestTarget onOverview(RequestCycle cycle, String strOrgId, String strUserId) {

        MiniOrgInfo org = secureGetMiniOrg(cycle, strOrgId);

        UserAccount user = getUserAccountService(cycle).getUserAccount(Long.valueOf(strUserId));
        Locale userLocale = ValueUtils.coalesce(CocositeUserHelper.getUserAccountUserLocale(user),
                CocoSiteConstants.DEFAULT_LOCALE);

        CharSequence orgRoleName = OrgRoleName.forOrg(org.getId());
        String userRole = user.getProfileValue(CocoSiteConstants.UA_PROFILE, orgRoleName.toString());

        Map<String, Object> map = createMap();

        map.put("user", user);
        map.put("locale", userLocale);
        map.put("role", userRole);
        map.put("userimg", new MiniUserAccountHelper(cycle).createInfo(user).getThumbnail());

        map.put("org", org);

        return new FreemarkerRequestTarget("/user/userOverview.html", map);
    }

    private UserAccountService getUserAccountService(RequestCycle cycle) {
        return CacheClients.getClient(cycle, UserAccountService.class);
    }
}
