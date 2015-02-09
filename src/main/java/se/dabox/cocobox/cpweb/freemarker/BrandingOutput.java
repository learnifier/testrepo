/*
 * (c) Dabox AB 2012 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.freemarker;

import freemarker.core.Environment;
import freemarker.template.TemplateDirectiveBody;
import freemarker.template.TemplateDirectiveModel;
import freemarker.template.TemplateException;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.io.IOException;
import java.util.Map;
import net.unixdeveloper.druwa.DruwaApplication;
import net.unixdeveloper.druwa.RequestCycle;
import se.dabox.cocosite.branding.GetOrgBrandingCommand;
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

        Branding branding = getBranding(env);

        RequestCycle cycle = DruwaApplication.getCurrentRequestCycle();
        BrandingOutputUtil.outputLink(cycle, env.getOut(), branding, "branding-styles");
    }

    public Branding getBranding(Environment env) throws TemplateModelException {
        Long orgId = getOrgId(env);

        if (orgId == null) {
            return null;
        }

        RequestCycle cycle = DruwaApplication.getCurrentRequestCycle();

        Branding branding = new GetOrgBrandingCommand(cycle).forOrg(orgId);

        return branding;
    }

}
