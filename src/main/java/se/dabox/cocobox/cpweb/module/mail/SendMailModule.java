/*
 * (c) Dabox AB 2012 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.module.mail;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.unixdeveloper.druwa.DruwaInternalException;
import net.unixdeveloper.druwa.HttpMethod;
import net.unixdeveloper.druwa.RequestCycle;
import net.unixdeveloper.druwa.RequestTarget;
import net.unixdeveloper.druwa.annotation.WebAction;
import net.unixdeveloper.druwa.annotation.mount.WebModuleMountpoint;
import net.unixdeveloper.druwa.formbean.DruwaFormValidationSession;
import net.unixdeveloper.druwa.freemarker.FreemarkerRequestTarget;
import net.unixdeveloper.druwa.request.JsonRequestTarget;
import net.unixdeveloper.druwa.request.WebModuleRedirectRequestTarget;
import org.apache.commons.collections4.map.Flat3Map;
import org.apache.commons.lang3.StringUtils;
import se.dabox.cocobox.cpweb.NavigationUtil;
import se.dabox.cocobox.cpweb.formdata.SendEmailForm;
import se.dabox.cocobox.cpweb.module.core.AbstractWebAuthModule;
import se.dabox.cocobox.cpweb.state.SendMailSession;
import se.dabox.cocobox.cpweb.state.SendMailTemplate;
import se.dabox.cocosite.mail.GetGenericMailBucketCommand;
import se.dabox.cocosite.mail.GetOrgMailBucketCommand;
import se.dabox.cocosite.modal.ModalParamsHelper;
import se.dabox.cocosite.org.MiniOrgInfo;
import se.dabox.service.login.client.UserAccount;
import se.dabox.service.login.client.UserAccountService;
import se.dabox.service.client.Clients;
import se.dabox.service.common.ajaxlongrun.AjaxLongOp;
import se.dabox.service.common.json.JsonUtils;
import se.dabox.service.common.mailsender.mailtemplate.GetHintedMailTemplateCommand;
import se.dabox.service.common.mailsender.mailtemplate.MailTemplate;
import se.dabox.service.common.mailsender.mailtemplate.MailTemplateServiceClient;
import se.dabox.service.common.mailsender.mailtemplate.MissingStickyMailTemplateException;
import se.dabox.service.common.mailsender.pmt.PortableMailTemplate;
import se.dabox.service.webutils.json.JsonExceptionHandler;
import se.dabox.util.ParamUtil;
import se.dabox.util.collections.CollectionsUtil;

/**
 *
 * @author Jerker Klang (jerker.klang@dabox.se)
 */
@WebModuleMountpoint("/mail")
public class SendMailModule extends AbstractWebAuthModule {

    public static final String VIEW_SENDMAIL_ACTION = "sendEmail";
    public static final String EXECUTE_SENDMAIL_ACTION = "deliverEmail";

    @WebAction
    public RequestTarget onSendEmail(RequestCycle cycle, String strOrgId,
            String strSessionId) {

        MiniOrgInfo org = secureGetMiniOrg(cycle, strOrgId);

        SendMailSession sms = SendMailSession.getFromSession(cycle.getSession(), strSessionId);

        if (sms == null) {
            return NavigationUtil.toOrgMain(strOrgId);
        }

        long mailBucket = new GetOrgMailBucketCommand(cycle).forOrg(Long.
                parseLong(strOrgId));

        String subject = null;
        String body = null;

        Map<String, Object> map = createMap();

        if (sms.isDropdownEnabled()) {
            TemplateLists lists = getLists(cycle, mailBucket);
            map.put("templateLists", lists);
        }

        if (sms.getStickyTemplateHint() != null) {
            MailTemplate stickyTemplate = getStickyTemplate(cycle, mailBucket,
                    sms.getStickyTemplateHint(),
                    sms.getStickyTemplateLocale());

            if (stickyTemplate == null) {
                String msg = String.format("Sticky template %s missing in bucket %d", sms.
                        getStickyTemplateHint(), mailBucket);
                throw new MissingStickyMailTemplateException(msg);
            }

            map.put("stickyTemplateId", stickyTemplate.getId());
        }

        if (sms.getPortableMailTemplate() != null) {
            PortableMailTemplate pmt = sms.getPortableMailTemplate();
            subject = pmt.getSubject().getContent();
            body = pmt.getParts().get(0).getContent();
        }

        DruwaFormValidationSession<SendEmailForm> formsess
                = getValidationSession(SendEmailForm.class, cycle);
        map.put("formsess", formsess);
        map.put("org", org);

        map.put("formLink",
                cycle.urlFor(SendMailModule.class.getName(), EXECUTE_SENDMAIL_ACTION,
                strOrgId, strSessionId));
        map.put("jobUrl", cycle.urlFor(SendMailModule.class, "processJob", strOrgId, strSessionId));
        map.put("completedUrl", cycle.urlFor(SendMailModule.class, "finishJob", strOrgId,
                strSessionId));
        map.put("sms", sms);
        map.put("showCancel", sms.getCancelTargetGenerator() != null);
        map.put("receivers", getReceivers(cycle, sms));
        map.put("displayReceivers", sms.getDisplayReceivers());
        map.put("sender", sms.getProcessor().getMailSender(cycle));
        map.put("modalMode", ModalParamsHelper.isModalRequest(cycle));
        
        addOldValueIfMissing(formsess, "body", body);
        addOldValueIfMissing(formsess, "subject", subject);

        return new FreemarkerRequestTarget("/sendEmail.html", map);
    }

    @WebAction()
    public RequestTarget onCancel(RequestCycle cycle, String strOrgId, String strSessionId) {
        checkOrgPermission(cycle, strOrgId);

        SendMailSession sms = SendMailSession.getFromSession(cycle.getSession(), strSessionId);

        if (sms == null || sms.getCancelTargetGenerator() == null) {
            return NavigationUtil.toOrgMain(strOrgId);
        }

        return sms.getCancelTargetGenerator().generateTarget(cycle);
    }

    @WebAction(methods =  HttpMethod.POST)
    public RequestTarget onProcessJob(RequestCycle cycle, String strOrgId, String strSessionId) {
        checkOrgPermission(cycle, strOrgId);

        SendMailSession sms = SendMailSession.getFromSession(cycle.getSession(), strSessionId);

        if (sms == null) {
            return jsonAnswer("status", "selfpage");
        }

        AjaxSendMailProcessor ajaxProcessor = (AjaxSendMailProcessor) sms.getProcessor();

        SendMailTemplate smt = null;
        if (!AjaxLongOp.isCommmandCall(cycle)) {
            DruwaFormValidationSession<SendEmailForm> formsess = getValidationSession(
                    SendEmailForm.class, cycle);

            if (!formsess.process()) {
                return jsonAnswer("status", "params");
            }

            SendEmailForm form = formsess.getObject();

            smt = new SendMailTemplate(form.getSubject(), form.
                    getBody(), form.getMtype());

            if (StringUtils.isEmpty(form.getBody())) {
                return jsonAnswer("status", "missingbody");
            }

            if (!sms.verifySendMail(cycle, smt)) {
                formsess.transferToViewSession();

                return jsonAnswer("status", "params");
            }
        }

        try {
            return ajaxProcessor.processAjaxRequest(cycle, sms, smt);
        } catch (DruwaInternalException ex) {
            throw ex;
        } catch(Exception ex) {
            return JsonExceptionHandler.exceptionHandler(cycle, null, ex);
        }
    }

    @WebAction
    public RequestTarget finishJob(RequestCycle cycle, String strOrgId, String strSessionId) {
        checkOrgPermission(cycle, strOrgId);

        SendMailSession sms = SendMailSession.getFromSession(cycle.getSession(), strSessionId);

        if (sms == null) {
            return NavigationUtil.toOrgMain(strOrgId);
        }

        sms.removeFromSession(cycle.getSession());

        RequestTarget target = sms.getCompletedRequestTarget(cycle);

        if (target == null) {
            throw new IllegalStateException("No completedRequestTarget from SendMailSession");
        }

        return target;
    }

    @WebAction(methods = HttpMethod.POST)
    public RequestTarget onDeliverEmail(RequestCycle cycle, String strOrgId, String strSessionId) {
        checkOrgPermission(cycle, strOrgId);

        SendMailSession sms = SendMailSession.getFromSession(cycle.getSession(), strSessionId);

        if (sms == null) {
            return NavigationUtil.toOrgMain(strOrgId);
        }

        DruwaFormValidationSession<SendEmailForm> formsess =
                getValidationSession(SendEmailForm.class, cycle);

        if (!formsess.process()) {
            return new WebModuleRedirectRequestTarget(SendMailModule.class,
                    VIEW_SENDMAIL_ACTION, strOrgId, strSessionId);
        }

        SendEmailForm form = formsess.getObject();

        SendMailTemplate smt = new SendMailTemplate(form.getSubject(), form.
                getBody(), form.getMtype());

        if (StringUtils.isEmpty(form.getBody())) {
            return new WebModuleRedirectRequestTarget(SendMailModule.class,
                    VIEW_SENDMAIL_ACTION, strOrgId, strSessionId);
        }

        if (!sms.verifySendMail(cycle, smt)) {
            formsess.transferToViewSession();

            return new WebModuleRedirectRequestTarget(SendMailModule.class,
                    VIEW_SENDMAIL_ACTION, strOrgId, strSessionId);
        }

        sms.processSendMail(cycle, smt);

        sms.removeFromSession(cycle.getSession());

        RequestTarget target = sms.getCompletedRequestTarget(cycle);
        
        if (target == null) {
            throw new IllegalStateException("No completedRequestTarget from SendMailSession");
        }

        return target;
    }

    private TemplateLists getLists(RequestCycle cycle, long mailBucket) {
        MailTemplateServiceClient mtClient = getMailTemplateClient(cycle);

        List<MailTemplate> templates =
                mtClient.getBucketMailTemplates(mailBucket);

        return new TemplateLists(cycle.getResponse().getLocale()).
                addTemplates(templates);
    }

    private MailTemplate getStickyTemplate(RequestCycle cycle, long mailBucket,
            String stickyTemplateHint, Locale locale) {
        ParamUtil.required(stickyTemplateHint, "stickyTemplateHint");

        MailTemplateServiceClient mtClient = getMailTemplateClient(cycle);
        long parentBucket = new GetGenericMailBucketCommand(cycle).getId();

        return GetHintedMailTemplateCommand.getHintedTemplate(mtClient, stickyTemplateHint,
                mailBucket,
                parentBucket,
                locale);
    }

    private List<UserAccount> getReceivers(RequestCycle cycle, SendMailSession sms) {
        final UserAccountService uaClient = Clients.getClient(cycle, UserAccountService.class);

        return CollectionsUtil.transformListNotNull(sms.getReceivers(), uaClient::getUserAccount);
    }

    private void addOldValueIfMissing(DruwaFormValidationSession<SendEmailForm> formsess,
            String name, String value) {
        if (value == null) {
            return;
        }

        if (formsess.getDefaultValue(name) == null) {
            formsess.putDefaultValue(name, value);
        }
    }

    private RequestTarget jsonAnswer(String name, String value) {
        Map<String,String> map = new Flat3Map<>();
        map.put(name, value);

        return new JsonRequestTarget(JsonUtils.encode(map));
    }
}
