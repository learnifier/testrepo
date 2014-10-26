/*
 * (c) Dabox AB 2013 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.module.project.role;

import net.unixdeveloper.druwa.RequestCycle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.dabox.cocobox.cpweb.state.SendMailSession;
import se.dabox.cocobox.cpweb.state.SendMailTemplate;
import se.dabox.service.client.CacheClients;
import se.dabox.service.common.ccbc.CocoboxCoordinatorClient;
import se.dabox.service.common.ccbc.project.role.ProjectUserRoleModification;
import se.dabox.service.webutils.login.LoginUserAccountHelper;

/**
 *
 * @author Jerker Klang (jerker.klang@dabox.se)
 */
public class AssignExistingUserRoleProcessor extends AbstractRoleProcessor {
    private static final Logger LOGGER =
            LoggerFactory.getLogger(AssignExistingUserRoleProcessor.class);

    private static final long serialVersionUID = 1L;

    private final long userId;
    
    public AssignExistingUserRoleProcessor(long projectId, long userId, String roleId) {
        super(projectId, roleId);
        this.userId = userId;        
    }

    @Override
    public void processSendMail(RequestCycle cycle, SendMailSession sms, SendMailTemplate smt) {
        long caller = LoginUserAccountHelper.getCurrentCaller();

        final long projectId = getProjectId();
        final String roleId = getRoleId();

        ProjectUserRoleModification mod = new ProjectUserRoleModification(caller, projectId, userId,
                roleId);

        CocoboxCoordinatorClient ccbc = CacheClients.getClient(cycle, CocoboxCoordinatorClient.class);
        boolean response = ccbc.grantProjectUserRole(mod);

        LOGGER.info("Adding role {} in project {} for {} (caller {}): {}",
                roleId, projectId, userId, caller, response);

        sendMail(cycle, sms, smt, userId);
    }

}
