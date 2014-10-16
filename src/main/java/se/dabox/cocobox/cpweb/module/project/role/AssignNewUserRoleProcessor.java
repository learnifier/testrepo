/*
 * (c) Dabox AB 2013 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.module.project.role;

import net.unixdeveloper.druwa.RequestCycle;
import se.dabox.cocobox.cpweb.state.SendMailSession;
import se.dabox.cocobox.cpweb.state.SendMailTemplate;
import se.dabox.service.client.CacheClients;
import se.dabox.service.login.client.AlreadyExistsException;
import se.dabox.service.login.client.CreateBasicUserAccountRequest;
import se.dabox.service.login.client.UserAccount;
import se.dabox.service.login.client.UserAccountService;
import se.dabox.service.orgadmin.client.ActivateCpAdminTokenGenerator;

/**
 *
 * @author Jerker Klang (jerker.klang@dabox.se)
 */
public class AssignNewUserRoleProcessor extends AbstractRoleProcessor {
    private static final long serialVersionUID = 1L;

    private final String email;
    
    AssignNewUserRoleProcessor(long projectId, String email, String role) {
        super(projectId, role);
        this.email = email;
    }

    @Override
    public void processSendMail(RequestCycle cycle, SendMailSession sms, SendMailTemplate smt) {
        //Remember to not send the edited email. It should be used in the after-registration phase.

        UserAccountService uaClient = CacheClients.getClient(cycle, UserAccountService.class);

        CreateBasicUserAccountRequest req = CreateBasicUserAccountRequest.createEmailAccount(email);

        UserAccount account = null;

        try {
            account = uaClient.createBasicUserAccount(req);
        } catch (AlreadyExistsException aee) {
            account = uaClient.getSingleUserAccountByEmail(email);
        }

    }

}
