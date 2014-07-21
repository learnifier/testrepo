/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package se.dabox.cocobox.cpweb.module.user;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.unixdeveloper.druwa.RequestCycle;
import net.unixdeveloper.druwa.RequestTarget;
import net.unixdeveloper.druwa.annotation.DefaultWebAction;
import net.unixdeveloper.druwa.annotation.WebAction;
import net.unixdeveloper.druwa.annotation.mount.WebModuleMountpoint;
import net.unixdeveloper.druwa.freemarker.FreemarkerRequestTarget;
import org.apache.commons.lang3.builder.CompareToBuilder;
import se.dabox.cocobox.cpweb.module.core.AbstractWebAuthModule;
import se.dabox.cocosite.druwa.CocoSiteConstants;
import se.dabox.cocosite.infocache.InfoCacheHelper;
import se.dabox.cocosite.login.CocositeUserHelper;
import se.dabox.cocosite.org.MiniOrgInfo;
import se.dabox.cocosite.security.role.CocoboxRoleUtil;
import se.dabox.cocosite.user.MiniUserAccountHelper;
import se.dabox.service.client.CacheClients;
import se.dabox.service.common.ccbc.org.OrgRoleName;
import se.dabox.service.login.client.UserAccount;
import se.dabox.service.login.client.UserAccountService;

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
        Locale userLocale = CocositeUserHelper.getUserLocale(cycle);

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

    @WebAction
    public RequestTarget onRoles(RequestCycle cycle, String strUserId) {
        UserAccount user = getUserAccountService(cycle).getUserAccount(Long.valueOf(strUserId));

        Locale userLocale = CocositeUserHelper.getUserLocale(cycle);

        List<RoleInfo> roles = getRoles(cycle);

        Map<String, Object> map = createMap();

        map.put("user", user);
        map.put("locale", userLocale);
        map.put("roles", roles);

        map.put("userimg", InfoCacheHelper.getInstance(cycle).getMiniUserInfo(user.getUserId()).
                getThumbnail());

        return new FreemarkerRequestTarget("/user/userRoles.html", map);
    }

    private UserAccountService getUserAccountService(RequestCycle cycle) {
        return CacheClients.getClient(cycle, UserAccountService.class);
    }

    private List<RoleInfo> getRoles(RequestCycle cycle) {
        Map<String, String> roleMap = new CocoboxRoleUtil().getCpRoles(cycle);

        List<RoleInfo> infoList = new ArrayList<>();
        for (Map.Entry<String, String> entry : roleMap.entrySet()) {
            infoList.add(new RoleInfo(entry.getKey(), entry.getValue()));
        }

        Collections.sort(infoList, new Comparator<RoleInfo>() {

            @Override
            public int compare(RoleInfo o1, RoleInfo o2) {
                return new CompareToBuilder().
                        append(o1.getName(), o2.getName()).
                        append(o1.getUuid(), o2.getUuid()).
                        build();
            }
        });
        
        return infoList;
    }

    public static class RoleInfo {
        private final String uuid;
        private final String name;

        public RoleInfo(String uuid, String name) {
            this.uuid = uuid;
            this.name = name;
        }

        public String getUuid() {
            return uuid;
        }

        public String getName() {
            return name;
        }
        
    }
}
