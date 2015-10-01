/*
 * (c) Dabox AB 2012 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.module.user;

import java.util.List;
import java.util.Locale;
import java.util.Map;
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
import se.dabox.cocosite.org.MiniOrgInfo;
import se.dabox.cocobox.security.permission.CocoboxPermissions;
import se.dabox.cocobox.security.role.CocoboxRoleUtil;
import se.dabox.cocobox.security.role.RoleUuidNamePair;
import se.dabox.cocobox.security.user.UserAccountRoleCheck;
import se.dabox.service.client.CacheClients;
import se.dabox.cocobox.security.user.OrgRoleName;
import se.dabox.service.common.locale.GetUserDefaultLocaleCommand;
import se.dabox.service.common.tx.ValidationFailureException;
import se.dabox.service.common.tx.VerificationStatus;
import se.dabox.service.login.client.SetUserAccountNameRequest;
import se.dabox.service.login.client.UserAccount;
import se.dabox.service.login.client.UserAccountService;

/**
 *
 * @author Jerker Klang (jerker.klang@dabox.se)
 */
@WebModuleMountpoint("/user.create")
public class CreateUserModule extends AbstractWebAuthModule {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(CreateUserModule.class);
    public static final String ACTION_VIEW_CREATE = "create";
    public static final String ACTION_VIEW_EDIT = "edit";
    public static final String ACTION_DO_CREATE = "doCreate";
    public static final String ACTION_DO_SAVE = "doSave";    

    @WebAction
    public RequestTarget onCreate(RequestCycle cycle, String strOrgId) {
        MiniOrgInfo org = secureGetMiniOrg(cycle, strOrgId);
        checkOrgPermission(cycle, org.getId(), CocoboxPermissions.CP_CREATE_USER);

        String formLink = cycle.urlFor(CreateUserModule.class, ACTION_DO_CREATE, strOrgId);
        DruwaFormValidationSession<CreateUser> formsess =
                getValidationSession(CreateUser.class, cycle);

        return genericCreateEditView(cycle, org, formLink, formsess, false);
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

        CharSequence orgRoleName = OrgRoleName.forOrg(org.getId());

        String role = userAccount.getProfileValue(CocoSiteConstants.UA_PROFILE, orgRoleName.
                toString());

        if (StringUtils.isBlank(role)) {
            role = CpAdminRoles.NONE;
        }

        form.setRole(role);

        String formLink = cycle.urlFor(CreateUserModule.class, ACTION_DO_SAVE, strOrgId, strUserId);
        DruwaFormValidationSession<CreateUser> formsess =
                getValidationSession(CreateUser.class, cycle);
        formsess.populateFromObject(form);

        return genericCreateEditView(cycle, org, formLink, formsess, true);
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
            return toEditUserPage(strOrgId, strUserId);
        }

        CreateUser form = formsess.getObject();
        final String newEmail = form.getEmail();

        boolean emailChanged = false;
        if (!userAccount.getPrimaryEmail().equals(newEmail)) {
            if (userExistsWithEmail(cycle, newEmail)) {
                formsess.addError(new ValidationError(ValidationConstraint.CONSISTENCY, "email",
                        "emailalreadyexists"));
                return toEditUserPage(strOrgId, strUserId);
            }
            emailChanged = true;
        }

        if (!isValidRole(form.getRole())) {
            formsess.addError(new ValidationError(ValidationConstraint.CONSISTENCY, "role",
                    "invalidrole"));
            return toEditUserPage(strOrgId, strUserId);
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

        UserAccountChangedListenerUtil.triggerEvent(cycle, userAccount.getUserId());

        return new RedirectUrlRequestTarget(NavigationUtil.toUserPageUrl(cycle, strOrgId,
                userAccount.getUserId()));
    }

    @WebAction(methods = HttpMethod.POST)
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

    private RequestTarget genericCreateEditView(RequestCycle cycle, MiniOrgInfo org, String formLink,
            DruwaFormValidationSession<CreateUser> formsess, boolean editMode) {

        Map<String, Object> map = createMap();

        map.put("formsess", formsess);
        map.put("org", org);
        map.put("formLink", formLink);
        map.put("userLocales", getUserLocales(cycle));
        map.put("defaultUserLocale", getDefaultUserLocale(cycle));
        map.put("editMode", editMode);
        if (!editMode) {

            Locale sortLocale = CocositeUserHelper.getUserLocale(cycle);
            CocoboxRoleUtil cru = new CocoboxRoleUtil();
            List<RoleUuidNamePair> roles
                    = cru.toSortedRoleUuidNamePairList(cru.getCpRoles(cycle), sortLocale);
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
            return new WebModuleRedirectRequestTarget(CreateUserModule.class, ACTION_VIEW_CREATE,
                    strOrgId);
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
                return new WebModuleRedirectRequestTarget(CreateUserModule.class,
                        ACTION_VIEW_CREATE, strOrgId);
            }

            throw vfe;
        }
        
        if (!form.getRole().equals(CpAdminRoles.NONE)) {
            return sendAdminInvitationWithMailPage(cycle, org.getId(), adminAccount.getUserId());
        }

        return new RedirectUrlRequestTarget(NavigationUtil.toOrgUsersUrl(cycle, strOrgId));
    }

    private RequestTarget createExistingAccountMail(RequestCycle cycle, UserAccount adminAccount,
            String strOrgId) {
        String userPageUrl = NavigationUtil.toUserPageUrl(cycle, strOrgId, adminAccount.getUserId());

        SendMailSession sms = new SendMailSession(new AdminWelcomeMailProcessor(adminAccount.
                getUserId(), Long.valueOf(strOrgId)),
                new UrlRequestTargetGenerator(userPageUrl),
                new UrlRequestTargetGenerator(userPageUrl));

        sms.addReceiver(adminAccount.getUserId());

        sms.setStickyTemplateHint(CpwebConstants.ADMIN_WELCOME_MAIL_HINT);
        sms.setStickyTemplateLocale(CocositeUserHelper.getUserAccountUserLocale(adminAccount));
        sms.setStickyHidesDropdown(false);

        sms.storeInSession(cycle);

        return sms.getPreSendTarget(Long.valueOf(strOrgId));
    }

    private RequestTarget createNewAccountMail(RequestCycle cycle, UserAccount adminAccount,
            String strOrgId) {
        String userPageUrl = NavigationUtil.toUserPageUrl(cycle, strOrgId, adminAccount.getUserId());

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

    private boolean userExistsWithEmail(RequestCycle cycle, String email) {
        List<UserAccount> users =
                getUserAccountService(cycle).
                getUserAccountsByProfileSetting("email", "email", email);

        return !users.isEmpty();
    }

    private UserAccountService getUserAccountService(final RequestCycle cycle) {
        return CacheClients.getClient(cycle, UserAccountService.class);
    }

    private RequestTarget toEditUserPage(String strOrgId, String strUserId) {
        return new WebModuleRedirectRequestTarget(CreateUserModule.class,
                ACTION_VIEW_EDIT, strOrgId, strUserId);
    }

    private RequestTarget sendAdminInvitationWithMailPage(RequestCycle cycle, long orgId, long userId) {
        UserAccount ua = getUserAccountService(cycle).getUserAccount(userId);

        String strOrgId = Long.toString(orgId);

        if (ua == null) {
            LOGGER.warn("User doesn't exist: {}", userId);
            return new RedirectUrlRequestTarget(NavigationUtil.toOrgUsersUrl(cycle, strOrgId));
        }

        if (!UserAccountRoleCheck.isCpAdmin(ua, orgId)) {
            LOGGER.warn("User doesn't have a client admin role in ou {}", orgId);
            return new RedirectUrlRequestTarget(NavigationUtil.toUserPageUrl(cycle, strOrgId, userId));
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
}
