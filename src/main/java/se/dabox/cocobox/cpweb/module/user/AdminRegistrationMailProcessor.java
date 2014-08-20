/*
 * (c) Dabox AB 2012 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.module.user;

import java.util.Map;
import net.unixdeveloper.druwa.RequestCycle;
import se.dabox.cocobox.cpweb.module.core.AbstractAuthModule;
import se.dabox.cocobox.cpweb.module.mail.MailSender;
import se.dabox.cocobox.cpweb.module.mail.SendMailProcessor;
import se.dabox.cocobox.cpweb.state.SendMailSession;
import se.dabox.cocobox.cpweb.state.SendMailTemplate;
import se.dabox.service.client.CacheClients;
import se.dabox.service.login.client.UserAccount;
import se.dabox.service.login.client.UserAccountService;
import se.dabox.service.client.Clients;
import se.dabox.service.common.ccbc.mail.SendMailRequestFactory;
import se.dabox.service.common.context.Configuration;
import se.dabox.service.common.context.DwsRealmHelper;
import se.dabox.service.common.mailsender.MailSendServiceClient;
import se.dabox.service.common.mailsender.SendMailRecipient;
import se.dabox.service.common.mailsender.SendMailRequest;
import se.dabox.service.orgadmin.client.ActivateCpAdminTokenGenerator;
import se.dabox.service.orgdir.client.OrgUnitInfo;
import se.dabox.service.orgdir.client.OrganizationDirectoryClient;
import se.dabox.service.randdata.client.RandomDataClient;
import se.dabox.service.webutils.login.LoginUserAccountHelper;

/**
 *
 * @author Jerker Klang (jerker.klang@dabox.se)
 */
public class AdminRegistrationMailProcessor implements SendMailProcessor {
    private static final long serialVersionUID = 1L;

    private final long userId;
    private final long orgUnitId;

    public AdminRegistrationMailProcessor(long userId, long orgUnitId) {
        this.userId = userId;
        this.orgUnitId = orgUnitId;
    }

    @Override
    public void processSendMail(RequestCycle cycle, SendMailSession sms, SendMailTemplate smt) {
        
        UserAccount account = getUserAccount(cycle, userId);
        OrgUnitInfo org = getOrgUnit(cycle, orgUnitId);

        String token = createToken(cycle, account, org);

        Configuration config = DwsRealmHelper.getRealmConfiguration(cycle);

        Map<String, String> vars =
                new AdminRegistrationMailVariables().produceFor(config, account, org);

        SendMailRequest req =
                getRequest(cycle);
        smt.toSendMailRequest(req);

        SendMailRecipient recipient = new SendMailRecipient(account.getDisplayName(), account.
                getPrimaryEmail());

        recipient.addVariables(vars);
        recipient.addVariable("registrationid", token);

        recipient.addVariable("link", vars.get("registrationlink") + token);

        req.addRecipient(recipient);

        CacheClients.getClient(cycle, MailSendServiceClient.class).sendMail(req);
    }

    private UserAccount getUserAccount(RequestCycle cycle, long userId) {
        return Clients.getClient(cycle, UserAccountService.class).
                getUserAccount(userId);
    }

    private OrgUnitInfo getOrgUnit(RequestCycle cycle, long orgUnitId) {
        return Clients.getClient(cycle, OrganizationDirectoryClient.class).
                getOrgUnitInfo(orgUnitId);
    }

    private String createToken(RequestCycle cycle, UserAccount account, OrgUnitInfo org) {
        String json = ActivateCpAdminTokenGenerator.
                createTokenData(org.getId(), account.getUserId());

        RandomDataClient rdc = AbstractAuthModule.getRandomDataClient(cycle);

        String token = rdc.addRandomData(LoginUserAccountHelper.getUserId(cycle), json);

        return token;
    }

    private SendMailRequest getRequest(RequestCycle cycle) {
        SendMailRequest req =
                SendMailRequestFactory.newRequest(LoginUserAccountHelper.getUserId(cycle));
        return req;
    }

    @Override
    public MailSender getMailSender(RequestCycle cycle) {
        SendMailRequest req = getRequest(cycle);

        return new MailSender(req.getFromName(), req.getFromEmail());
    }

}
