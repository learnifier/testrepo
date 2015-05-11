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
import se.dabox.cocosite.security.CocoboxPermissions;

/**
 *
 * @author Magnus Andersson (magnus.andersson@learnifier.com)
 */
@WebModuleMountpoint("/papi")
public class PapiModule extends AbstractWebAuthModule {

    @WebAction
//    @WebActionMountpoint("/listApiKeys")
    public RequestTarget onListApiKeys(RequestCycle cycle, String strOrgId) {
      checkOrgPermission(cycle, strOrgId, CocoboxPermissions.BO_VIEW_APIKEY);

       Map<String, Object> map = createMap();
       map.put("orgId", strOrgId); // Long?
        return new FreemarkerRequestTarget("papi/listApiKeys.html", map);
    }

}
