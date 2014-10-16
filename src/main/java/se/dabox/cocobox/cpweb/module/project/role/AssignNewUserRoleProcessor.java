/*
 * (c) Dabox AB 2013 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.module.project.role;

import net.unixdeveloper.druwa.RequestCycle;
import se.dabox.cocobox.cpweb.state.SendMailSession;
import se.dabox.cocobox.cpweb.state.SendMailTemplate;

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
    }

}
