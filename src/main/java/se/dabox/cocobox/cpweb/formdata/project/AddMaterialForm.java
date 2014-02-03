/*
 * (c) Dabox AB 2012 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.formdata.project;

import net.unixdeveloper.druwa.formbean.annotation.FormField;

/**
 *
 * @author Jerker Klang <jerker.klang@dabox.se>
 */
public class AddMaterialForm {
    private String materialId;

    @FormField
    public String getMaterialId() {
        return materialId;
    }

    public void setMaterialId(String materialId) {
        this.materialId = materialId;
    }

}
