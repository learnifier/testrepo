/*
 * (c) Dabox AB 2013 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.command;

import com.google.common.base.Objects;
import java.util.TimeZone;
import net.unixdeveloper.druwa.RequestCycle;
import se.dabox.cocobox.cpweb.module.project.OrgProjectTimezoneFactory;
import se.dabox.cocosite.druwa.CocoSiteConstants;
import se.dabox.service.client.CacheClients;
import se.dabox.service.orgdir.client.OrganizationDirectoryClient;
import se.dabox.util.ParamUtil;
import se.dabox.util.RecentList;

/**
 * Command that update the recent timezone string for a org unit. The string
 * is only updated if the timezone is non-null and the recent string differ from the existing.
 *
 * @author Jerker Klang <jerker.klang@dabox.se>
 */
public class RecentTimezoneUpdateCommand {

    private final RequestCycle cycle;

    public RecentTimezoneUpdateCommand(RequestCycle cycle) {
        ParamUtil.required(cycle, "cycle");
        this.cycle = cycle;
    }

    /**
     * Updates the recent timezone string a organization unit. The string is only
     * updated if the timezone is non-null and the recent string differ from the existing.
     *
     * @param orgUnitId A id to an organization unit.
     * @param tz The latest used timezone or {@code null}.
     * @return {@code true} if the timezone string was updated or false otherwise.
     */
    public boolean updateRecentTimezone(long orgUnitId, TimeZone tz) {
        if (tz == null) {
            return false;
        }

        String orgRecentString = new RecentTimezoneStringForOrgCommand(cycle).forOrgId(orgUnitId);

        RecentList<TimeZone> recent = OrgProjectTimezoneFactory.newRecentList(cycle, orgUnitId);
        recent.addRecent(tz);

        String newRecentString = recent.getRecentString();

        if (Objects.equal(orgRecentString, newRecentString)) {
            return false;
        }

        OrganizationDirectoryClient odClient
                = CacheClients.getClient(cycle, OrganizationDirectoryClient.class);

        odClient.setProfileValue(orgUnitId,
                CocoSiteConstants.ORG_PROFILE_CPWEB,
                CocoSiteConstants.OU_CP_TZRECENT,
                newRecentString);

        return true;
    }
}
