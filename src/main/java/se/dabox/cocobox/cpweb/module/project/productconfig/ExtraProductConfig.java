/*
 * (c) Dabox AB 2015 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.module.project.productconfig;

import java.io.Serializable;
import java.util.Map;
import se.dabox.cocobox.crisp.response.config.ProjectConfigResponse;

/**
 *
 * @author Jerker Klang (jerker.klang@dabox.se)
 */
public class ExtraProductConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String productId;
    private final ProjectConfigResponse projectConfig;

    private Map<String, String> settings;

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

    public Map<String, String> getSettings() {
        return settings;
    }

    public void setSettings(Map<String, String> settings) {
        this.settings = settings;
    }

    @Override
    public String toString() {
        return "ExtraProductConfig{" + "productId=" + productId + ", projectConfig=" + projectConfig
                + ", settings=" + settings + '}';
    }

    public String getFormPrefix() {
        return productId + '-';
    }

}
