/*
 * (c) Dabox AB 2012 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.module.papi;

import java.util.Map;
import net.unixdeveloper.druwa.RequestCycle;
import net.unixdeveloper.druwa.RequestTarget;
import net.unixdeveloper.druwa.annotation.WebAction;
import net.unixdeveloper.druwa.annotation.mount.WebModuleMountpoint;
import net.unixdeveloper.druwa.freemarker.FreemarkerRequestTarget;
import se.dabox.cocobox.cpweb.module.core.AbstractWebAuthModule;
import se.dabox.cocobox.security.permission.CocoboxPermissions;
import se.dabox.cocosite.druwa.CocoSiteConfKey;
import se.dabox.cocosite.org.MiniOrgInfo;
import se.dabox.service.common.context.Configuration;
import se.dabox.service.common.context.DwsRealmHelper;

/**
 *
 * @author Magnus Andersson (magnus.andersson@learnifier.com)
 */
@WebModuleMountpoint("/papi")
public class PapiModule extends AbstractWebAuthModule {

    @WebAction
    public RequestTarget onListApiKeys(RequestCycle cycle, String strOrgId) {
        checkOrgPermission(cycle, strOrgId, CocoboxPermissions.BO_VIEW_APIKEY);
        MiniOrgInfo org = secureGetMiniOrg(cycle, strOrgId);

        Map<String, Object> map = createMap();
        map.put("org", org);
        map.put("apiEndpoint", getEndpoint(cycle));
        map.put("orgUnitEndpoint", getOrgunitEndpoint(cycle, strOrgId));

        return new FreemarkerRequestTarget("papi/listApiKeys.html", map);
    }

    private String getEndpoint(RequestCycle cycle) {
        Configuration config = DwsRealmHelper.getRealmConfiguration(cycle);

        String apiweb = config.getValue(CocoSiteConfKey.APIWEB_BASEURL);

        return apiweb + "v1/";
    }

    private String getOrgunitEndpoint(RequestCycle cycle, String strOrgId) {
        Configuration config = DwsRealmHelper.getRealmConfiguration(cycle);

        String apiweb = config.getValue(CocoSiteConfKey.APIWEB_BASEURL);

        return apiweb + "v1/orgunits/" + strOrgId;
    }
}
