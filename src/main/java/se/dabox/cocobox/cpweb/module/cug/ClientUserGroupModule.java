/*
* (c) Dabox AB 2015 All Rights Reserved
*/
package se.dabox.cocobox.cpweb.module.cug;

import java.util.List;
import java.util.Map;
import net.unixdeveloper.druwa.HttpMethod;
import net.unixdeveloper.druwa.RequestCycle;
import net.unixdeveloper.druwa.RequestTarget;
import net.unixdeveloper.druwa.annotation.DefaultWebAction;
import net.unixdeveloper.druwa.annotation.WebAction;
import net.unixdeveloper.druwa.annotation.mount.WebModuleMountpoint;
import net.unixdeveloper.druwa.formbean.DruwaFormValidationSession;
import net.unixdeveloper.druwa.formbean.validation.ValidationConstraint;
import net.unixdeveloper.druwa.formbean.validation.ValidationError;
import net.unixdeveloper.druwa.freemarker.FreemarkerRequestTarget;
import net.unixdeveloper.druwa.request.WebModuleRedirectRequestTarget;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.dabox.cocobox.cpweb.NavigationUtil;
import se.dabox.cocobox.cpweb.formdata.cug.AddCugMemberForm;
import se.dabox.cocobox.cpweb.formdata.project.AddMemberForm;
import se.dabox.cocosite.module.core.AbstractCocositeJsModule;
import se.dabox.cocosite.org.MiniOrgInfo;
import se.dabox.cocosite.security.CocoboxPermissions;
import se.dabox.service.client.Clients;
import se.dabox.service.cug.client.ClientUserGroup;
import se.dabox.service.cug.client.ClientUserGroupClient;
import se.dabox.service.login.client.MultipleResponseException;
import se.dabox.service.login.client.UserAccount;
import se.dabox.service.login.client.UserAccountService;
import se.dabox.service.webutils.login.LoginUserAccountHelper;
import se.dabox.util.email.SimpleEmailValidator;

/**
 *
 * @author Magnus Andersson (magnus.andersson@learnifier.com)
 */
@WebModuleMountpoint("/cug")
public class ClientUserGroupModule extends AbstractUserClientGroupModule {
    private static final Logger LOGGER =
            LoggerFactory.getLogger(ClientUserGroupModule.class);
    
    public static final String OVERVIEW_ACTION = "overview";
    public static final String CHILDREN_ACTION = "children";
    
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
        
        map.put("formsess", getValidationSession(AddMemberForm.class, cycle));
        
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
        long caller = LoginUserAccountHelper.getUserId(cycle);
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
            cugService.deleteGroup(caller, groupId);
            if(parentId != null && parentId != 0) {
                // Go to parent overview
                map.put("location", NavigationUtil.toClientUserGroupChildrenUrl(cycle, org.getId(), parentId));
            } else {
                // Go to org group list
                map.put("location", NavigationUtil.toClientUserGroupListUrl(cycle, org.getId()));
            }
        }
        
        return AbstractCocositeJsModule.jsonTarget(map);
    }
    
    @WebAction(methods = HttpMethod.POST)
    public RequestTarget onAddMember(RequestCycle cycle, String strOrgId, String strCugId) {
        long groupId = Long.valueOf(strCugId);
        long caller = LoginUserAccountHelper.getUserId(cycle);
        
        MiniOrgInfo org = secureGetMiniOrg(cycle, strOrgId);
        checkOrgPermission(cycle, org.getId(), CocoboxPermissions.CP_VIEW_USER);
        ClientUserGroupClient cugService = getClientUserGroupService(cycle);
        ClientUserGroup cug = cugService.getGroup(groupId);
        
        checkOrgCUGAccess(cycle, org, cug);
        
        DruwaFormValidationSession<AddCugMemberForm> sess = getValidationSession(AddCugMemberForm.class,
                cycle);
        
        sess.setTransferToViewSession(true);
        
        if (!sess.process()) {
            return toMemberOverview(strOrgId, strCugId);
        }
        
        AddCugMemberForm form = sess.getObject();
        
        if (!SimpleEmailValidator.getInstance().isValidEmail(form.getMemberemail())) {
            sess.addError(new ValidationError(ValidationConstraint.EMAIL_FORMAT, "memberemail", "email.invalid.input"));
        }
        
        if (!sess.process()) {
            LOGGER.info("Invalid email: {}", form.getMemberemail());
            return toMemberOverview(strOrgId, strCugId);
        }
        
        try {
            UserAccount ua = Clients.getClient(cycle, UserAccountService.class).
                    getSingleUserAccountByEmail(form.getMemberemail());
            List<UserAccount> uas = cugService.listGroupMembers(groupId);
            if(uas.stream().anyMatch(u -> u.hasEmailAddress(form.getMemberemail()))) {
                LOGGER.info("Account with email already present in group: {}", form.getMemberemail());
            } else {
                cugService.addGroupMember(caller, groupId, ua.getUserId());
            }
        } catch(IllegalArgumentException e) {
            LOGGER.info("Invalid email: {}", form.getMemberemail());
        } catch (MultipleResponseException e) {
            LOGGER.info("Multiple user accounts with same email found: {}", form.getMemberemail());
        }
        return toMemberOverview(strOrgId, strCugId);
    }
    
    private WebModuleRedirectRequestTarget toMemberOverview(String orgId, String groupId) {
        return new WebModuleRedirectRequestTarget(ClientUserGroupModule.class, OVERVIEW_ACTION,
                orgId, groupId);
    }
}


