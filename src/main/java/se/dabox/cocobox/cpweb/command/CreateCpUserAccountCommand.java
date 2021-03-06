/*
 * (c) Dabox AB 2013 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.command;

import java.util.List;
import java.util.Locale;

import com.google.common.collect.ImmutableMap;
import net.unixdeveloper.druwa.RequestCycle;
import se.dabox.cocobox.security.CocoboxSecurityConstants;
import se.dabox.cocobox.security.user.UserAccountRoleCheck;
import se.dabox.cocobox.cpweb.CpAdminRoles;
import se.dabox.cocosite.druwa.CocoSiteConstants;
import se.dabox.service.client.CacheClients;
import se.dabox.cocobox.security.user.OrgRoleName;
import se.dabox.service.common.RealmId;
import se.dabox.service.common.ccbc.material.ImmutableOrgMaterial;
import se.dabox.service.common.context.DwsRealmHelper;
import se.dabox.service.common.tx.UTComplexTxOperation;
import se.dabox.service.common.tx.VerificationStatus;
import se.dabox.service.login.client.CreateBasicUserAccountRequest;
import se.dabox.service.login.client.UserAccount;
import se.dabox.service.login.client.UserAccountService;
import se.dabox.service.webtracking.WebTracking;
import se.dabox.service.webtracking.WebTrackingMessage;
import se.dabox.service.webutils.login.LoginUserAccountHelper;

/**
 *
 * @author Jerker Klang (jerker.klang@dabox.se)
 */
public class CreateCpUserAccountCommand {

    private final RequestCycle cycle;

    public CreateCpUserAccountCommand(RequestCycle cycle) {
        this.cycle = cycle;
    }

    public UserAccount createUserAccount(final String firstName, final String lastName,
            final String email,
            final Locale locale, final long orgId, final String role) {

        UTComplexTxOperation<Void, UserAccount> op = new UTComplexTxOperation<Void, UserAccount>() {
            private UserAccountService uas;
            private UserAccount oldAccount;

            @Override
            protected void lock() {
            }

            @Override
            protected void preVerification() {
                uas = CacheClients.getClient(cycle, UserAccountService.class);
            }

            @Override
            protected VerificationStatus verifyState() {
                List<UserAccount> accounts = uas.getUserAccountsByProfileSetting(
                        "email",
                        "email",
                        email);

                if (accounts.isEmpty()) {
                    //No existing user with the e-mail exists. That is good.
                    return null;
                }

                if (accounts.size() > 1) {
                    return VerificationStatus.DATAFAILURE;
                }

                if (userAccountHasOrgRole(accounts, orgId)) {
                    return VerificationStatus.DUPLICATE;
                } else if (CpAdminRoles.NONE.equals(role)) {
                    //Also considered a duplicate
                    return VerificationStatus.DUPLICATE;
                }

                this.oldAccount = accounts.get(0);

                return null;
            }

            @Override
            protected boolean verifyVersion() {
                return true;
            }

            @Override
            protected UserAccount performOperation() {
                UserAccount userAccount = oldAccount;
                if (userAccount == null) {
                    CreateBasicUserAccountRequest req = CreateBasicUserAccountRequest.
                            createAccountWithoutPw(
                                    firstName,
                                    lastName,
                                    email);

                    userAccount = uas.createBasicUserAccount(req);

                    // Track to segment
                    WebTracking.simpleEvent(cycle, LoginUserAccountHelper.getUserId(cycle), DwsRealmHelper.determineRequestRealmId(cycle),
                            "inviteAdmin",
                            ImmutableMap.of(
                                    "email", email,
                                    "firstName", firstName,
                                    "lastName", lastName
                            ));
                }

                uas.addUserRole(userAccount.getUserId(), CocoboxSecurityConstants.USER_ROLE);

                addAdminRole(userAccount);

                uas.updateUserProfileValue(userAccount.getUserId(),
                        CocoSiteConstants.UA_PROFILE,
                        CocoSiteConstants.UA_LOCALE,
                        locale.toLanguageTag());

                return uas.getUserAccount(userAccount.getUserId());
            }

            private void addAdminRole(UserAccount userAccount) {
                CharSequence orgRoleName = OrgRoleName.forOrg(orgId);

                uas.updateUserProfileValue(userAccount.getUserId(),
                        CocoSiteConstants.UA_PROFILE,
                        orgRoleName,
                        role);
            }

            @Override
            protected void releaseLock() {
            }

            private boolean userAccountHasOrgRole(List<UserAccount> ua, long id) {
                return UserAccountRoleCheck.isCpAdmin(ua.get(0), orgId);
            }
        };

        return op.call(null);
    }

}
