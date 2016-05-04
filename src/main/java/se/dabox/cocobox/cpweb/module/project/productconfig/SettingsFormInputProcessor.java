/*
 * (c) Dabox AB 2015 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.module.project.productconfig;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.unixdeveloper.druwa.WebRequest;
import org.apache.commons.lang3.StringUtils;
import se.dabox.cocobox.crisp.response.config.ProjectConfigItem;
import se.dabox.cocobox.crisp.response.config.ProjectConfigType;

/**
 *
 * @author Jerker Klang (jerker.klang@learnifier.com)
 */
public class SettingsFormInputProcessor {

    public Map<String, Map<String, String>> processFormInput(
            final List<ExtraProductConfig> extraConfigList, WebRequest req) {
        Map<String, Map<String, String>> productsMap = new HashMap<>();
        for (ExtraProductConfig extraConfig : extraConfigList) {
            String prefix = extraConfig.getFormPrefix();

            Map<String, String> map = new HashMap<>();

            addExtraSettingsValues(req, map, prefix,
                    extraConfig.getProjectConfig().getItems());
            addExtraSettingsValues(req, map, prefix,
                    extraConfig.getProjectConfig().getAdvancedItems());

            productsMap.put(extraConfig.getProductId(), map);
        }
        return productsMap;
    }

    private void addExtraSettingsValues(WebRequest req, Map<String, String> map, String prefix,
            List<ProjectConfigItem> items) {
        for (ProjectConfigItem item : items) {
            String val = req.getParameter(prefix + item.getId());

            if (StringUtils.isBlank(val) && item.getType() == ProjectConfigType.toggle) {
                val = "false";
            }

            if (StringUtils.isBlank(val) && !item.isOptional()) {
                //TODO: Create custom exception
                throw new IllegalArgumentException();
            }

            //TODO: Validate the input
            map.put(item.getId(), StringUtils.trimToNull(val));
        }
    }
}
