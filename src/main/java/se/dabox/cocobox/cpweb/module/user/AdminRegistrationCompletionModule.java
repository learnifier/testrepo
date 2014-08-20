/*
 * (c) Dabox AB 2012 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.module.user;

import java.util.Locale;
import java.util.Map;
import net.unixdeveloper.druwa.RequestCycle;
import net.unixdeveloper.druwa.RequestTarget;
import net.unixdeveloper.druwa.annotation.WebAction;
import net.unixdeveloper.druwa.annotation.mount.WebModuleMountpoint;
import net.unixdeveloper.druwa.freemarker.FreemarkerRequestTarget;
import net.unixdeveloper.druwa.request.ErrorCodeRequestTarget;
import se.dabox.cocobox.cpweb.CpwebConstants;
import se.dabox.cocobox.cpweb.NavigationUtil;
import se.dabox.cocobox.cpweb.module.core.AbstractWebAuthModule;
import se.dabox.cocobox.cpweb.state.SendMailSession;
import se.dabox.cocobox.cpweb.state.SendMailTemplate;
import se.dabox.cocosite.mail.GetGenericMailBucketCommand;
import se.dabox.cocosite.mail.GetOrgMailBucketCommand;
import se.dabox.dws.client.JacksonHelper;
import se.dabox.service.client.CacheClients;
import se.dabox.service.common.json.JsonUtils;
import se.dabox.service.common.mailsender.mailtemplate.GetHintedMailTemplateCommand;
import se.dabox.service.common.mailsender.mailtemplate.MailTemplate;
import se.dabox.service.common.mailsender.mailtemplate.MailTemplateServiceClient;
import se.dabox.service.login.client.CocoboxUserAccount;
import se.dabox.service.login.client.UserAccount;
import se.dabox.service.login.client.UserAccountService;

/**
 * Module that completes the admin registration. This logic is in a separate module
 * from the first because this requires the user to be logged in
 *
 * @author Jerker Klang (jerker.klang@dabox.se)
 */
@WebModuleMountpoint("/registration2")
public class AdminRegistrationCompletionModule extends AbstractWebAuthModule {
    public static final String ACTION = "complete";

    @WebAction
    public RequestTarget onComplete(RequestCycle cycle, String id) {
        @SuppressWarnings("unchecked")
        Map<String,Object> map = (Map<String,Object>) getStoredMap(cycle, id);

        if (map == null) {
            return new ErrorCodeRequestTarget(404, "id not found");
        }

        long userId = JacksonHelper.getLong(map, "userId");

        if (userId != getCurrentUser(cycle)) {
            return new ErrorCodeRequestTarget(404, "id not found");
        }

        Boolean consumed = (Boolean) map.get("consumed");

        if (consumed != null && consumed) {
            return new FreemarkerRequestTarget("/registration/registrationAlreadyDone.html", null);
        }

        long orgId = JacksonHelper.getLong(map, "orgId");
        sendWelcomeMail(cycle, userId, orgId);

        map.put("consumed", Boolean.TRUE);
        getRandomDataClient(cycle).updateRandomData(userId, id, JsonUtils.encode(map));

        return NavigationUtil.toOrgMain(Long.toString(orgId));
    }

    private Map<String, ?> getStoredMap(RequestCycle cycle, String id) {
        if (id == null) {
            return null;
        }

        String json = getRandomDataClient(cycle).getRandomData(id);

        if (json == null) {
            return null;
        }

        return JsonUtils.decode(json);
    }

    private void sendWelcomeMail(RequestCycle cycle, long userId, long orgId) {
        AdminWelcomeMailProcessor awmp = new AdminWelcomeMailProcessor(userId, orgId);
        SendMailSession session = SendMailSession.createSimple(awmp);
        session.addReceiver(userId);

        SendMailTemplate template = getWelcomeMailTemplate(cycle, orgId, userId);

        awmp.processSendMail(cycle, session, template);
    }

    private SendMailTemplate getWelcomeMailTemplate(RequestCycle cycle, long orgId, long userId) {

        MailTemplateServiceClient mtClient = getMailTemplateClient(cycle);
        long parentBucket = new GetGenericMailBucketCommand(cycle).getId();
        long bucket = new GetOrgMailBucketCommand(cycle).forOrg(orgId);

        UserAccountService uaClient = CacheClients.getClient(cycle, UserAccountService.class);

        UserAccount user = uaClient.getUserAccount(userId);
        CocoboxUserAccount acc = new CocoboxUserAccount(user);
        Locale locale = acc.getLocale();

        MailTemplate template =
                GetHintedMailTemplateCommand.getHintedTemplate(mtClient,
                CpwebConstants.ADMIN_WELCOME_MAIL_HINT, bucket, parentBucket, locale);

        if (template == null) {
            throw new IllegalStateException("Failed to locate welcome mail template for org: "+orgId);
        }

        return new SendMailTemplate(template.getSubject(), template.getMainContent(), template.
                getType());
    }
}
