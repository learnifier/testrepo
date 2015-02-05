/*
 * (c) Dabox AB 2013 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.module.project.role;

import java.util.List;
import java.util.Locale;
import net.unixdeveloper.druwa.RequestCycle;
import net.unixdeveloper.druwa.RequestTarget;
import org.apache.commons.lang3.StringUtils;
import se.dabox.cocobox.cpweb.CpwebConstants;
import se.dabox.cocobox.cpweb.state.Receiver;
import se.dabox.cocobox.cpweb.state.SendMailSession;
import se.dabox.cocosite.login.CocositeUserHelper;
import se.dabox.cocosite.security.UserAccountRoleCheck;
import se.dabox.service.client.CacheClients;
import se.dabox.service.common.ccbc.CocoboxCoordinatorClient;
import se.dabox.service.common.ccbc.project.ProjectDetails;
import se.dabox.service.common.ccbc.project.role.ProjectUserRoleSearch;
import se.dabox.service.common.locale.GetUserDefaultLocaleCommand;
import se.dabox.service.login.client.UserAccount;
import se.dabox.service.login.client.UserAccountService;
import se.dabox.util.ParamUtil;
import se.dabox.util.collections.CollectionsUtil;

/**
 *
 * @author Jerker Klang (jerker.klang@dabox.se)
 */
public class AssignRoleCommand {
    private final RequestCycle cycle;
    private final String email;
    private final ProjectDetails project;
    private final String role;

    public AssignRoleCommand(RequestCycle cycle, ProjectDetails project, String email, String role) {
        this.cycle = cycle;
        this.email = StringUtils.trimToNull(email);
        this.project = project;
        this.role = role;
        ParamUtil.required(cycle, "cycle");
        ParamUtil.required(project, "project");
        ParamUtil.required(this.email, "email");
    }

    public RequestTarget assign() {
        UserType userType = determineUserType();

        return getTarget(userType);
    }

    private RequestTarget getTarget(UserType userType) throws IllegalStateException {
        RequestTarget target = null;

        switch(userType) {
            case NEW_USER:
                target = createAssignNewUserTarget();
                break;
            case EXISTING_ADMIN:
                target = createAssignExistingAdmin();
                break;
            case EXISTING_USER:
                target = createAssignExistingUser();
                break;
            default:
                throw new IllegalStateException("Unhandled user type: "+userType);
        }

        return target;
    }

    private UserType determineUserType() {
        UserAccount account = getMatchingUserAccount();

        if (account == null) {
            return UserType.NEW_USER;
        }

        boolean isAdmin = isAdmin(account);

        if (isAdmin) {
            return UserType.EXISTING_ADMIN;
        }

        return UserType.EXISTING_USER;
    }

    private RequestTarget createAssignNewUserTarget() {
        ProjectRolesRedirectTargetGenerator rolesPageTarget
                = new ProjectRolesRedirectTargetGenerator(project.getProjectId());

        AssignNewUserRoleProcessor processor = new AssignNewUserRoleProcessor(
                project.getProjectId(),
                email,
                role);

        SendMailSession sms = new SendMailSession(processor, rolesPageTarget, rolesPageTarget);
        sms.setStickyTemplateHint(CpwebConstants.PRJADMIN_WELCOME_MAIL_HINT);
        Locale mailLocale = getMailLocale();
        sms.setStickyTemplateLocale(mailLocale);
        sms.setStickyHidesDropdown(false);
        sms.addDisplayReceiver(new Receiver("", email));
        sms.setSkin(CpwebConstants.SKIN_MODAL_MAIL);

        sms.storeInSession(cycle);

        return sms.getPreSendTarget(project.getOrgId());
    }

    private RequestTarget createAssignExistingAdmin() {
        return createAssignExistingWithTemplate(CpwebConstants.PRJADMIN_ROLE_ASSIGNED_HINT);
    }

    private RequestTarget createAssignExistingUser() {
        return createAssignExistingWithTemplate(CpwebConstants.PRJADMIN_WELCOME_MAIL_HINT);
    }

    private RequestTarget createAssignExistingWithTemplate(String templateHint) {
        ProjectRolesRedirectTargetGenerator rolesPageTarget
                = new ProjectRolesRedirectTargetGenerator(project.getProjectId());

        AssignExistingUserRoleProcessor processor = new AssignExistingUserRoleProcessor(
                project.getProjectId(),
                getMatchingUserAccount().getUserId(),
                role);

        SendMailSession sms = new SendMailSession(processor, rolesPageTarget, rolesPageTarget);
        sms.setStickyTemplateHint(templateHint);
        Locale mailLocale = getMailLocale();
        sms.setStickyTemplateLocale(mailLocale);
        sms.setStickyHidesDropdown(false);
        sms.setSkin(CpwebConstants.SKIN_MODAL_MAIL);
        
        UserAccount matching = getMatchingUserAccount();
        
        sms.addReceiver(matching.getUserId());

        sms.storeInSession(cycle);

        return sms.getPreSendTarget(project.getOrgId());
    }

    private UserAccount getMatchingUserAccount() {
        UserAccountService uaService = CacheClients.getClient(cycle, UserAccountService.class);

        List<UserAccount> accounts
                = uaService.getUserAccountsByProfileSetting("email", "email",
                        email);

        return CollectionsUtil.singleItemOrNull(accounts);
    }

    private boolean isAdmin(UserAccount account) {
        if (UserAccountRoleCheck.isBoAdmin(account)) {
            return true;
        } else if (!UserAccountRoleCheck.getCpAdminOrgUnits(account).isEmpty()) {
            return true;
        } else if (isTrainer(account)) {
            return true;
        }

        return false;
    }

    private boolean isTrainer(UserAccount account) {
        ProjectUserRoleSearch search = ProjectUserRoleSearch.forUserId(account.getUserId());

        CocoboxCoordinatorClient ccbc = CacheClients.getClient(cycle, CocoboxCoordinatorClient.class);

        return !ccbc.searchProjectUserRoles(search).isEmpty();
    }

    private Locale getMailLocale() {
        UserAccount account = getMatchingUserAccount();
        if (account == null) {
            return determineRealmDefaultUserLocale();
        }
        return CocositeUserHelper.getUserAccountUserLocale(account);
    }

    private Locale determineRealmDefaultUserLocale() {
        return new GetUserDefaultLocaleCommand().getLocale(cycle);
    }

    private static enum UserType {
        NEW_USER,
        EXISTING_USER,
        EXISTING_ADMIN
    };
}
