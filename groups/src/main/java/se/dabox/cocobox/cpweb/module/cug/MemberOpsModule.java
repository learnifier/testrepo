/*
 * (c) Dabox AB 2015 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.module.cug;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import net.unixdeveloper.druwa.RequestCycle;
import net.unixdeveloper.druwa.RequestTarget;
import net.unixdeveloper.druwa.WebRequest;
import net.unixdeveloper.druwa.annotation.WebAction;
import net.unixdeveloper.druwa.annotation.mount.WebModuleMountpoint;
import net.unixdeveloper.druwa.request.RedirectUrlRequestTarget;
import se.dabox.cocobox.cpweb.NavigationUtil;
import se.dabox.cocobox.security.permission.CocoboxPermissions;
import se.dabox.cocosite.org.MiniOrgInfo;
import se.dabox.service.cug.client.ClientUserGroup;
import se.dabox.service.cug.client.ClientUserGroupClient;

/**
 *
 * @author Magnus Andersson (magnus.andersson@learnifier.com)
 */
@WebModuleMountpoint("/cug")
public class MemberOpsModule extends AbstractUserClientGroupModule {

    final Map<String, MemberOpsInterface> commands;
    public MemberOpsModule() {
        MemberOpsModule self = this; 
        commands = new HashMap<String, MemberOpsInterface>() {{
            put("removeMembers", self::removeMember); // Why can't I use this::removeMember
        }};
    }

    @WebAction
    public RequestTarget onMemberOps(RequestCycle cycle, String strOrgId, String strCugId) {
        long groupId = Long.valueOf(strCugId);
        MiniOrgInfo org = secureGetMiniOrg(cycle, strOrgId);
        checkOrgPermission(cycle, org.getId(), CocoboxPermissions.CP_VIEW_USER);
        ClientUserGroupClient cugService = getClientUserGroupService(cycle);
        ClientUserGroup cug = cugService.getGroup(groupId);
        checkOrgCUGAccess(cycle, org, cug);

        final String COMMAND_VARIABLE = "__command";
        final String IDS_VARIABLE = "__ids";

        WebRequest request = cycle.getRequest();
        String cmd = (String)request.getParameter(COMMAND_VARIABLE);
        String idString = request.getParameter(IDS_VARIABLE);
        
        int[] ids = Arrays.stream(idString.split(","))
                      .map(String::trim)
                      .mapToInt(Integer::parseInt).toArray();
        
        return commands.get("removeMembers").call(cycle, org, cug, ids);
        
    }

    
    public RequestTarget removeMember(RequestCycle cycle, MiniOrgInfo org, ClientUserGroup cug, int[] ids) {
        Map<String, Object> map = createMap();
        long groupId = cug.getGroupId();
        ClientUserGroupClient cugService = getClientUserGroupService(cycle);
        for(long id: ids) {
            cugService.removeGroupMember(0L, groupId, id);
        }
        
        return new RedirectUrlRequestTarget(NavigationUtil.toClientUserGroupOverviewUrl(cycle, org.getId(), groupId));
    }

}
