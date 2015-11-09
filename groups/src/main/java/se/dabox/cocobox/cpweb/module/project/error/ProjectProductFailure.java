/*
 * (c) Dabox AB 2013 All Rights Reserved
 */

package se.dabox.cocobox.cpweb.module.project.error;

import java.io.Serializable;

/**
 *
 * @author Jerker Klang (jerker.klang@dabox.se)
 */
public class ProjectProductFailure implements Serializable {
    public static final String VIEWSESSION_NAME = "projectProductFailure";
    
    private static final long serialVersionUID = 1L;
    
    private final String productId;
    private final String productName;
    private final String type;
    private final String errorDescription;

    public ProjectProductFailure(String productId, String productName, String type,
            String errorDescription) {
        this.productId = productId;
        this.productName = productName;
        this.type = type;
        this.errorDescription = errorDescription;
    }

    public static long getSerialVersionUID() {
        return serialVersionUID;
    }

    public String getProductId() {
        return productId;
    }

    public String getProductName() {
        return productName;
    }

    public String getType() {
        return type;
    }

    public String getErrorDescription() {
        return errorDescription;
    }

    @Override
    public String toString() {
        return "ProjectProductFailure{" + "productId=" + productId + ", productName=" + productName +
                ", type=" + type + ", errorDescription=" + errorDescription + '}';
    }
    
}
