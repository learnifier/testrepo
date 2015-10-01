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
import java.util.Set;
import net.unixdeveloper.druwa.RequestCycle;
import net.unixdeveloper.druwa.RequestTarget;
import net.unixdeveloper.druwa.annotation.DefaultWebAction;
import net.unixdeveloper.druwa.annotation.WebAction;
import net.unixdeveloper.druwa.annotation.mount.WebModuleMountpoint;
import net.unixdeveloper.druwa.freemarker.FreemarkerRequestTarget;
import net.unixdeveloper.druwa.request.WebModuleRedirectRequestTarget;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.CompareToBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.dabox.cocobox.cpweb.module.core.AbstractWebAuthModule;
import se.dabox.cocosite.druwa.CocoSiteConstants;
import se.dabox.cocosite.druwa.DruwaParamHelper;
import se.dabox.cocosite.infocache.InfoCacheHelper;
import se.dabox.cocosite.login.CocositeUserHelper;
import se.dabox.cocosite.org.MiniOrgInfo;
import se.dabox.cocobox.security.permission.CocoboxPermissions;
import se.dabox.cocobox.security.role.CocoboxRoleUtil;
import se.dabox.cocobox.security.user.UserAccountRoleCheck;
import se.dabox.cocosite.user.MiniUserAccountHelper;
import se.dabox.service.client.CacheClients;
import se.dabox.cocobox.security.user.OrgRoleName;
import se.dabox.service.login.client.UserAccount;
import se.dabox.service.login.client.UserAccountService;

/**
 *
 * @author borg321
 */
@WebModuleMountpoint("/user")
public class UserModule extends AbstractWebAuthModule {
    private static final Logger LOGGER =
            LoggerFactory.getLogger(UserModule.class);

    public static final String OVERVIEW_ACTION = "overview";

    @DefaultWebAction
    @WebAction
    public RequestTarget onOverview(RequestCycle cycle, String strOrgId, String strUserId) {

        MiniOrgInfo org = secureGetMiniOrg(cycle, strOrgId);
        checkOrgPermission(cycle, org.getId(), CocoboxPermissions.CP_VIEW_USER);

        UserAccount user = getUserAccountService(cycle).getUserAccount(Long.valueOf(strUserId));

        checkOrgUserAccess(cycle, org, user);

        Locale userLocale = CocositeUserHelper.getUserAccountUserLocale(user);

        CharSequence orgRoleName = OrgRoleName.forOrg(org.getId());
        String userRole = user.getProfileValue(CocoSiteConstants.UA_PROFILE, orgRoleName.toString());

        boolean isAdmin = UserAccountRoleCheck.isCpAdmin(user, org.getId());

        Map<String, Object> map = createMap();

        map.put("user", user);
        map.put("locale", userLocale);
        map.put("role", userRole);
        map.put("isAdmin", isAdmin);
        map.put("userimg", new MiniUserAccountHelper(cycle).createInfo(user).getThumbnail());

        map.put("org", org);

        return new FreemarkerRequestTarget("/user/userOverview.html", map);
    }

    @WebAction
    public RequestTarget onRoles(RequestCycle cycle, String strOrgId, String strUserId) {
        UserAccount user = getUserAccountService(cycle).getUserAccount(Long.valueOf(strUserId));

        MiniOrgInfo org = secureGetMiniOrg(cycle, strOrgId);
        checkOrgPermission(cycle, org.getId(), CocoboxPermissions.CP_VIEW_USER);

        Locale userLocale = CocositeUserHelper.getUserLocale(cycle);

        List<RoleInfo> roles = getRoles(cycle);

        Map<String, Object> map = createMap();
        CharSequence orgRoleName = OrgRoleName.forOrg(org.getId());
        String userRole = user.getProfileValue(CocoSiteConstants.UA_PROFILE, orgRoleName.toString());

        boolean isAdmin = UserAccountRoleCheck.isCpAdmin(user, org.getId());

        map.put("user", user);
        map.put("role", userRole);
        map.put("isAdmin", isAdmin);
        map.put("locale", userLocale);
        map.put("roles", roles);

        
        map.put("userimg", InfoCacheHelper.getInstance(cycle).getMiniUserInfo(user.getUserId()).
                getThumbnail());

        map.put("org", org);
        
        return new FreemarkerRequestTarget("/user/userRoles.html", map);
    }

    @WebAction
    public RequestTarget onAddRole(RequestCycle cycle, String strOrgId) {
        MiniOrgInfo org = secureGetMiniOrg(cycle, strOrgId);
        checkOrgPermission(cycle, org.getId(), CocoboxPermissions.CP_EDIT_USER);

        long userId = DruwaParamHelper.getMandatoryLongParam(LOGGER, cycle.getRequest(), "userId");
        String role = DruwaParamHelper.getMandatoryParam(LOGGER, cycle.getRequest(), "role");

        long orgId = Long.parseLong(strOrgId);

        UserAccount user = getUserAccountService(cycle).getUserAccount(userId);

        Set<String> roles = UserAccountRoleCheck.getCpRoles(user, orgId, true);
        final Map<String, String> cpRoles = new CocoboxRoleUtil().getCpRoles(cycle);        

        if (!cpRoles.containsKey(role)) {
            throw new IllegalStateException("Non existing client role: "+role);
        }

        roles.add(role);

        String roleString = StringUtils.join(roles, ',');

        CharSequence valueName = OrgRoleName.forOrg(org.getId());

        getUserAccountService(cycle).updateUserProfileValue(userId,
                CocoSiteConstants.UA_PROFILE, valueName, roleString);

        return new WebModuleRedirectRequestTarget(UserModule.class, "roles", strOrgId,
                Long.toString(userId));
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

    private void checkOrgUserAccess(RequestCycle cycle, MiniOrgInfo org, UserAccount user) {

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
