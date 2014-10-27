/*
 * (c) Dabox AB 2013 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.module.project.role;

import java.util.Set;
import net.unixdeveloper.druwa.RequestCycle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.dabox.cocobox.cpweb.CpAdminRoles;
import se.dabox.cocosite.druwa.CocoSiteConstants;
import se.dabox.cocosite.security.UserAccountRoleCheck;
import se.dabox.service.client.CacheClients;
import se.dabox.service.common.ccbc.org.OrgRoleName;
import se.dabox.service.login.client.UserAccount;
import se.dabox.service.login.client.UserAccountService;
import se.dabox.util.ParamUtil;

/**
 * Adds a "none" permission for the orgunit if the specified user does not have any
 * previous for the orgunit.
 *
 * @author Jerker Klang (jerker.klang@dabox.se)
 */
class AddMissingClientAccessRole {
    private static final Logger LOGGER =
            LoggerFactory.getLogger(AddMissingClientAccessRole.class);

    private final RequestCycle cycle;

    public AddMissingClientAccessRole(RequestCycle cycle) {
        ParamUtil.required(cycle, "cycle");
        this.cycle = cycle;
    }
    
    /**
     * Add an org unit access role (set to {@code CpAdminRoles.NONE}) if the specified account
     * has no previous roles for the specified orgunit 
     * 
     * @param account The user account
     * @param orgId The org id that a role should exist for
     * @return True if a role was added; false if role already existed.
     * 
     * @see CpAdminRoles#NONE
     */
    public boolean addOrgUnitAccess(UserAccount account, long orgId) {
        ParamUtil.required(account, "account");
        
        Set<String> roles = UserAccountRoleCheck.getCpRoles(account, orgId, true);
        if (!roles.isEmpty()) {
            LOGGER.debug("User {} already have roles in orgunit {}: {}", account.getUserId(), orgId,
                    roles);
            return false ;
        }

        CharSequence orgRoleName = OrgRoleName.forOrg(orgId);
        String role = CpAdminRoles.NONE;

        LOGGER.info("Adding role 'none' for user {} and orgunit {}", account.getUserId(), orgId);
        
        UserAccountService uaService = CacheClients.getClient(cycle, UserAccountService.class);
        uaService.updateUserProfileValue(account.getUserId(),
                CocoSiteConstants.UA_PROFILE,
                orgRoleName,
                role);
        
        return true;
    }

    
    /**
     * Add an org unit access role (set to {@code CpAdminRoles.NONE}) if the specified user id
     * has no previous roles for the specified orgunit 
     * 
     * @param userId The user id
     * @param orgId The org id that a role should exist for
     * @return True if a role was added; false if role already existed.
     * 
     * @see CpAdminRoles#NONE
     */
    public boolean addOrgUnitAccess(long userId, long orgId) {
        UserAccountService uaClient = CacheClients.getClient(cycle, UserAccountService.class);

        UserAccount account = uaClient.getUserAccount(userId);

        return addOrgUnitAccess(account, orgId);
    }

}
