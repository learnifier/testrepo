/*
 * (c) Dabox AB 2012 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.freemarker;

import freemarker.core.Environment;
import freemarker.template.TemplateDirectiveBody;
import freemarker.template.TemplateDirectiveModel;
import freemarker.template.TemplateException;
import freemarker.template.TemplateModel;
import java.io.IOException;
import java.util.Map;
import net.unixdeveloper.druwa.DruwaApplication;
import net.unixdeveloper.druwa.RequestCycle;
import se.dabox.cocobox.cpweb.command.GetOrgBrandingCommand;
import se.dabox.cocosite.branding.freemarker.AbstractOrgBrandingOutput;
import se.dabox.cocosite.branding.freemarker.BrandingOutputUtil;
import se.dabox.cocosite.branding.freemarker.RealmBrandingOutput;
import se.dabox.service.branding.client.Branding;

/**
 *
 * @author Jerker Klang (jerker.klang@dabox.se)
 */
public class BrandingOutput extends AbstractOrgBrandingOutput implements TemplateDirectiveModel {

    @Override
    public void execute(Environment env, Map params, TemplateModel[] loopVars,
            TemplateDirectiveBody body) throws TemplateException, IOException {

        RealmBrandingOutput.addFavIcons(env);
        RealmBrandingOutput.addRealmBranding(env, "branding-styles");

        Long orgId = getOrgId(env);

        if (orgId == null) {
            return;
        }
        
        RequestCycle cycle = DruwaApplication.getCurrentRequestCycle();

        Branding branding = new GetOrgBrandingCommand(cycle).forOrg(orgId);

        BrandingOutputUtil.outputLink(cycle, env.getOut(), branding, "branding-styles");
    }

}
