/*
 * (c) Dabox AB 2012 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.module.favorites;

import java.util.Map;
import net.unixdeveloper.druwa.RequestCycle;
import net.unixdeveloper.druwa.RequestTarget;
import net.unixdeveloper.druwa.annotation.WebAction;
import net.unixdeveloper.druwa.annotation.mount.WebModuleMountpoint;
import se.dabox.cocobox.cpweb.module.core.AbstractJsonAuthModule;
import se.dabox.service.webutils.login.LoginUserAccountHelper;

/**
 *
 * @author Jerker Klang (jerker.klang@dabox.se)
 */
@WebModuleMountpoint("/favorites.json")
public class FavoritesJsonModule extends AbstractJsonAuthModule {

    @WebAction
    public RequestTarget onAdd(RequestCycle cycle) {
        String strProjectId = cycle.getRequest().getParameter("projectId");

        long projectId = Long.valueOf(strProjectId);

        long userId = LoginUserAccountHelper.getUserId(cycle);
        getCocoboxCordinatorClient(cycle).addFavorite(userId, projectId);

        Map<String, Object> map = createMap();
        map.put("status", "OK");

        return jsonTarget(map);
    }

    @WebAction
    public RequestTarget onDelete(RequestCycle cycle) {
        String strProjectId = cycle.getRequest().getParameter("projectId");

        long projectId = Long.valueOf(strProjectId);

        long userId = LoginUserAccountHelper.getUserId(cycle);
        getCocoboxCordinatorClient(cycle).deleteFavorite(userId, projectId);

        Map<String, Object> map = createMap();
        map.put("status", "OK");

        return jsonTarget(map);
    }
}
