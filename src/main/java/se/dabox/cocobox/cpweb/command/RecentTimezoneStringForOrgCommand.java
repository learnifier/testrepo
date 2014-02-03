/*
 * (c) Dabox AB 2013 All Rights Reserved
 */

package se.dabox.cocobox.cpweb.command;

import net.unixdeveloper.druwa.RequestCycle;
import se.dabox.cocosite.druwa.CocoSiteConstants;
import se.dabox.service.client.CacheClients;
import se.dabox.service.orgdir.client.OrgUnitInfo;
import se.dabox.service.orgdir.client.OrganizationDirectoryClient;
import se.dabox.util.ParamUtil;

/**
 *
 * @author Jerker Klang <jerker.klang@dabox.se>
 */
public class RecentTimezoneStringForOrgCommand {
    private final RequestCycle cycle;

    public RecentTimezoneStringForOrgCommand(RequestCycle cycle) {
        ParamUtil.required(cycle, "cycle");
        this.cycle = cycle;
    }

    public String forOrgId(long orgId) {
        OrganizationDirectoryClient odClient
                = CacheClients.getClient(cycle, OrganizationDirectoryClient.class);

        OrgUnitInfo ou = odClient.getOrgUnitInfo(orgId);

        if (ou == null) {
            throw new IllegalArgumentException("Invalid orgunit id: "+orgId);
        }

        return forOrgUnit(ou);
    }

    private String forOrgUnit(OrgUnitInfo ou) {
        ParamUtil.required(ou, "ou");

        String recentString = ou.getProfileValue(CocoSiteConstants.ORG_PROFILE_CPWEB,
                CocoSiteConstants.OU_CP_TZRECENT);

        return recentString;
    }
}
