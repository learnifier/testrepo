/*
 * (c) Dabox AB 2012 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.module.project.credit;

import java.io.Serializable;

/**
 *
 * @author Jerker Klang <jerker.klang@dabox.se>
 */
public class CreditAllocationFailure implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String productId;
    private final String productName;
    private final long requestedCredits;
    private final long availableCredits;

    public CreditAllocationFailure(String productId, String productName, long requestedCredits,
            long availableCredits) {
        this.productId = productId;
        this.productName = productName;
        this.requestedCredits = requestedCredits;
        this.availableCredits = availableCredits;
    }

    public String getProductId() {
        return productId;
    }

    public String getProductName() {
        return productName;
    }

    public long getRequestedCredits() {
        return requestedCredits;
    }

    public long getAvailableCredits() {
        return availableCredits;
    }

    @Override
    public String toString() {
        return "CreditAllocationFailure{" + "productId=" + productId + ", productName=" + productName +
                ", requestedCredits=" + requestedCredits + ", availableCredits=" + availableCredits +
                '}';
    }
    
}
