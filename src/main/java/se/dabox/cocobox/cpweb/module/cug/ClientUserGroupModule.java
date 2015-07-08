/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package se.dabox.cocobox.cpweb.module.cug;

import java.util.List;
import java.util.Map;
import net.unixdeveloper.druwa.RequestCycle;
import net.unixdeveloper.druwa.RequestTarget;
import net.unixdeveloper.druwa.annotation.DefaultWebAction;
import net.unixdeveloper.druwa.annotation.WebAction;
import net.unixdeveloper.druwa.annotation.mount.WebModuleMountpoint;
import net.unixdeveloper.druwa.freemarker.FreemarkerRequestTarget;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.dabox.cocobox.cpweb.NavigationUtil;
import se.dabox.cocobox.cpweb.module.core.AbstractWebAuthModule;
import se.dabox.cocosite.module.core.AbstractCocositeJsModule;
import se.dabox.cocosite.org.MiniOrgInfo;
import se.dabox.cocosite.security.CocoboxPermissions;
import se.dabox.service.client.CacheClients;
import se.dabox.service.common.ccbc.NotFoundException;
import se.dabox.service.cug.client.ClientUserGroup;
import se.dabox.service.cug.client.ClientUserGroupClient;
import se.dabox.service.webutils.login.LoginUserAccountHelper;

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
        Long parentId = cug.getParent();
        checkOrgCUGAccess(cycle, org, cug);
        
        ClientUserGroup parentCug = null;
        
        if(parentId != null && parentId != 0) {
            parentCug = cugService.getGroup(parentId);
        }
        Map<String, Object> map = createMap();
        
        map.put("cug", cug);
        map.put("parentCug", parentCug);
        map.put("org", org);

        return new FreemarkerRequestTarget("/cug/cugOverviewMembers.html", map);
    }

    @WebAction
    public RequestTarget onChildren(RequestCycle cycle, String strOrgId, String strCugId) {

        MiniOrgInfo org = secureGetMiniOrg(cycle, strOrgId);
        checkOrgPermission(cycle, org.getId(), CocoboxPermissions.CP_VIEW_USER);
        ClientUserGroupClient cugService = getClientUserGroupService(cycle);
        ClientUserGroup cug = cugService.getGroup(Long.valueOf(strCugId));
        Long parentId = cug.getParent();
        checkOrgCUGAccess(cycle, org, cug);
        
        ClientUserGroup parentCug = null;
        if(parentId != null && parentId != 0) {
            parentCug = cugService.getGroup(parentId);
        }
        
        Map<String, Object> map = createMap();
        map.put("cug", cug);
        map.put("parentCug", parentCug);
        map.put("org", org);

        return new FreemarkerRequestTarget("/cug/cugOverviewChildren.html", map);
    }

    @WebAction
    public RequestTarget onDelete(RequestCycle cycle, String strOrgId, String strCugId) {
        long groupId = Long.valueOf(strCugId);
        MiniOrgInfo org = secureGetMiniOrg(cycle, strOrgId);
        checkOrgPermission(cycle, org.getId(), CocoboxPermissions.CP_VIEW_USER);
        ClientUserGroupClient cugService = getClientUserGroupService(cycle);
        ClientUserGroup cug = cugService.getGroup(groupId);
        List<ClientUserGroup> children = cugService.listChildren(groupId);
        
        Long parentId = cug.getParent();
        checkOrgCUGAccess(cycle, org, cug);

        Map<String, Object> map = createMap();

        if (!children.isEmpty()) {
            map.put("children", children.size());
        } else {
            //Delete the group
            cugService.deleteGroup(0L, groupId);
            if(parentId != null) {
                // Go to parent overview
                map.put("location", NavigationUtil.toClientUserGroupOverviewUrl(cycle, org.getId(), parentId));
            } else {
                // Go to org group list
                map.put("location", NavigationUtil.toOrgProjectsUrl(cycle, strOrgId));
            }
        }

        return AbstractCocositeJsModule.jsonTarget(map);
    }

    
    private ClientUserGroupClient getClientUserGroupService(RequestCycle cycle) {
        return CacheClients.getClient(cycle, ClientUserGroupClient.class);
    }

    private void checkOrgCUGAccess(RequestCycle cycle, MiniOrgInfo org, ClientUserGroup cug) {
        if(cug.getOrgId() != org.getId()) {
            handleAccessDenied(cycle, "Access denied to client user group from current organization.");
        }
    }
}
