/*
 * (c) Dabox AB 2012 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.module;

import java.util.List;
import net.unixdeveloper.druwa.RequestCycle;
import net.unixdeveloper.druwa.RequestTarget;
import net.unixdeveloper.druwa.annotation.DefaultWebAction;
import net.unixdeveloper.druwa.annotation.WebAction;
import net.unixdeveloper.druwa.annotation.mount.WebActionMountpoint;
import net.unixdeveloper.druwa.request.RedirectUrlRequestTarget;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.dabox.cocobox.cpweb.NavigationUtil;
import se.dabox.cocobox.cpweb.module.core.AbstractModule;
import se.dabox.service.client.CacheClients;
import se.dabox.service.orgdir.client.OrgUnitInfo;
import se.dabox.service.orgdir.client.OrganizationDirectoryClient;

/**
 *
 * @author Jerker Klang <jerker.klang@dabox.se>
 */
public class StartPageModule extends AbstractModule {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(StartPageModule.class);

    @WebActionMountpoint("/start")
    @DefaultWebAction
    @WebAction
    public RequestTarget onStart(RequestCycle cycle, String[] params) {
        String id = params.length > 0 ? params[0] : null;

        if (id == null) {
            LOGGER.debug("No id specified for start page");
            throw new IllegalStateException("No start page id specified");
        }

        //TODO: Problem. We don't know which realm we act in (yet)
        //Let us assume that there is only one realm right now

        OrgUnitInfo ou = getOrgUnitFromId(cycle, id);

        if (ou == null) {
            throw new IllegalStateException("No organization with id " + id
                    + " found");
        }

        //TODO: Detect previous login somehow?
        //TODO: Prevent recheck if already logged in as the correct user?

        return new RedirectUrlRequestTarget(NavigationUtil.toOrgFragmentStart(cycle,
                Long.toString(ou.getId())));
    }

    private OrgUnitInfo getOrgUnitFromId(RequestCycle cycle, String id) {
        OrganizationDirectoryClient orgDir =
                CacheClients.getClient(cycle, OrganizationDirectoryClient.class);

        List<OrgUnitInfo> ous = orgDir.listOrgUnits("client");

        for (OrgUnitInfo orgUnitInfo : ous) {
            String siteid = orgUnitInfo.getProfileValue("sitelogin", "siteid");
            
            if (id.equals(siteid)) {
                return orgUnitInfo;
            }

        }
        return null;
    }
}
