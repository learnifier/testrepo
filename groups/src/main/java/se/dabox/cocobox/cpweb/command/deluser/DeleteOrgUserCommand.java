/*
 * (c) Dabox AB 2012 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.command.deluser;

import java.util.List;
import net.unixdeveloper.druwa.RequestCycle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.dabox.cocobox.cpweb.CpAdminRoles;
import se.dabox.cocosite.druwa.CocoSiteConstants;
import se.dabox.service.client.CacheClients;
import se.dabox.service.common.ccbc.CocoboxCoordinatorClient;
import se.dabox.cocobox.security.user.OrgRoleName;
import se.dabox.service.common.ccbc.project.ProjectParticipation;
import se.dabox.service.login.client.UserAccount;
import se.dabox.service.login.client.UserAccountService;

/**
 *
 * @author Jerker Klang (jerker.klang@dabox.se)
 */
public class DeleteOrgUserCommand {
    private static final Logger LOGGER =
            LoggerFactory.getLogger(DeleteOrgUserCommand.class);

    private final RequestCycle cycle;
    private final long caller;
    private final CocoboxCoordinatorClient ccbc;
    private final UserAccountService uaService;
    private final long orgId;

    public DeleteOrgUserCommand(RequestCycle cycle, long caller, long orgId) {
        this.cycle = cycle;
        this.caller = caller;
        this.orgId = orgId;
        this.ccbc = CacheClients.getClient(cycle, CocoboxCoordinatorClient.class);
        this.uaService = CacheClients.getClient(cycle, UserAccountService.class);
    }

    public DeleteStatus delete(long userId) {

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Delete org user {} attempted by {} on org {}",
                    new Object[]{userId, caller, orgId});
        }

        List<ProjectParticipation> parts = ccbc.listProjectParticipationsForUserId(userId);

        if (!parts.isEmpty()) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("User {} had {} participations. Unable to delete.", userId, parts.
                        size());
            }
            return DeleteStatus.HAS_PARTICIPATIONS;
        }

        String orgRole = OrgRoleName.forOrg(orgId).toString();

        UserAccount ua = uaService.getUserAccount(userId);

        String userOrgRole = ua.getProfileValue(CocoSiteConstants.UA_PROFILE,
                CocoSiteConstants.ADMIN_ROLE);
        
        if (userOrgRole != null && !CpAdminRoles.NONE.equals(orgRole)) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("User {} has admin role {}. Unable to delete", userId, orgRole);
            }
            return DeleteStatus.IS_ADMIN;
        }

        //OK We can now delete

        uaService.updateUserProfileValue(userId, CocoSiteConstants.UA_PROFILE,
                CocoSiteConstants.ADMIN_ROLE, null);

        return DeleteStatus.OK;
    }
}
