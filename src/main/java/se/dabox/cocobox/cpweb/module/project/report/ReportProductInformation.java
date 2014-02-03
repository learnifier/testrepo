/*
 * (c) Dabox AB 2013 All Rights Reserved
 */

package se.dabox.cocobox.cpweb.module.project.report;

import java.util.HashMap;
import java.util.Map;
import net.unixdeveloper.druwa.ServiceRequestCycle;
import se.dabox.service.client.CacheClients;
import se.dabox.service.common.material.Material;
import se.dabox.service.common.proddir.ProductDirectoryClient;
import se.dabox.service.common.proddir.material.ProductMaterialConverter;
import se.dabox.service.proddir.data.Product;
import se.dabox.util.ParamUtil;

/**
 *
 * @author Jerker Klang <jerker.klang@dabox.se>
 */
class ReportProductInformation {
    private final ProductMaterialConverter prodConverter;

    public ReportProductInformation(ServiceRequestCycle cycle) {
        ParamUtil.required(cycle, "cycle");
    
        ProductDirectoryClient pdClient
                = CacheClients.getClient(cycle, ProductDirectoryClient.class);
        prodConverter = new ProductMaterialConverter(cycle, pdClient);
    }

    Map<String, Object> forProduct(Product product) {
        if (product == null) {
            return null;
        }

        Material mat = prodConverter.convert(product);

        Map<String, Object> map = new HashMap<>();

        map.put("productId", mat.getId());
        map.put("title", mat.getTitle());
        map.put("description", mat.getDescription());
        map.put("thumbnail", mat.getThumbnail(64));
        map.put("id", mat.getId());
        map.put("type", product.getProductTypeId().getId());

        return map;
    }

}
