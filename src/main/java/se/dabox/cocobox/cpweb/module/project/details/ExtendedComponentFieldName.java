/*
 * (c) Dabox AB 2013 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.module.project.details;

import java.util.UUID;
import org.apache.commons.lang3.StringUtils;
import se.dabox.service.common.coursedesign.ComponentFieldName;
import se.dabox.service.common.coursedesign.v1.Component;
import se.dabox.service.common.coursedesign.v1.DataType;

/**
 *
 * @author Jerker Klang <jerker.klang@dabox.se>
 */
public class ExtendedComponentFieldName extends ComponentFieldName {

    private Component component;
    private String stringValue;
    private boolean primaryField;
    private boolean overrideValue;

    public ExtendedComponentFieldName(UUID cid, String name, String defaultValue, DataType dataType,
            boolean mandatory) {
        super(cid, name, defaultValue, dataType, mandatory);
    }

    public Component getComponent() {
        return component;
    }

    public void setComponent(Component component) {
        this.component = component;
    }

    public String getStringValue() {
        return StringUtils.trimToEmpty(stringValue);
    }

    public void setStringValue(String stringValue) {
        this.stringValue = stringValue;
    }

    public boolean isPrimaryField() {
        return primaryField;
    }

    public void setPrimaryField(boolean primaryField) {
        this.primaryField = primaryField;
    }

    public boolean isOverrideValue() {
        return overrideValue;
    }

    public void setOverrideValue(boolean overrideValue) {
        this.overrideValue = overrideValue;
    }
    
}
