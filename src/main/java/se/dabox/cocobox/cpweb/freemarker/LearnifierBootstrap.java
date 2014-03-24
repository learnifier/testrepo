/*
 * (c) Dabox AB 2013 All Rights Reserved
 */

package se.dabox.cocobox.cpweb.freemarker;

import freemarker.core.Environment;
import freemarker.template.TemplateDirectiveBody;
import freemarker.template.TemplateDirectiveModel;
import freemarker.template.TemplateException;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.utility.DeepUnwrap;
import java.io.IOException;
import java.util.Map;
import net.unixdeveloper.druwa.DruwaApplication;
import net.unixdeveloper.druwa.RequestCycle;
import se.dabox.cocobox.cpweb.command.GetOrgBrandingCommand;
import se.dabox.cocosite.branding.GetRealmBrandingId;
import se.dabox.cocosite.branding.freemarker.BrandingOutputUtil;
import se.dabox.service.branding.client.Branding;
import se.dabox.service.branding.client.BrandingClient;
import se.dabox.service.client.CacheClients;

/**
 * Client Portal freemarker directive that outputs the learnifier bootstrap file.
 * The file is either the client one (if it exists) or the realm wide.
 *
 * @author Jerker Klang <jerker.klang@dabox.se>
 */
public class LearnifierBootstrap extends AbstractBrandingOutput implements TemplateDirectiveModel {
    private static final LearnifierBootstrap INSTANCE = new LearnifierBootstrap();

    public static final String NAME = "learnifierBootstrap";

    public static final String CSS_NAME = "learnifier-bootstrap";

    public static LearnifierBootstrap getInstance() {
        return INSTANCE;
    }

    @Override
    public void execute(Environment env, Map params, TemplateModel[] loopVars,
            TemplateDirectiveBody body) throws TemplateException, IOException {

        Branding branding = getBranding(env);

        RequestCycle cycle = DruwaApplication.getCurrentRequestCycle();
        BrandingOutputUtil.outputLink(cycle, env.getOut(), branding, CSS_NAME);
    }

    private Branding getBranding(Environment env) throws TemplateModelException {
        TemplateModel orgModel = env.getVariable("org");

        if (orgModel == null) {
            return getRealmBranding();
        }

        long orgId = getOrgId(DeepUnwrap.unwrap(orgModel));

        RequestCycle cycle = DruwaApplication.getCurrentRequestCycle();

        Branding branding = new GetOrgBrandingCommand(cycle).forOrg(orgId);

        if (branding != null && !branding.getGeneratedData().containsKey(CSS_NAME)) {
            return getRealmBranding();
        }

        return branding;
    }

    private Branding getRealmBranding() {
        RequestCycle cycle = DruwaApplication.getCurrentRequestCycle();
        long brandingId = new GetRealmBrandingId(cycle).getBrandingId();

        BrandingClient brandingClient = CacheClients.getClient(cycle, BrandingClient.class);

        return brandingClient.getBranding(brandingId);
    }

}