/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package se.dabox.cocobox.cpweb.module.mail;

import java.util.Date;
import java.util.Locale;
import java.util.Map;

import net.unixdeveloper.druwa.RequestCycle;
import net.unixdeveloper.druwa.RequestTarget;
import net.unixdeveloper.druwa.annotation.DefaultWebAction;
import net.unixdeveloper.druwa.annotation.WebAction;
import net.unixdeveloper.druwa.annotation.mount.WebModuleMountpoint;
import net.unixdeveloper.druwa.formbean.DruwaFormValidationSession;
import net.unixdeveloper.druwa.freemarker.FreemarkerRequestTarget;
import net.unixdeveloper.druwa.request.RedirectUrlRequestTarget;
import net.unixdeveloper.druwa.request.WebModuleRedirectRequestTarget;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.dabox.cocobox.cpweb.CpwebConstants;
import se.dabox.cocobox.cpweb.NavigationUtil;
import se.dabox.cocobox.cpweb.formdata.mail.EmailSettingForm;
import se.dabox.cocobox.cpweb.module.core.AbstractWebAuthModule;
import se.dabox.cocobox.cpweb.module.util.CpwebParameterUtil;
import se.dabox.cocobox.maileditor.initdata.MeInitData;
import se.dabox.cocosite.branding.GetOrgBrandingIdCommand;
import se.dabox.cocosite.druwa.DruwaParamHelper;
import se.dabox.cocosite.email.EmailLocaleListFactory;
import se.dabox.cocosite.login.CocositeUserHelper;
import se.dabox.cocosite.mail.GetOrgMailBucketCommand;
import se.dabox.cocosite.org.MiniOrgInfo;
import se.dabox.cocobox.security.permission.CocoboxPermissions;
import se.dabox.cocosite.user.UserIdentifierHelper;
import se.dabox.service.common.mailsender.mailtemplate.CreateMailTemplateRequest;
import se.dabox.service.common.mailsender.mailtemplate.MailTemplate;
import se.dabox.service.common.mailsender.mailtemplate.MailTemplateServiceClient;
import se.dabox.service.webutils.login.LoginUserAccountHelper;

/**
 *
 * @author borg321
 */
@WebModuleMountpoint("/email")
public class MailModule extends AbstractWebAuthModule {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(MailModule.class);
    public static final String OVERVIEW_ACTION = "overview";

    @DefaultWebAction
    @WebAction
    public RequestTarget onOverview(RequestCycle cycle, String strOrgId, String strTemplateId) {
        MiniOrgInfo org = secureGetMiniOrg(cycle, strOrgId);
        checkOrgPermission(cycle, org.getId(), CocoboxPermissions.CP_VIEW_EMAIL);

        Long templateId = CpwebParameterUtil.stringToLong(strTemplateId);

        MailTemplate template = null;

        if (templateId != null) {
            template = getOrgMailTemplate(cycle, templateId, org.getId());
        }

        if (template == null) {
            LOGGER.warn("Mail template not found: {}({})", strTemplateId, templateId);
            String url = NavigationUtil.toEmailListPageUrl(cycle, strOrgId);
            return new RedirectUrlRequestTarget(url);
        }

        Map<String, Object> map = createMap();

        map.put("org", org);
        map.put("template", template);
        map.put("usernameHelper", new UserIdentifierHelper(cycle));

        return new FreemarkerRequestTarget("/email/emailOverview.html", map);
    }

    @WebAction
    public RequestTarget onEdit(RequestCycle cycle, String strOrgId, String templateId,
            String strCopy) {

        MiniOrgInfo org = secureGetMiniOrg(cycle, strOrgId);

        boolean copyMode = !StringUtils.isEmpty(strCopy);

        if (copyMode) {
            checkOrgPermission(cycle, strOrgId, CocoboxPermissions.CP_COPY_EMAIL);
        }  else {
            checkOrgPermission(cycle, strOrgId, CocoboxPermissions.CP_EDIT_EMAIL);
        }

        final MailTemplate template = getOrgMailTemplate(cycle, Long.valueOf(templateId), org.
                getId());

        if (template == null) {
            LOGGER.warn("Mail template not found: {}", templateId);
            String url = NavigationUtil.toEmailListPageUrl(cycle, strOrgId);
            return new RedirectUrlRequestTarget(url);
        }


        EmailSettingForm form = templateToEmailSettingsForm(template);

        DruwaFormValidationSession<EmailSettingForm> formsess =
                getValidationSession(EmailSettingForm.class, cycle);
        formsess.populateFromObject(form);

        Map<String, Object> map = createMap();

        map.put("formsess", formsess);
        map.put("org", org);
        map.put("template", template);
        map.put("copyMode", copyMode);

        Locale userLocale = CocositeUserHelper.getUserLocale(cycle);
        map.put("emailLocales", new EmailLocaleListFactory().getLocales(cycle, userLocale));

        String formLink;

        if (copyMode) {
            formLink = cycle.urlFor(MailModule.class, "editSave", strOrgId, templateId, "t");
        } else {
            formLink = cycle.urlFor(MailModule.class, "editSave", strOrgId, templateId);
        }

        map.put("formLink", formLink);

        return new FreemarkerRequestTarget("/email/editEmailSettings.html", map);
    }

    @WebAction
    public RequestTarget onDelete(RequestCycle cycle, String strOrgId, String templateId) {

        MiniOrgInfo org = secureGetMiniOrg(cycle, strOrgId);

        checkOrgPermission(cycle, org.getId(), CocoboxPermissions.CP_DELETE_EMAIL);

        MailTemplate template = getOrgMailTemplate(cycle, Long.valueOf(templateId), org.getId());

        if (template == null || template.isStickyCheck()) {
            return toListPage(cycle, strOrgId);
        }

        long bucketId = new GetOrgMailBucketCommand(cycle).forOrg(org.getId());
        final MailTemplateServiceClient mtClient = getMailTemplateClient(cycle);

        long userId = LoginUserAccountHelper.getUserId(cycle);

        mtClient.removeBucketMailTemplate(bucketId, template.getId(), userId);

        mtClient.removeMailTemplate(userId, template.getId());
        return toListPage(cycle, strOrgId);
    }

    @WebAction
    public RequestTarget onEditSave(RequestCycle cycle, String strOrgId, String templateId,
            String strCopy) {
        MiniOrgInfo org = secureGetMiniOrg(cycle, strOrgId);

        boolean copyMode = !StringUtils.isEmpty(strCopy);

        if (copyMode) {
            checkOrgPermission(cycle, strOrgId, CocoboxPermissions.CP_COPY_EMAIL);
        } else {
            checkOrgPermission(cycle, strOrgId, CocoboxPermissions.CP_EDIT_EMAIL);
        }

        final MailTemplate template = getOrgMailTemplate(cycle, Long.valueOf(templateId), org.
                getId());

        DruwaFormValidationSession<EmailSettingForm> formsess =
                getValidationSession(EmailSettingForm.class, cycle);

        if (!formsess.process()) {
            if (copyMode) {
                return new WebModuleRedirectRequestTarget(MailModule.class, "edit", strOrgId,
                        templateId, strCopy);
            } else {
                return new WebModuleRedirectRequestTarget(MailModule.class, "edit", strOrgId,
                        templateId);
            }
        }

        if (copyMode) {
            return copyTemplate(cycle, template, formsess, strOrgId);
        } else {
            return saveTemplate(cycle, template, formsess, strOrgId);
        }
    }

    @WebAction
    public RequestTarget onEditor(RequestCycle cycle, String strOrgId, String templateId) {
        MiniOrgInfo org = secureGetMiniOrg(cycle, strOrgId);

        final MailTemplate template = getOrgMailTemplate(cycle, Long.valueOf(templateId), org.
                getId());

        if (template.getSticky()) {
            return toListPage(cycle, strOrgId);
        }

        long brandingId = new GetOrgBrandingIdCommand(cycle).forOrg(org.getId());

        String backUrl = NavigationUtil.toEmailPageUrl(cycle, strOrgId, template.getId());
        String cancelUrl = backUrl;

        MeInitData initData =
                new MeInitData(org.getId(), 
                CpwebConstants.MAILEDITOR_SKIN,
                brandingId,
                template.getName(),
                template.getId(),
                backUrl,
                cancelUrl);

        return new RedirectUrlRequestTarget(GotoMailEditor.process(cycle, initData));
    }

    private EmailSettingForm templateToEmailSettingsForm(MailTemplate template) {
        EmailSettingForm form = new EmailSettingForm();

        form.setDescription(template.getDescription());
        form.setLang(template.getLocale());
        form.setName(template.getName());

        return form;
    }

    private MailTemplate getOrgMailTemplate(RequestCycle cycle, long templateId, long orgId) {

        long bucketId = new GetOrgMailBucketCommand(cycle).forOrg(orgId);

        return getMailTemplateClient(cycle).getBucketMailTemplate(bucketId, templateId);
    }

    private RequestTarget copyTemplate(RequestCycle cycle, MailTemplate template,
            DruwaFormValidationSession<EmailSettingForm> formsess, String strOrgId) {

        EmailSettingForm form = formsess.getObject();

        long userId = LoginUserAccountHelper.getUserId(cycle);

        CreateMailTemplateRequest createReq = CreateMailTemplateRequest.newStandardMail(form.getName(),
                form.getDescription(),
                form.getLang(), true,
                template.getMainContent(),
                template.getSubject(),
                template.getType(),
                false,
                userId,
                false);

        long bucketId = new GetOrgMailBucketCommand(cycle).forOrg(Long.valueOf(strOrgId));

        MailTemplateServiceClient mtClient = getMailTemplateClient(cycle);

        long newMailTemplateId = mtClient.createMailTemplate(createReq);

        mtClient.addBucketMailTemplate(bucketId, newMailTemplateId, false, userId);

        String url = NavigationUtil.toEmailPageUrl(cycle, strOrgId, newMailTemplateId);
        return new RedirectUrlRequestTarget(url);
    }

    private RequestTarget saveTemplate(RequestCycle cycle, MailTemplate template,
            DruwaFormValidationSession<EmailSettingForm> formsess, String strOrgId) {

        if (template.getSticky() != null && template.getSticky()) {
            toListPage(cycle, strOrgId);
        }

        EmailSettingForm form = formsess.getObject();

        template.setName(form.getName());
        template.setDescription(form.getDescription());
        template.setLocale(form.getLang());

        template.setUpdated(new Date());
        template.setUpdatedBy(LoginUserAccountHelper.getUserId(cycle));

        getMailTemplateClient(cycle).updateMailTemplate(template);

        String url = NavigationUtil.toEmailPageUrl(cycle, strOrgId, Long.
                valueOf(template.getId()));
        return new RedirectUrlRequestTarget(url);
    }

    private RequestTarget toListPage(RequestCycle cycle, String strOrgId) {
        return new RedirectUrlRequestTarget(NavigationUtil.toEmailListPageUrl(cycle, strOrgId));
    }
}
