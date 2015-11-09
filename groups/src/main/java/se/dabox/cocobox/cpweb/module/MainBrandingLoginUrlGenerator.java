/*
 * (c) Dabox AB 2012 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.module;

import net.unixdeveloper.druwa.RequestCycle;
import net.unixdeveloper.druwa.request.WebModuleRequestTarget;
import se.dabox.cocosite.branding.GetOrgBrandingIdCommand;
import se.dabox.service.webutils.login.generator.LoginSiteLoginUrlGenerator;
import se.dabox.service.webutils.login.legacylogin.LegacyLoginCheck;

/**
 *
 * @author Jerker Klang (jerker.klang@dabox.se)
 */
public class MainBrandingLoginUrlGenerator extends LoginSiteLoginUrlGenerator {

    @Override
    public String generateLoginUrl(LegacyLoginCheck loginCheck, RequestCycle cycle) {

        brandingCheck(cycle);
        return super.generateLoginUrl(loginCheck, cycle);
    }

    private void brandingCheck(RequestCycle cycle) {
        WebModuleRequestTarget target = (WebModuleRequestTarget) cycle.getTarget();

        if ("homeFirst".equals(target.getAction().getName())) {
            String strOrgId = target.getParameters()[0];
            try {
                Long orgId = Long.valueOf(strOrgId);
                Long brandingId = getBrandingForOrg(cycle, orgId);
                if (brandingId != null) {
                    cycle.setAttribute(LoginSiteLoginUrlGenerator.BRANDING_ID, brandingId);
                }
            } catch (NumberFormatException numberFormatException){
                return;
            }
        }
    }

    private Long getBrandingForOrg(RequestCycle cycle, Long orgId) {
        return new GetOrgBrandingIdCommand(cycle).forOrg(orgId);
    }

}
