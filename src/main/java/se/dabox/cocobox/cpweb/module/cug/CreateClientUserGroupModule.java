/*
 * (c) Dabox AB 2012 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.module.cug;

import se.dabox.cocobox.cpweb.module.user.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import net.unixdeveloper.druwa.HttpMethod;
import net.unixdeveloper.druwa.RequestCycle;
import net.unixdeveloper.druwa.RequestTarget;
import net.unixdeveloper.druwa.annotation.WebAction;
import net.unixdeveloper.druwa.annotation.mount.WebModuleMountpoint;
import net.unixdeveloper.druwa.formbean.DruwaFormValidationSession;
import net.unixdeveloper.druwa.formbean.validation.ValidationConstraint;
import net.unixdeveloper.druwa.formbean.validation.ValidationError;
import net.unixdeveloper.druwa.freemarker.FreemarkerRequestTarget;
import net.unixdeveloper.druwa.request.RedirectUrlRequestTarget;
import net.unixdeveloper.druwa.request.WebModuleRedirectRequestTarget;
import net.unixdeveloper.druwa.util.UrlBuilder;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.dabox.cocobox.cpweb.CpAdminRoles;
import se.dabox.cocobox.cpweb.CpwebConstants;
import se.dabox.cocobox.cpweb.NavigationUtil;
import se.dabox.cocobox.cpweb.command.CreateCpUserAccountCommand;
import se.dabox.cocobox.cpweb.formdata.user.CreateUser;
import se.dabox.cocobox.cpweb.module.core.AbstractWebAuthModule;
import se.dabox.cocobox.cpweb.module.mail.UrlRequestTargetGenerator;
import se.dabox.cocobox.cpweb.state.SendMailSession;
import se.dabox.cocosite.druwa.CocoSiteConstants;
import se.dabox.cocosite.event.UserAccountChangedListenerUtil;
import se.dabox.cocosite.locale.FormLocale;
import se.dabox.cocosite.locale.PlatformFormLocaleFactory;
import se.dabox.cocosite.login.CocositeUserHelper;
import se.dabox.cocosite.modal.ModalParamsHelper;
import se.dabox.cocosite.org.MiniOrgInfo;
import se.dabox.cocosite.security.CocoboxPermissions;
import se.dabox.cocosite.security.UserAccountRoleCheck;
import se.dabox.cocosite.security.role.CocoboxRoleUtil;
import se.dabox.cocosite.security.role.RoleUuidNamePair;
import se.dabox.cocosite.user.sort.UserListSortComparator;
import se.dabox.service.client.CacheClients;
import se.dabox.service.common.ccbc.org.OrgRoleName;
import se.dabox.service.common.locale.GetUserDefaultLocaleCommand;
import se.dabox.service.common.tx.ValidationFailureException;
import se.dabox.service.common.tx.VerificationStatus;
import se.dabox.service.login.client.SetUserAccountNameRequest;
import se.dabox.service.login.client.UserAccount;
import se.dabox.service.login.client.UserAccountService;
import se.dabox.service.orgdir.client.OrgUnitInfo;
import se.dabox.service.orgdir.client.OrganizationDirectoryClient;
import se.dabox.service.webutils.login.LoginUserAccountHelper;

/**
 *
 * @author Jerker Klang (jerker.klang@dabox.se)
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
    public RequestTarget onCreate(RequestCycle cycle, String strOrgId) {
        MiniOrgInfo org = secureGetMiniOrg(cycle, strOrgId);
        checkOrgPermission(cycle, org.getId(), CocoboxPermissions.CP_CREATE_USER);

        String formLink = cycle.urlFor(CreateClientUserGroupModule.class, ACTION_DO_CREATE, strOrgId);
        DruwaFormValidationSession<CreateUser> formsess =
                getValidationSession(CreateUser.class, cycle);

        List<OrgUnitInfo> orgList = getOrgList(cycle, org, null);

        return genericCreateEditView(cycle, org, orgList, ModalParamsHelper.decorateUrl(cycle, formLink),
                formsess, false);
    }

    @WebAction
    public RequestTarget onEdit(RequestCycle cycle, String strOrgId, String strUserId) {
        MiniOrgInfo org = secureGetMiniOrg(cycle, strOrgId);
        checkOrgPermission(cycle, org.getId(), CocoboxPermissions.CP_EDIT_USER);

        UserAccount userAccount =
                getUserAccountService(cycle).getUserAccount(Long.valueOf(
                strUserId));

        if (userAccount == null) {
            return NavigationUtil.toOrgUsers(cycle, org.getId());
        }

        CreateUser form = new CreateUser();
        form.setEmail(userAccount.getPrimaryEmail());
        form.setFirstname(userAccount.getGivenName());
        form.setLastname(userAccount.getSurname());
        form.setLang(CocositeUserHelper.getUserAccountUserLocale(userAccount));
        form.setOrganization(userAccount.getOrganizationId());

        CharSequence orgRoleName = OrgRoleName.forOrg(org.getId());

        String role = userAccount.getProfileValue(CocoSiteConstants.UA_PROFILE, orgRoleName.
                toString());

        if (StringUtils.isBlank(role)) {
            role = CpAdminRoles.NONE;
        }

        form.setRole(role);

        String formLink = cycle.urlFor(CreateClientUserGroupModule.class, ACTION_DO_SAVE, strOrgId, strUserId);
        DruwaFormValidationSession<CreateUser> formsess =
                getValidationSession(CreateUser.class, cycle);
        formsess.populateFromObject(form);

        List<OrgUnitInfo> orgList = getOrgList(cycle, org, userAccount);

        return genericCreateEditView(cycle, org, orgList, formLink, formsess, true);
    }

    @WebAction(methods = HttpMethod.POST)
    public RequestTarget onDoSave(final RequestCycle cycle, String strOrgId, String strUserId) {
        MiniOrgInfo org = secureGetMiniOrg(cycle, strOrgId);
        final UserAccountService uaService = getUserAccountService(cycle);
        checkOrgPermission(cycle, org.getId(), CocoboxPermissions.CP_EDIT_USER);

        UserAccount userAccount =
                uaService.getUserAccount(Long.valueOf(
                strUserId));

        if (userAccount == null) {
            return NavigationUtil.toOrgUsers(cycle, org.getId());
        }

        DruwaFormValidationSession<CreateUser> formsess =
                getValidationSession(CreateUser.class, cycle);

        if (!formsess.process()) {
            return toEditUserPage(cycle, strOrgId, strUserId);
        }

        CreateUser form = formsess.getObject();
        final String newEmail = form.getEmail();

        boolean emailChanged = false;
        if (!userAccount.getPrimaryEmail().equals(newEmail)) {
            if (userExistsWithEmail(cycle, newEmail)) {
                formsess.addError(new ValidationError(ValidationConstraint.CONSISTENCY, "email",
                        "emailalreadyexists"));
                return toEditUserPage(cycle, strOrgId, strUserId);
            }
            emailChanged = true;
        }

        if (!isValidRole(form.getRole())) {
            formsess.addError(new ValidationError(ValidationConstraint.CONSISTENCY, "role",
                    "invalidrole"));
            return toEditUserPage(cycle, strOrgId, strUserId);
        }

        SetUserAccountNameRequest updateReq =
                new SetUserAccountNameRequest(userAccount.getUserId(),
                form.getFirstname(),
                form.getLastname());

        uaService.setUserAccountName(updateReq);

        uaService.updateUserProfileValue(userAccount.getUserId(),
                CocoSiteConstants.UA_PROFILE,
                CocoSiteConstants.UA_LOCALE,
                form.getLang().toString());

        if (emailChanged) {
            uaService.updateUserProfileValue(userAccount.getUserId(), "email", "email", form.
                    getEmail());
        }
        
        final Long formOrgId = form.getOrganization();
        if(formOrgId == 0) {
            // We are trying to set organization to null.
            if(userAccount.getOrganizationId() != null) { 
                if(UserAccountRoleCheck.isBoAdmin(LoginUserAccountHelper.getUserAccount(cycle)) || (org.getId() == userAccount.getOrganizationId())) {
                    // We are boAdmin, always ok to set to null || Only allow to change to null if home org is set to the users own org
                    uaService.setOrganization(userAccount.getUserId(), null);
                } 
                // Else silently do nothing; operation not permitted
            }
        } else {
            // We are trying to change to a new non-null organization
            if(!Objects.equals(userAccount.getOrganizationId(), formOrgId)) {
                if(UserAccountRoleCheck.isBoAdmin(LoginUserAccountHelper.getUserAccount(cycle)) || (org.getId() == formOrgId)) {
                    // Always ok for boAdmin || Allow to change to home org only
                    uaService.setOrganization(userAccount.getUserId(), formOrgId);
                } 
                // Else silently do nothing; operation is not permitted.
            }
        }
        UserAccountChangedListenerUtil.triggerEvent(cycle, userAccount.getUserId());

        //No need to decorate modal params here
        String directUrl = NavigationUtil.toUserPageUrl(cycle, strOrgId,userAccount.getUserId());
        String redirectUrl = createTopRedirectUrl(cycle, directUrl);
        return new RedirectUrlRequestTarget(redirectUrl);
    }

    @WebAction
    public RequestTarget onResendInvitation(RequestCycle cycle, String strOrgId, String strUserId) {
        MiniOrgInfo org = secureGetMiniOrg(cycle, strOrgId);

        UserAccount userAccount =
                getUserAccountService(cycle).getUserAccount(Long.valueOf(
                strUserId));

        if (userAccount == null) {
            return NavigationUtil.toOrgUsers(cycle, org.getId());
        }

        return sendAdminInvitationWithMailPage(cycle, org.getId(), userAccount.getUserId());
    }

    private RequestTarget genericCreateEditView(RequestCycle cycle, MiniOrgInfo org, List<OrgUnitInfo> orgList, String formLink,
            DruwaFormValidationSession<CreateUser> formsess, boolean editMode) {

        Map<String, Object> map = createMap();
        map.put("formsess", formsess);
        map.put("org", org);
        map.put("orgList", orgList);
        map.put("formLink", formLink);
        map.put("userLocales", getUserLocales(cycle));
        map.put("defaultUserLocale", getDefaultUserLocale(cycle));
        map.put("editMode", editMode);
        if (!editMode) {
            CocoboxRoleUtil cru = new CocoboxRoleUtil();
            List<RoleUuidNamePair> roles
                    = cru.toSortedRoleUuidNamePairList(cycle, cru.getCpRoles(cycle));
            map.put("roles", roles);
        }

        return new FreemarkerRequestTarget("/user/createUser.html", map);
    }

    @WebAction(methods = HttpMethod.POST)
    public RequestTarget onDoCreate(final RequestCycle cycle, String strOrgId) {
        final MiniOrgInfo org = secureGetMiniOrg(cycle, strOrgId);
        checkOrgPermission(cycle, org.getId(), CocoboxPermissions.CP_CREATE_USER);

        DruwaFormValidationSession<CreateUser> formsess =
                getValidationSession(CreateUser.class, cycle);

        if (!formsess.process()) {
            return toCreatePage(cycle, strOrgId);
        }

        final CreateUser form = formsess.getObject();

        UserAccount adminAccount;
        try {
            adminAccount = new CreateCpUserAccountCommand(cycle).createUserAccount(form.
                    getFirstname(), form.getLastname(), form.getEmail(), form.getLang(), org.
                    getId(),
                    form.getRole());
        } catch (ValidationFailureException vfe) {

            if (vfe.getVerificationStatus() == VerificationStatus.DUPLICATE) {
                formsess.addError(new ValidationError("duplicate", "email", null));
                return toCreatePage(cycle, strOrgId);
            }

            throw vfe;
        }
        
        if (!form.getRole().equals(CpAdminRoles.NONE)) {
            return sendAdminInvitationWithMailPage(cycle, org.getId(), adminAccount.getUserId());
        }

        String url = NavigationUtil.toOrgUsersUrl(cycle, strOrgId);
        return topRedirect(cycle, url);
    }

    private RequestTarget toCreatePage(RequestCycle cycle, String strOrgId) {
        WebModuleRedirectRequestTarget target
                = new WebModuleRedirectRequestTarget(CreateClientUserGroupModule.class, ACTION_VIEW_CREATE,
                        strOrgId);

        target.setExtraTargetParameterString(ModalParamsHelper.getParameterString(cycle));

        return target;
    }

    private RequestTarget createExistingAccountMail(RequestCycle cycle, UserAccount adminAccount,
            String strOrgId) {
        String userPageUrl = NavigationUtil.toUserPageUrl(cycle, strOrgId, adminAccount.getUserId());
        userPageUrl = createTopRedirectUrl(cycle, userPageUrl);

        SendMailSession sms = new SendMailSession(new AdminWelcomeMailProcessor(adminAccount.
                getUserId(), Long.valueOf(strOrgId)),
                new UrlRequestTargetGenerator(userPageUrl),
                new UrlRequestTargetGenerator(userPageUrl));

        sms.addReceiver(adminAccount.getUserId());

        sms.setStickyTemplateHint(CpwebConstants.ADMIN_WELCOME_MAIL_HINT);
        sms.setStickyTemplateLocale(CocositeUserHelper.getUserAccountUserLocale(adminAccount));
        sms.setStickyHidesDropdown(false);
        sms.setSkin(CpwebConstants.SKIN_MODAL_MAIL);

        sms.storeInSession(cycle);

        return sms.getPreSendTarget(Long.valueOf(strOrgId));
    }

    private RequestTarget createNewAccountMail(RequestCycle cycle, UserAccount adminAccount,
            String strOrgId) {
        String userPageUrl = NavigationUtil.toUserPageUrl(cycle, strOrgId, adminAccount.getUserId());
        userPageUrl = createTopRedirectUrl(cycle, userPageUrl);

        UrlRequestTargetGenerator target =
                new UrlRequestTargetGenerator(userPageUrl);

        UrlRequestTargetGenerator cancelTarget =
                new UrlRequestTargetGenerator(userPageUrl);

        SendMailSession sms = new SendMailSession(new AdminRegistrationMailProcessor(adminAccount.
                getUserId(), Long.valueOf(strOrgId)),
                target,
                cancelTarget);

        sms.addReceiver(adminAccount.getUserId());

        sms.setStickyTemplateHint(CpwebConstants.ADMIN_WELCOME_MAIL_HINT);
        sms.setStickyTemplateLocale(CocositeUserHelper.getUserAccountUserLocale(adminAccount));
        sms.setStickyHidesDropdown(false);
        sms.setSkin(CpwebConstants.SKIN_MODAL_MAIL);

        sms.storeInSession(cycle);

        return sms.getPreSendTarget(Long.valueOf(strOrgId));
    }

    private List<FormLocale> getUserLocales(RequestCycle cycle) {
        Locale userLocale = CocositeUserHelper.getUserLocale(cycle);

        return new PlatformFormLocaleFactory().getLocales(cycle, userLocale);
    }

    private Locale getDefaultUserLocale(RequestCycle cycle) {
        return new GetUserDefaultLocaleCommand().getLocale(cycle);
    }
    
    private List<OrgUnitInfo> getOrgList(RequestCycle cycle, MiniOrgInfo org, UserAccount userAccount) {
        OrganizationDirectoryClient odc = getOrganizationDirectoryClient(cycle);
        
        OrgUnitInfo nullOrg = new OrgUnitInfo();
        nullOrg.setId(0);
        nullOrg.setDisplayName("");
            
        // Return all organizations if we have are BO-admin
        if(UserAccountRoleCheck.isBoAdmin(LoginUserAccountHelper.getUserAccount(cycle))) {
            List<OrgUnitInfo> orgUnits = odc.listOrgUnits(CocoSiteConstants.OUTYPE_CLIENT);
            orgUnits.add(nullOrg);
            Collections.sort(orgUnits, new UserListSortComparator<OrgUnitInfo>(){
                @Override
                public int compare(OrgUnitInfo p1, OrgUnitInfo p2){
                    return collator.compare(p1.getDisplayName(), p2.getDisplayName());
                }
            });
            return orgUnits;
        }
        
        // New user, null org or org = home org -> allow null or home org
        if(userAccount == null || userAccount.getOrganizationId() == null || org.getId() == userAccount.getOrganizationId()) {
            return Arrays.asList(nullOrg, odc.getOrgUnitInfo(org.getId()));
        }
        
        // Org is set to something that is not null or the users home org -> do not allow change.
        return Collections.singletonList(odc.getOrgUnitInfo(org.getId()));
    }
        

    private boolean userExistsWithEmail(RequestCycle cycle, String email) {
        List<UserAccount> users =
                getUserAccountService(cycle).
                getUserAccountsByProfileSetting("email", "email", email);

        return !users.isEmpty();
    }

    private UserAccountService getUserAccountService(final RequestCycle cycle) {
        return CacheClients.getClient(cycle, UserAccountService.class);
    }

    private OrganizationDirectoryClient getOrganizationDirectoryClient(final RequestCycle cycle) {
        return CacheClients.getClient(cycle, OrganizationDirectoryClient.class);
    }

    private RequestTarget toEditUserPage(RequestCycle cycle, String strOrgId, String strUserId) {
        WebModuleRedirectRequestTarget target
                = new WebModuleRedirectRequestTarget(CreateClientUserGroupModule.class,
                        ACTION_VIEW_EDIT, strOrgId, strUserId);

        target.setExtraTargetParameterString(ModalParamsHelper.getParameterString(cycle));

        return target;
    }

    private RequestTarget sendAdminInvitationWithMailPage(RequestCycle cycle, long orgId, long userId) {
        UserAccount ua = getUserAccountService(cycle).getUserAccount(userId);

        String strOrgId = Long.toString(orgId);

        if (ua == null) {
            LOGGER.warn("User doesn't exist: {}", userId);
            return topRedirect(cycle, NavigationUtil.toOrgUsersUrl(cycle, strOrgId));
        }

        if (!UserAccountRoleCheck.isCpAdmin(ua, orgId)) {
            LOGGER.warn("User doesn't have a client admin role in ou {}", orgId);
            return topRedirect(cycle, NavigationUtil.toUserPageUrl(cycle, strOrgId, userId));
        }

        if (ua.isPasswordSet()) {
            LOGGER.debug("Password already set for new admin: {}", ua);
            return createExistingAccountMail(cycle, ua, strOrgId);
        } else {
            LOGGER.debug("Password not set for new admin: {}", ua);
            return createNewAccountMail(cycle, ua, strOrgId);
        }
    }

    private boolean isValidRole(String role) {
        return true;
    }

    private RedirectUrlRequestTarget redirect(RequestCycle cycle, String url) {
        String targetUrl = ModalParamsHelper.decorateUrl(cycle, url);
        return new RedirectUrlRequestTarget(targetUrl);
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
