/*
 * (c) Dabox AB 2012 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.module.user;

import java.util.Map;
import net.unixdeveloper.druwa.RequestCycle;
import net.unixdeveloper.druwa.RequestTarget;
import net.unixdeveloper.druwa.annotation.WebAction;
import net.unixdeveloper.druwa.annotation.mount.WebModuleMountpoint;
import net.unixdeveloper.druwa.freemarker.FreemarkerRequestTarget;
import net.unixdeveloper.druwa.request.ErrorCodeRequestTarget;
import net.unixdeveloper.druwa.request.RedirectUrlRequestTarget;
import se.dabox.cocobox.cpweb.module.core.AbstractModule;
import se.dabox.cocosite.branding.GetRealmBrandingId;
import se.dabox.cocosite.druwa.CocoSiteConstants;
import se.dabox.service.common.json.JsonUtils;

/**
 *
 * @author Jerker Klang (jerker.klang@dabox.se)
 */
@WebModuleMountpoint("/registration")
public class AdminRegistrationModule extends AbstractModule {

    @WebAction(name="a")
    public RequestTarget onFirstStage(RequestCycle cycle, String id) {
        Map<String,?> map = getStoredMap(cycle, id);

        if (map == null) {
            return new ErrorCodeRequestTarget(404, "id not found");
        }

        Boolean consumed = (Boolean) map.get("consumed");

        if (consumed != null && consumed) {
            return new FreemarkerRequestTarget("/registration/registrationAlreadyDone.html", null);
        }

        long userId = ((Number) map.get("userId")).longValue();
        String url = cycle.urlFor(AdminRegistrationCompletionModule.class,
                AdminRegistrationCompletionModule.ACTION, id);

        long brandingId = new GetRealmBrandingId(cycle).getBrandingId();

        String loginUrl = getServiceClientFactory(cycle).getLoginService().autoLogin(userId, url,
                true, CocoSiteConstants.DEFAULT_LOGIN_SKIN, brandingId);
        
        return new RedirectUrlRequestTarget(loginUrl);
    }

    private Map<String, ?> getStoredMap(RequestCycle cycle, String id) {
        if (id == null) {
            return null;
        }

        String json = getRandomDataClient(cycle).getRandomData(id);

        if (json == null) {
            return null;
        }

        return JsonUtils.decode(json);
    }

}
