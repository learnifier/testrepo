/*
 * (c) Dabox AB 2012 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.formdata.branding;

import net.unixdeveloper.druwa.formbean.annotation.FormField;

/**
 *
 * @author Jerker Klang (jerker.klang@dabox.se)
 */
public class BrandingColorForm {

    private String navcolor;
    private String primarycolor;
    private String secondarycolor;
    private String topbarcolor;

    @FormField(type="colorpicker")
    public String getNavcolor() {
        return navcolor;
    }

    public void setNavcolor(String navcolor) {
        this.navcolor = navcolor;
    }

    @FormField(type="colorpicker")
    public String getPrimarycolor() {
        return primarycolor;
    }

    public void setPrimarycolor(String primarycolor) {
        this.primarycolor = primarycolor;
    }

    @FormField(type="colorpicker")
    public String getSecondarycolor() {
        return secondarycolor;
    }

    public void setSecondarycolor(String secondarycolor) {
        this.secondarycolor = secondarycolor;
    }
    
    @FormField(type="colorpicker")
    public String getTopbarcolor() {
        return topbarcolor;
    }

    public void setTopbarcolor(String topbarcolor) {
        this.topbarcolor = topbarcolor;
    }



}
