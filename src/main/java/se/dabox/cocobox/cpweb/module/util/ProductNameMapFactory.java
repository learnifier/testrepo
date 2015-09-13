/*
 * (c) Dabox AB 2015 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.module.util;

import java.util.List;
import java.util.Map;
import java.util.Set;
import net.unixdeveloper.druwa.DruwaService;
import net.unixdeveloper.druwa.ServiceRequestCycle;
import se.dabox.cocobox.cpweb.module.project.ExtraProductConfig;
import se.dabox.service.client.CacheClients;
import se.dabox.service.common.proddir.ProductDirectoryClient;
import se.dabox.service.proddir.data.Product;
import se.dabox.util.collections.CollectionsUtil;
import se.dabox.util.collections.MapUtil;

/**
 *
 * @author Jerker Klang (jerker.klang@dabox.se)
 */
public class ProductNameMapFactory {

    public ProductNameMapFactory() {
    }

    public Map<String,String> create(List<ExtraProductConfig> extraConfig) {
        Set<String> productIds = CollectionsUtil.transform(extraConfig, c -> c.getProductId());

        return createMap(productIds);
    }

    private Map<String, String> createMap(Set<String> productIds) {
        Map<String,String> map = MapUtil.createHash(productIds);

        final ServiceRequestCycle cycle = DruwaService.getCurrentCycle();
        ProductDirectoryClient pdClient
                = CacheClients.getClient(cycle, ProductDirectoryClient.class);

        List<Product> products = pdClient.getProducts(productIds);

        for (Product product : products) {
            map.put(product.getId().getId(), product.getTitle());
        }

        return map;
    }

}
