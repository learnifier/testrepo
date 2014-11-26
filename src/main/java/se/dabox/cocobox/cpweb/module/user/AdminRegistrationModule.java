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
import se.dabox.cocobox.cpweb.module.project.role.ActivateNewProjectAdmin;
import se.dabox.cocosite.branding.GetRealmBrandingId;
import se.dabox.cocosite.druwa.CocoSiteConstants;
import se.dabox.service.client.CacheClients;
import se.dabox.service.common.ccbc.project.role.ProjectRoleAdminTokenGenerator;
import se.dabox.service.common.json.JsonUtils;
import se.dabox.service.login.client.LoginService;
import se.dabox.service.orgadmin.client.ActivateCpAdminTokenGenerator;

/**
 * Handles registration of activation of admins and project admins.
 *
 * <p>This module are not having a login checker because the user is not logged in yet</p>
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

        String type = (String) map.get("type");

        final String url = processRegistration(cycle, id, type, map);

        long userId = ((Number) map.get("userId")).longValue();

        long brandingId = new GetRealmBrandingId(cycle).getBrandingId();

        String loginUrl = CacheClients.getClient(cycle, LoginService.class).
                autoLogin(userId, url, true, CocoSiteConstants.DEFAULT_LOGIN_SKIN, brandingId);
        
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

    private String processRegistration(RequestCycle cycle, String id, String type,
            Map<String, ?> map) {
        switch(type) {
            case ActivateCpAdminTokenGenerator.TYPE:
                return cycle.urlFor(AdminRegistrationCompletionModule.class,
                        AdminRegistrationCompletionModule.ACTION, id);
            case ProjectRoleAdminTokenGenerator.TYPE:
                return activateProjectAdmin(cycle, id, map);
            default:
                throw new IllegalStateException("Unknown registration type for session "+id+": "+type);
        }
    }

    private String activateProjectAdmin(RequestCycle cycle, String id,
            Map<String, ?> map) {
        return new ActivateNewProjectAdmin(cycle, id, map).getTargetUrl();
    }

}
