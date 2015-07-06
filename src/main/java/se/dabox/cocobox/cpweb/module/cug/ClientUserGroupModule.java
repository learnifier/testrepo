/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package se.dabox.cocobox.cpweb.module.cug;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import net.unixdeveloper.druwa.RequestCycle;
import net.unixdeveloper.druwa.RequestTarget;
import net.unixdeveloper.druwa.annotation.DefaultWebAction;
import net.unixdeveloper.druwa.annotation.WebAction;
import net.unixdeveloper.druwa.annotation.mount.WebModuleMountpoint;
import net.unixdeveloper.druwa.freemarker.FreemarkerRequestTarget;
import org.apache.commons.lang3.builder.CompareToBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.dabox.cocobox.cpweb.module.core.AbstractWebAuthModule;
import se.dabox.cocosite.org.MiniOrgInfo;
import se.dabox.cocosite.security.CocoboxPermissions;
import se.dabox.cocosite.security.role.CocoboxRoleUtil;
import se.dabox.service.client.CacheClients;
import se.dabox.service.cug.client.ClientUserGroup;
import se.dabox.service.cug.client.ClientUserGroupClient;
import se.dabox.service.login.client.UserAccount;
import se.dabox.service.login.client.UserAccountService;
import se.dabox.service.orgdir.client.OrganizationDirectoryClient;

/**
 *
 * @author Magnus Andersson (magnus.andersson@learnifier.com)
 */
@WebModuleMountpoint("/cug")
public class ClientUserGroupModule extends AbstractWebAuthModule {
    private static final Logger LOGGER =
            LoggerFactory.getLogger(ClientUserGroupModule.class);

    public static final String OVERVIEW_ACTION = "overview";

    @DefaultWebAction
    @WebAction
    public RequestTarget onOverview(RequestCycle cycle, String strOrgId, String strCugId) {

        MiniOrgInfo org = secureGetMiniOrg(cycle, strOrgId);
        checkOrgPermission(cycle, org.getId(), CocoboxPermissions.CP_VIEW_USER);
        ClientUserGroupClient cugService = getClientUserGroupService(cycle);
        ClientUserGroup cug = cugService.getGroup(Long.valueOf(strCugId));

//        checkOrgUserAccess(cycle, org, user);

//        Locale userLocale = CocositeUserHelper.getUserAccountUserLocale(user);

//        CharSequence orgRoleName = OrgRoleName.forOrg(org.getId());
//        String userRole = user.getProfileValue(CocoSiteConstants.UA_PROFILE, orgRoleName.toString());

//        boolean isAdmin = UserAccountRoleCheck.isCpAdmin(user, org.getId());
          boolean isAdmin = true;
//        OrganizationDirectoryClient odc = getOrganizationDirectoryClient(cycle);
//        OrgUnitInfo organization;
//        if(user.getOrganizationId() != null) {
//            organization = odc.getOrgUnitInfo(user.getOrganizationId());
//        } else {
//            organization = null;
//        }
        
        Map<String, Object> map = createMap();

        
        map.put("cug", cug);
//        map.put("subGroups", null);
//        map.put("members", cugService.listGroupMembers(cug.getGroupId()));
        
//        map.put("locale", userLocale);
        map.put("isAdmin", isAdmin);
//        map.put("userimg", new MiniUserAccountHelper(cycle).createInfo(user).getThumbnail());

        map.put("org", org);

        return new FreemarkerRequestTarget("/cug/clientUserGroupOverview.html", map);
    }

    private UserAccountService getUserAccountService(RequestCycle cycle) {
        return CacheClients.getClient(cycle, UserAccountService.class);
    }

    private ClientUserGroupClient getClientUserGroupService(RequestCycle cycle) {
        return CacheClients.getClient(cycle, ClientUserGroupClient.class);
    }

    private OrganizationDirectoryClient getOrganizationDirectoryClient(final RequestCycle cycle) {
        return CacheClients.getClient(cycle, OrganizationDirectoryClient.class);
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
