/*
 * (c) Dabox AB 2013 All Rights Reserved
 */

package se.dabox.cocobox.cpweb.model.project.task;

import java.util.Date;
import net.unixdeveloper.druwa.RequestCycle;
import se.dabox.cocobox.cpweb.module.mail.MailSender;
import se.dabox.cocobox.cpweb.state.SendMailSession;
import se.dabox.cocobox.cpweb.state.SendMailTemplate;
import se.dabox.service.client.CacheClients;
import se.dabox.service.common.ccbc.CocoboxCordinatorClient;
import se.dabox.service.common.ccbc.NewProjectTaskRequest;
import se.dabox.service.common.ccbc.mailfilter.MailFilterTarget;
import se.dabox.service.common.mailsender.pmt.PortableMailTemplate;
import se.dabox.service.common.mailsender.pmt.PortableMailTemplateCodec;
import se.dabox.service.webutils.login.LoginUserAccountHelper;

/**
 *
 * @author Jerker Klang <jerker.klang@dabox.se>
 */
public class CreateMailTaskSendMailProcessor extends AbstractMailSendMailProcessor {
    private static final long serialVersionUID = 2L;

    private final MailFilterTarget filterTarget;
    private final Date triggerTime;
    private final long projectId;

    public CreateMailTaskSendMailProcessor(long projectId, MailFilterTarget filterTarget, Date triggerTime) {
        this.projectId = projectId;
        this.filterTarget = filterTarget;
        this.triggerTime = triggerTime;
    }

    @Override
    public void processSendMail(RequestCycle cycle, SendMailSession sms, SendMailTemplate smt) {
        PortableMailTemplate pmt = createPortableMailTemplate(cycle, smt);

        NewProjectTaskRequest newTask = NewProjectTaskRequest.newPortableMailTask(
                LoginUserAccountHelper.getUserId(cycle),
                projectId,
                "email",
                triggerTime,
                PortableMailTemplateCodec.encode(pmt, true),
                filterTarget.getId());

        getCocoboxCordinatorClient(cycle).scheduleProjectTask(newTask);
    }

    @Override
    public MailSender getMailSender(RequestCycle cycle) {
        return null;
    }

    private CocoboxCordinatorClient getCocoboxCordinatorClient(RequestCycle cycle) {
        return CacheClients.getClient(cycle, CocoboxCordinatorClient.class);
    }

}
