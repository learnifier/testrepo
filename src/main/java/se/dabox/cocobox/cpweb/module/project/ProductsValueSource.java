/*
 * (c) Dabox AB 2015 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.module.project;

import java.text.DateFormat;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.List;
import java.util.Locale;
import se.dabox.cocobox.crisp.response.config.ProjectConfigItem;
import se.dabox.cocobox.crisp.response.config.ProjectConfigType;

/**
 *
 * @author Jerker Klang (jerker.klang@dabox.se)
 */
public class ProductsValueSource {
    private static final ProductValue BLANK = new ProductValue("", "");

    private final List<ExtraProductConfig> configs;
    private final Locale locale;
    private final DateTimeFormatter dateInstance;
    private final DateTimeFormatter dateTimeInstance;

    public ProductsValueSource(Locale locale, List<ExtraProductConfig> configs) {
        this.locale = locale;
        this.configs = configs;

        dateInstance = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale);
        dateTimeInstance = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT).withLocale(locale);
    }

    public ProductValue getValue(String productId, String id) {
        ExtraProductConfig config = getConfig(productId);

        if (config == null) {
            return BLANK;
        }

        ProjectConfigItem item = config.getProjectConfig().getItem(id);

        if (item == null || item.getValue() == null) {
            return BLANK;
        }

        String val = item.getValue().toString();
        String name = val;

        if (item.getType() == ProjectConfigType.date) {
            name = dateInstance.format(item.getValue().getDate());
        }

        return new ProductValue(val, name);
    }

    private ExtraProductConfig getConfig(String productId) {
        for (ExtraProductConfig config : configs) {
            if (config.getProductId().equals(productId)) {
                return config;
            }
        }
        
        return null;
    }

    public static class ProductValue {
        public String value;
        public String name;

        public ProductValue(String value, String name) {
            this.value = value;
            this.name = name;
        }

        public String getValue() {
            return value;
        }

        public String getName() {
            return name;
        }
        
    }

}
