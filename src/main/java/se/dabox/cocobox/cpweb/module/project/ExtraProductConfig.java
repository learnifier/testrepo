/*
 * (c) Dabox AB 2015 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.module.project;

import java.io.Serializable;
import se.dabox.cocobox.crisp.response.ProjectConfigResponse;

/**
 *
 * @author Jerker Klang (jerker.klang@dabox.se)
 */
public class ExtraProductConfig implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private final String productId;
    private final ProjectConfigResponse projectConfig;

    public ExtraProductConfig(String productId, ProjectConfigResponse projectConfig) {
        this.productId = productId;
        this.projectConfig = projectConfig;
    }

    public String getProductId() {
        return productId;
    }

    public ProjectConfigResponse getProjectConfig() {
        return projectConfig;
    }

    @Override
    public String toString() {
        return "ProductConfig{" + "productId=" + productId + ", projectConfig=" + projectConfig + '}';
    }
    
}
