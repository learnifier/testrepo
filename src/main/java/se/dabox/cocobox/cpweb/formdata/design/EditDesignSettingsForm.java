/*
 * (c) Dabox AB 2012 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.formdata.design;

import java.io.Serializable;
import net.unixdeveloper.druwa.formbean.annotation.FormField;

/**
 *
 * @author Jerker Klang (jerker.klang@dabox.se)
 */
public class EditDesignSettingsForm implements Serializable {
    private static final long serialVersionUID = 1L;

    private String name;
    private String description;

    @FormField
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @FormField(required=false)
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }



}
