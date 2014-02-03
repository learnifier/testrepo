/*
 * (c) Dabox AB 2012 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.module.project;

import java.util.List;
import net.unixdeveloper.druwa.RequestCycle;
import se.dabox.cocobox.cpweb.CpwebConstants;
import se.dabox.cocobox.cpweb.module.core.AbstractModule;
import se.dabox.cocobox.cpweb.module.mail.MailSender;
import se.dabox.cocobox.cpweb.module.mail.SendMailProcessor;
import se.dabox.cocobox.cpweb.module.project.credit.CpCreditCheck;
import se.dabox.cocobox.cpweb.module.project.credit.CreditAllocationFailure;
import se.dabox.cocobox.cpweb.state.SendMailSession;
import se.dabox.cocobox.cpweb.state.SendMailTemplate;
import se.dabox.cocobox.cpweb.state.SendMailVerifier;
import se.dabox.service.client.CacheClients;
import se.dabox.service.common.ccbc.CocoboxCordinatorClient;
import se.dabox.service.common.ccbc.ProjectParticipantMailRequest;
import se.dabox.service.orgdir.client.OrgUnitInfo;
import se.dabox.service.orgdir.client.OrganizationDirectoryClient;
import se.dabox.service.webutils.login.LoginUserAccountHelper;
import se.dabox.util.collections.CollectionsUtil;
import se.dabox.util.collections.Transformer;

/**
 *
 * @author Jerker Klang <jerker.klang@dabox.se>
 */
public class ProjectSendMailProcessor implements SendMailProcessor, SendMailVerifier {
    private static final long serialVersionUID = 2L;
    private final long projectId;
    private final long orgId;

    public ProjectSendMailProcessor(long projectId, long orgId) {
        this.projectId = projectId;
        this.orgId = orgId;
    }
    
    @Override
    public void processSendMail(RequestCycle cycle, SendMailSession session,
            SendMailTemplate template) {
        CocoboxCordinatorClient ccbc =
                AbstractModule.getCocoboxCordinatorClient(cycle);

        long userId = LoginUserAccountHelper.getUserId(cycle);

        ProjectParticipantMailRequest req =
                new ProjectParticipantMailRequest(userId, projectId, "dws/mail-template", template.
                getSubject(), template.getBody());

        for (Object object : session.getExtraDatas()) {
            req.addParticipationId((Long)object);
        }

        ccbc.sendProjectParticipantMail(req);
    }

    @Override
    public MailSender getMailSender(RequestCycle cycle) {
        return null;
    }

    @Override
    public boolean verifySendMail(RequestCycle cycle, SendMailSession session,
            SendMailTemplate mailTemplate) {

        List<Long> participations = CollectionsUtil.transformList(session.getExtraDatas(), new Transformer<Object, Long>() {

                    @Override
                    public Long transform(Object item) {
                        return (Long) item;
                    }
                });

        OrgUnitInfo org =
                CacheClients.getClient(cycle, OrganizationDirectoryClient.class).getOrgUnitInfo(
                orgId);

        CpCreditCheck creditChecker = new CpCreditCheck(cycle, org, projectId);

        List<CreditAllocationFailure> failures = creditChecker.getCreditFailures(participations);

        if (failures.isEmpty()) {
            return true;
        }

        cycle.getSession().setFlashAttribute(CpwebConstants.CREDIT_ALLOC_FLASH, failures);
        return false;
    }

}
