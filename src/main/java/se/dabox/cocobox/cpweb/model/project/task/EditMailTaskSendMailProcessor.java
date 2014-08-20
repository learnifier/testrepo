/*
 * (c) Dabox AB 2013 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.model.project.task;

import java.io.Serializable;
import net.unixdeveloper.druwa.RequestCycle;
import se.dabox.cocobox.cpweb.module.mail.MailSender;
import se.dabox.cocobox.cpweb.module.mail.SendMailProcessor;
import se.dabox.cocobox.cpweb.state.SendMailSession;
import se.dabox.cocobox.cpweb.state.SendMailTemplate;
import se.dabox.service.client.CacheClients;
import se.dabox.service.common.ccbc.CocoboxCordinatorClient;
import se.dabox.service.common.ccbc.UpdateProjectTaskRequest;
import se.dabox.service.common.mailsender.pmt.PortableMailTemplate;
import se.dabox.service.common.mailsender.pmt.PortableMailTemplateCodec;
import se.dabox.service.webutils.login.LoginUserAccountHelper;

/**
 *
 * @author Jerker Klang (jerker.klang@dabox.se)
 */
public class EditMailTaskSendMailProcessor extends AbstractMailSendMailProcessor {

    private static final long serialVersionUID = 2L;

    private final long taskId;

    public EditMailTaskSendMailProcessor(long taskId) {
        this.taskId = taskId;
    }

    @Override
    public void processSendMail(RequestCycle cycle, SendMailSession sms, SendMailTemplate smt) {

        long caller = LoginUserAccountHelper.getUserId(cycle);

        PortableMailTemplate pmt = createPortableMailTemplate(cycle, smt);
        String pmtString = PortableMailTemplateCodec.encode(pmt, true);

        UpdateProjectTaskRequest update = UpdateProjectTaskRequest.
                updatePortableMailTemplate(caller, taskId, pmtString);

        CocoboxCordinatorClient ccbcClient
                = CacheClients.getClient(cycle, CocoboxCordinatorClient.class);

        ccbcClient.updateScheduledProjectTask(update);
    }

    @Override
    public MailSender getMailSender(RequestCycle cycle) {
        return null;
    }

}
