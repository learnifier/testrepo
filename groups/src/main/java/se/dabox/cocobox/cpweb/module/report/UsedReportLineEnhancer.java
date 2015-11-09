/*
 * (c) Dabox AB 2012 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.module.report;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.unixdeveloper.druwa.RequestCycle;
import org.apache.commons.lang3.StringUtils;
import se.dabox.service.client.CacheClients;
import se.dabox.service.common.ccbc.CocoboxCoordinatorClient;
import se.dabox.service.common.ccbc.org.OrgProduct;
import se.dabox.service.common.ccbc.org.OrgProductLink;
import se.dabox.service.common.ccbc.project.OrgProject;
import se.dabox.service.common.proddir.ProductDirectoryClient;
import se.dabox.service.proddir.data.Product;

/**
 *
 * @author Jerker Klang (jerker.klang@dabox.se)
 */
class UsedReportLineEnhancer {

    private final RequestCycle cycle;
    private final CocoboxCoordinatorClient ccbc;
    private final ProductDirectoryClient pdClient;

    UsedReportLineEnhancer(RequestCycle cycle) {
        this.cycle = cycle;

        ccbc = CacheClients.getClient(cycle, CocoboxCoordinatorClient.class);
        pdClient = CacheClients.getClient(cycle, ProductDirectoryClient.class);
    }

    List<Map<String, Object>> enhance(List<Map<String, Object>> reportLines) {
        if (reportLines == null || reportLines.isEmpty()) {
            return reportLines;
        }

        List<Map<String, Object>> list = new ArrayList<>(reportLines.size());

        for (Map<String, Object> map : reportLines) {
            String type = (String) map.get("targetType");
            if (type == null) {
                list.add(map);
                continue;
            }

            Map<String, Object> enhancedMap = new HashMap<>(map);

            switch (type) {
                case "project":
                    enhancedMap.put("name", getProjectName(map));
                    break;
                case "deeplink":
                    enhancedMap.put("name", getDeeplinkName(map));
                    break;
                default:
                    enhancedMap.put("name", null);
                    break;
            }

            list.add(enhancedMap);
        }

        return list;
    }

    private String getProjectName(Map<String, Object> map) {
        String strProjectId = (String) map.get("projectId");

        long projectId = Long.parseLong(strProjectId);
        OrgProject prj = ccbc.getProject(projectId);
        if (prj == null) {
            return null;
        }

        return prj.getName();
    }

    private String getDeeplinkName(Map<String, Object> map) {
        String strDeeplinkId = (String) map.get("deeplinkId");

        long deeplinkId = Long.parseLong(strDeeplinkId);
        OrgProductLink link = ccbc.getOrgProductLink(deeplinkId);
        if (link == null) {
            return null;
        }

        if (!StringUtils.isEmpty(link.getTitle())) {
            return link.getTitle();
        }

        //TODO: This usees the product
        //Need to find org product by id
        OrgProduct op = ccbc.getOrgProduct(link.getOrgProductId());

        if (op == null) {
            return null;
        }

        Product product = pdClient.getProduct(op.getProdId());

        if (product == null) {
            return null;
        }

        return product.getTitle();
    }
}
