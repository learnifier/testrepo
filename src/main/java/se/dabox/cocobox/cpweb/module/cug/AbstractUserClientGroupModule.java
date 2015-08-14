/*
 * (c) Dabox AB 2015 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.module.cug;

import net.unixdeveloper.druwa.RequestCycle;
import se.dabox.cocobox.cpweb.module.core.AbstractWebAuthModule;
import se.dabox.cocosite.org.MiniOrgInfo;
import se.dabox.service.client.CacheClients;
import se.dabox.service.cug.client.ClientUserGroup;
import se.dabox.service.cug.client.ClientUserGroupClient;

/**
 *
 * @author Magnus Andersson (magnus.andersson@learnifier.com)
 */
public class AbstractUserClientGroupModule extends AbstractWebAuthModule {
    
    protected ClientUserGroupClient getClientUserGroupService(RequestCycle cycle) {
        return CacheClients.getClient(cycle, ClientUserGroupClient.class);
    }

    protected void checkOrgCUGAccess(RequestCycle cycle, MiniOrgInfo org, ClientUserGroup cug) {
        if(cug.getOrgId() != org.getId()) {
            handleAccessDenied(cycle, "Access denied to group from current organization.");
        }
    }

}
