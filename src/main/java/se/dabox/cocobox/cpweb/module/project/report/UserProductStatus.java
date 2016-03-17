/*
 * (c) Dabox AB 2013 All Rights Reserved
 */

package se.dabox.cocobox.cpweb.module.project.report;

import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import se.dabox.service.common.coursedesign.extstatus.ExtendedStatus;
import se.dabox.service.proddir.data.ProductId;

/**
 *
 * @author Jerker Klang (jerker.klang@dabox.se)
 */
public class UserProductStatus {
    private final long userId;
    private final Map<String, ExtendedStatus> productStatusMap = new HashMap<>();

    public UserProductStatus(long userId) {
        this.userId = userId;
    }

    public long getUserId() {
        return userId;
    }

    public Map<String, ExtendedStatus> getProductStatusMap() {
        return productStatusMap;
    }

    public void addStatus(ProductId productId, ExtendedStatus status) {
        if (status != null) {
            productStatusMap.merge(productId.getId(), status, UserProductStatus::merge);
        }
    }

    public static ExtendedStatus merge(@Nullable ExtendedStatus oldStatus,
            @Nonnull ExtendedStatus newStatus) {
        if (oldStatus == null) {
            return newStatus;
        }

        if (newStatus.compareTo(oldStatus) > 0) {
            return newStatus;
        }

        return oldStatus;
    }

}
