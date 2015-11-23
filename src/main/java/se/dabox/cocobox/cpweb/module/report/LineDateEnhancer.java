/*
 * (c) Dabox AB 2012 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.module.report;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.unixdeveloper.druwa.RequestCycle;
import se.dabox.cocosite.login.CocositeUserHelper;

/**
 *
 * @author Jerker Klang (jerker.klang@dabox.se)
 */
class LineDateEnhancer {

    private final RequestCycle cycle;
    private final DateFormat df;

    public LineDateEnhancer(RequestCycle cycle) {
        this.cycle = cycle;
        df = DateFormat.getDateTimeInstance(DateFormat.MEDIUM,
                DateFormat.MEDIUM,
                CocositeUserHelper.getUserLocale(cycle));
    }

    List<Map<String, Object>> enhance(List<Map<String, Object>> reportLines) {
        if (reportLines == null || reportLines.isEmpty()) {
            return reportLines;
        }

        List<Map<String, Object>> list = new ArrayList<>(reportLines.size());

        for (Map<String, Object> map : reportLines) {
            Map<String, Object> enhancedMap = new HashMap<>(map);

            dateEnhance(enhancedMap, LineConstants.CREATED);
            dateEnhance(enhancedMap, LineConstants.EXPIRES);

            list.add(enhancedMap);
        }

        return list;
    }

    private void dateEnhance(Map<String, Object> map, String name) {
        Date date = (Date) map.get(name);

        if (date == null) {
            return;
        }

        String strDate = df.format(date);

        map.put(name.concat("Str"), strDate);
    }
}
