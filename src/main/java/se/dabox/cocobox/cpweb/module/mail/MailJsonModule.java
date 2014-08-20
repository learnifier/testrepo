/*
 * (c) Dabox AB 2012 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.module.mail;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import net.unixdeveloper.druwa.HttpMethod;
import net.unixdeveloper.druwa.RequestCycle;
import net.unixdeveloper.druwa.RequestTarget;
import net.unixdeveloper.druwa.annotation.WebAction;
import net.unixdeveloper.druwa.annotation.mount.WebModuleMountpoint;
import org.codehaus.jackson.JsonGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.dabox.cocobox.cpweb.NavigationUtil;
import se.dabox.cocobox.cpweb.module.core.AbstractJsonAuthModule;
import se.dabox.cocosite.druwa.DruwaParamHelper;
import se.dabox.cocosite.login.CocositeUserHelper;
import se.dabox.cocosite.mail.GetOrgMailBucketCommand;
import se.dabox.cocosite.org.MiniOrgInfo;
import se.dabox.cocosite.security.CocoboxPermissions;
import se.dabox.service.common.mailsender.mailtemplate.MailTemplate;
import se.dabox.service.webutils.json.JsonEncoding;

/**
 *
 * @author Jerker Klang (jerker.klang@dabox.se)
 */
@WebModuleMountpoint("/mail.json")
public class MailJsonModule extends AbstractJsonAuthModule {
    public static final String GET_TEMPLATE_ACTION = "getTemplate";

    private static final Logger LOGGER =
            LoggerFactory.getLogger(MailJsonModule.class);

    @WebAction(methods=HttpMethod.POST)
    public RequestTarget onGetTemplate(RequestCycle cycle) {
        long templateId = DruwaParamHelper.getMandatoryLongParam(LOGGER, cycle.getRequest(),
                "templateId");
        
        final MailTemplate template =
                getMailTemplateClient(cycle).getMailTemplate(templateId);

        byte[] data = new JsonEncoding() {

            @Override
            protected void encodeData(JsonGenerator generator) throws IOException {
                generator.writeStartObject();

                generator.writeStringField("body", template.getMainContent());
                generator.writeStringField("subject", template.getSubject());
                generator.writeStringField("mtype", template.getType());
                generator.writeNumberField("id", template.getId());

                generator.writeEndObject();
            }
        }.encode();

        return jsonTarget(data);
    }

    @WebAction
    public RequestTarget onListTemplates(RequestCycle cycle, String strOrgId) {
        MiniOrgInfo org = secureGetMiniOrg(cycle, strOrgId);
        checkOrgPermission(cycle, org.getId(), CocoboxPermissions.CP_LIST_EMAILS);

        long bucketId = new GetOrgMailBucketCommand(cycle).forOrg(org.getId());

        final List<MailTemplate> template =
                getMailTemplateClient(cycle).getBucketMailTemplates(bucketId);

        return jsonTarget(toMailTemplateInfoJson(cycle, strOrgId, template));
    }

    private byte[] toMailTemplateInfoJson(final RequestCycle cycle, final String strOrgId, final List<MailTemplate> templateList) {

        final Locale userLocale = CocositeUserHelper.getUserLocale(cycle);

        return new JsonEncoding() {

            @Override
            protected void encodeData(JsonGenerator generator) throws IOException {
                generator.writeStartObject();

                generator.writeArrayFieldStart("aaData");

                for (MailTemplate template : templateList) {
                    generator.writeStartObject();

                    generator.writeStringField("mtype", template.getType());
                    generator.writeNumberField("id", template.getId());
                    generator.writeStringField("name", template.getName());
                    generator.writeStringField("description", template.getDescription());
                    generator.writeBooleanField("sticky", template.getSticky());
                    generator.writeBooleanField("enabled", template.isEnabled());
                    generator.writeStringField("editlink", NavigationUtil.toEmailPageUrl(cycle,
                      strOrgId, template.getId()));

                    generator.writeStringField("locale", template.getLocale().toString());
                    generator.writeStringField("localeStr",
                            template.getLocale().getDisplayName(userLocale));
                    generator.writeStringField("softid", template.getSoftId());


                    generator.writeEndObject();
                }

                generator.writeEndArray();

                generator.writeEndObject();
            }
            
        }.encode();
    }
}
