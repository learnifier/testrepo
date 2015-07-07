/*
 * (c) Dabox AB 2015 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.module.cug;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import net.unixdeveloper.druwa.HttpMethod;
import net.unixdeveloper.druwa.RequestCycle;
import net.unixdeveloper.druwa.RequestTarget;
import net.unixdeveloper.druwa.annotation.WebAction;
import net.unixdeveloper.druwa.annotation.mount.WebModuleMountpoint;
import net.unixdeveloper.druwa.formbean.DruwaFormValidationSession;
import net.unixdeveloper.druwa.freemarker.FreemarkerRequestTarget;
import net.unixdeveloper.druwa.request.RedirectUrlRequestTarget;
import net.unixdeveloper.druwa.request.WebModuleRedirectRequestTarget;
import net.unixdeveloper.druwa.util.UrlBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.dabox.cocobox.cpweb.NavigationUtil;
import se.dabox.cocobox.cpweb.formdata.cug.CreateClientUserGroup;
import se.dabox.cocobox.cpweb.module.core.AbstractWebAuthModule;
import se.dabox.cocosite.modal.ModalParamsHelper;
import se.dabox.cocosite.org.MiniOrgInfo;
import se.dabox.cocosite.security.CocoboxPermissions;
import se.dabox.service.client.CacheClients;
import se.dabox.service.cug.client.ClientUserGroup;
import se.dabox.service.cug.client.ClientUserGroupClient;

/**
 *
 * @author Magnus Andersson (magnus.andersson@learnifier.com)
 */
@WebModuleMountpoint("/cug.create")
public class CreateClientUserGroupModule extends AbstractWebAuthModule {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(CreateClientUserGroupModule.class);
    public static final String ACTION_VIEW_CREATE = "create";
    public static final String ACTION_VIEW_EDIT = "edit";
    public static final String ACTION_DO_CREATE = "doCreate";
    public static final String ACTION_DO_SAVE = "doSave";    

    @WebAction
    public RequestTarget onCreate(RequestCycle cycle, String strOrgId, String strGroupId) {
        MiniOrgInfo org = secureGetMiniOrg(cycle, strOrgId);
        checkOrgPermission(cycle, org.getId(), CocoboxPermissions.CP_CREATE_USER);

        String formLink = cycle.urlFor(CreateClientUserGroupModule.class, ACTION_DO_CREATE, strOrgId);
        DruwaFormValidationSession<CreateClientUserGroup> formsess =
                getValidationSession(CreateClientUserGroup.class, cycle);

        CreateClientUserGroup form = new CreateClientUserGroup();

        try {
            Long groupId = Long.valueOf(strGroupId);
            form.setParent(groupId);
            formsess.populateFromObject(form);
        } catch (NumberFormatException e) {
            // NOOP
        }
    
        return genericCreateEditView(cycle, org, ModalParamsHelper.decorateUrl(cycle, formLink),
                formsess, false);
    }

    @WebAction
    public RequestTarget onEdit(RequestCycle cycle, String strOrgId, String strGroupId) {
        MiniOrgInfo org = secureGetMiniOrg(cycle, strOrgId);
        checkOrgPermission(cycle, org.getId(), CocoboxPermissions.CP_EDIT_USER);
        long groupId = Long.valueOf(strGroupId);
        ClientUserGroup cug = getClientUserGroupClient(cycle).getGroup(groupId);

        if (cug == null) {
            return NavigationUtil.toClientUserGroupOverview(cycle, org.getId(), groupId);
        }

        CreateClientUserGroup form = new CreateClientUserGroup();
        form.setName(cug.getName());

        String formLink = cycle.urlFor(CreateClientUserGroupModule.class, ACTION_DO_SAVE, strOrgId, strGroupId);
        DruwaFormValidationSession<CreateClientUserGroup> formsess =
                getValidationSession(CreateClientUserGroup.class, cycle);
        formsess.populateFromObject(form);

        return genericCreateEditView(cycle, org, formLink, formsess, true);
    }

    @WebAction(methods = HttpMethod.POST)
    public RequestTarget onDoSave(final RequestCycle cycle, String strOrgId, String strGroupId) {
        MiniOrgInfo org = secureGetMiniOrg(cycle, strOrgId);
        long groupId = Long.valueOf(strGroupId);
        checkOrgPermission(cycle, org.getId(), CocoboxPermissions.CP_EDIT_USER);
        ClientUserGroupClient cugClient = getClientUserGroupClient(cycle);
        ClientUserGroup cug = cugClient.getGroup(groupId);

        if (cug == null) {
            return NavigationUtil.toClientUserGroupOverview(cycle, org.getId(), groupId);
        }

        DruwaFormValidationSession<CreateClientUserGroup> formsess =
                getValidationSession(CreateClientUserGroup.class, cycle);

        if (!formsess.process()) {
            return toEditUserPage(cycle, strOrgId, strGroupId);
        }

        CreateClientUserGroup form = formsess.getObject();

        cugClient.updateGroupName(org.getId(), groupId, form.getName());

//        TODO: ???
//        UserAccountChangedListenerUtil.triggerEvent(cycle, userAccount.getUserId());

        String url = NavigationUtil.toClientUserGroupOverviewUrl(cycle, org.getId(), groupId);
        return topRedirect(cycle, url);
    }

    @WebAction(methods = HttpMethod.POST)
    public RequestTarget onDoCreate(final RequestCycle cycle, String strOrgId) {
        final MiniOrgInfo org = secureGetMiniOrg(cycle, strOrgId);
        checkOrgPermission(cycle, org.getId(), CocoboxPermissions.CP_CREATE_USER);

        DruwaFormValidationSession<CreateClientUserGroup> formsess =
                getValidationSession(CreateClientUserGroup.class, cycle);

        if (!formsess.process()) {
            return toCreatePage(cycle, strOrgId);
        }

        final CreateClientUserGroup form = formsess.getObject();

        ClientUserGroupClient cugClient = getClientUserGroupClient(cycle);

        long groupId = cugClient.createGroup(0L, org.getId(), form.getName(), form.getParent());
        
        String url = NavigationUtil.toClientUserGroupOverviewUrl(cycle, org.getId(), groupId);
        return topRedirect(cycle, url);
    }

    private RequestTarget genericCreateEditView(RequestCycle cycle, MiniOrgInfo org, String formLink,
            DruwaFormValidationSession<CreateClientUserGroup> formsess, boolean editMode) {

        Map<String, Object> map = createMap();
        map.put("formsess", formsess);
        map.put("org", org);
        map.put("formLink", formLink);
        map.put("editMode", editMode);

        return new FreemarkerRequestTarget("/cug/createClientUserGroup.html", map);
    }

    private RequestTarget toCreatePage(RequestCycle cycle, String strOrgId) {
        WebModuleRedirectRequestTarget target
                = new WebModuleRedirectRequestTarget(CreateClientUserGroupModule.class, ACTION_VIEW_CREATE,
                        strOrgId);

        target.setExtraTargetParameterString(ModalParamsHelper.getParameterString(cycle));

        return target;
    }

    private ClientUserGroupClient getClientUserGroupClient(final RequestCycle cycle) {
        return CacheClients.getClient(cycle, ClientUserGroupClient.class);
    }

    private RequestTarget toEditUserPage(RequestCycle cycle, String strOrgId, String strUserId) {
        WebModuleRedirectRequestTarget target
                = new WebModuleRedirectRequestTarget(CreateClientUserGroupModule.class,
                        ACTION_VIEW_EDIT, strOrgId, strUserId);

        target.setExtraTargetParameterString(ModalParamsHelper.getParameterString(cycle));

        return target;
    }

    private List<ClientUserGroup> getCugList(RequestCycle cycle, long orgId, Long selfGroupId) {
        List<ClientUserGroup> cugs =  getClientUserGroupClient(cycle).listGroups(orgId);
        if(selfGroupId != null) // Filter out self
            return cugs.stream().filter(cug -> cug.getGroupId() != selfGroupId).collect(Collectors.toList());
        return cugs;
    }
        
    private RedirectUrlRequestTarget topRedirect(RequestCycle cycle, String url) {
        return new RedirectUrlRequestTarget(createTopRedirectUrl(cycle, url));
    }

    private String createTopRedirectUrl(RequestCycle cycle, String url) {
        String target = cycle.getRequest().getContextPath()+"/_fr.html";
        UrlBuilder builder = new UrlBuilder(target);
        builder.addParameter("t", url);

        return builder.toString();
    }


}
