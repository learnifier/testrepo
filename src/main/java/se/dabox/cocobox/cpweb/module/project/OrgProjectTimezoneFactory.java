/*
 * (c) Dabox AB 2013 All Rights Reserved
 */

package se.dabox.cocobox.cpweb.module.project;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.TimeZone;
import net.unixdeveloper.druwa.RequestCycle;
import org.apache.commons.lang3.builder.CompareToBuilder;
import se.dabox.cocobox.cpweb.command.RecentTimezoneStringForOrgCommand;
import se.dabox.util.ParamUtil;
import se.dabox.util.RecentList;

/**
 * Factory that creates RecentList instances for project timezones.
 *
 * @author Jerker Klang (jerker.klang@dabox.se)
 */
public final class OrgProjectTimezoneFactory {
    
    public static RecentList<TimeZone> newRecentList(RequestCycle cycle, long orgId) {
        ParamUtil.required(cycle, "cycle");

        String recentString = new RecentTimezoneStringForOrgCommand(cycle).forOrgId(orgId);

        RecentList<TimeZone> recentList
                = new RecentList<TimeZone>(10) {

                    @Override
                    protected TimeZone stringToObject(
                            Collection<? extends TimeZone> allItems, String recentString) {
                                return TimeZone.getTimeZone(recentString);
                            }

                            @Override
                            protected Comparator<TimeZone> getSortComparator() {
                                return new Comparator<TimeZone>() {
                                    @Override
                                    public int compare(TimeZone o1, TimeZone o2) {
                                        return new CompareToBuilder().append(o1.getID(), o2.getID()).
                                                append(o1, o2).build();
                                    }
                                };
                            }

                            @Override
                            protected String objectToString(TimeZone item) {
                                return item.getID();
                            }
                };

        List<TimeZone> timeZones = NewProjectModule.getTimezones(cycle);

        recentList.process(recentString, timeZones);

        return recentList;
    }

    private OrgProjectTimezoneFactory() {
    }

}
