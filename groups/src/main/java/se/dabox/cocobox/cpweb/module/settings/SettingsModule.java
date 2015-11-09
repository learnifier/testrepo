/*
 * (c) Dabox AB 2013 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.module.settings;

import java.util.Map;
import net.unixdeveloper.druwa.RequestCycle;
import net.unixdeveloper.druwa.RequestTarget;
import net.unixdeveloper.druwa.annotation.WebAction;
import net.unixdeveloper.druwa.annotation.mount.WebActionMountpoint;
import net.unixdeveloper.druwa.freemarker.FreemarkerRequestTarget;
import net.unixdeveloper.druwa.request.WebModuleRedirectRequestTarget;
import se.dabox.cocobox.cpweb.module.branding.BrandingModule;
import se.dabox.cocobox.cpweb.module.core.AbstractWebAuthModule;
import se.dabox.cocosite.org.MiniOrgInfo;
import se.dabox.cocobox.security.permission.CocoboxPermissions;

/**
 *
 * @author Jerker Klang (jerker.klang@dabox.se)
 */
public class SettingsModule extends AbstractWebAuthModule {

    @WebAction
    @WebActionMountpoint("/settings")
    public RequestTarget onSettings(RequestCycle cycle, String strOrgId) {
        MiniOrgInfo org = secureGetMiniOrg(cycle, strOrgId);

        if (hasOrgPermission(cycle, org.getId(), CocoboxPermissions.CP_EDIT_BRANDING)) {
            return new WebModuleRedirectRequestTarget(BrandingModule.class, "logo", strOrgId);
        }

        Map<String, Object> map = createMap();
        map.put("org", org);

        return new FreemarkerRequestTarget("/settings/settingsOverview.html", map);
    }

}
