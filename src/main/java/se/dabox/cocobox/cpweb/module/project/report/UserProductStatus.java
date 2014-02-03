/*
 * (c) Dabox AB 2013 All Rights Reserved
 */

package se.dabox.cocobox.cpweb.module.project.report;

import java.util.HashMap;
import java.util.Map;
import se.dabox.service.proddir.data.ProductId;

/**
 *
 * @author Jerker Klang <jerker.klang@dabox.se>
 */
public class UserProductStatus {
    private final long userId;
    private final Map<String, String> productStatusMap = new HashMap<>();

    public UserProductStatus(long userId) {
        this.userId = userId;
    }

    public long getUserId() {
        return userId;
    }

    public Map<String, String> getProductStatusMap() {
        return productStatusMap;
    }

    public void addStatus(ProductId productId, String status) {
        productStatusMap.put(productId.getId(), status);
    }

}
