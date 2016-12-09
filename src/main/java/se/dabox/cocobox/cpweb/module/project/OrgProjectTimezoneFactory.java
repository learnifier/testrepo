/*
 * (c) Dabox AB 2013 All Rights Reserved
 */

package se.dabox.cocobox.cpweb.module.project;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.TimeZone;
import net.unixdeveloper.druwa.RequestCycle;
import se.dabox.cocobox.cpweb.command.RecentTimezoneStringForOrgCommand;
import se.dabox.cocosite.login.CocositeUserHelper;
import se.dabox.cocosite.timezone.FormTimeZone;
import se.dabox.cocosite.timezone.PlatformFormTimeZoneFactory;
import se.dabox.util.ParamUtil;
import se.dabox.util.RecentList;

/**
 * Factory that creates RecentList instances for project timezones.
 *
 * @author Jerker Klang (jerker.klang@dabox.se)
 */
public final class OrgProjectTimezoneFactory {

    public static RecentList<FormTimeZone> newRecentList(RequestCycle cycle, long orgId) {
        ParamUtil.required(cycle, "cycle");

        String recentString = new RecentTimezoneStringForOrgCommand(cycle).forOrgId(orgId);

        final PlatformFormTimeZoneFactory ftzFactory = new PlatformFormTimeZoneFactory(cycle,
                CocositeUserHelper.getUserLocale(cycle));

        RecentList<FormTimeZone> recentList
                = new RecentList<FormTimeZone>(10) {

            @Override
            protected FormTimeZone stringToObject(
                    Collection<? extends FormTimeZone> allItems, String recentString) {
                return ftzFactory.toFormTimeZone(TimeZone.getTimeZone(recentString));
            }

            @Override
            protected Comparator<FormTimeZone> getSortComparator() {
                return ftzFactory.getComparator();
            }

            @Override
            protected String objectToString(FormTimeZone item) {
                return item.getId();
            }
        };

        List<FormTimeZone> timeZones = NewProjectModule.getTimezones(cycle);

        recentList.process(recentString, timeZones);

        return recentList;
    }

    private OrgProjectTimezoneFactory() {
    }

}
