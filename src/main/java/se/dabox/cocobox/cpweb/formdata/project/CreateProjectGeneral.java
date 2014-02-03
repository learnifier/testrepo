/*
 * (c) Dabox AB 2012 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.formdata.project;

import java.io.Serializable;
import java.util.Locale;
import java.util.TimeZone;
import net.unixdeveloper.druwa.formbean.annotation.FormField;

/**
 *
 * @author Jerker Klang <jerker.klang@dabox.se>
 */
public class CreateProjectGeneral implements Serializable {

    private static final long serialVersionUID = 2L;
    private String projectname;
    private Locale projectlang;
    private Locale country;
    private TimeZone timezone;
    private Long designId;

    @FormField
    public Locale getProjectlang() {
        return projectlang;
    }

    public void setProjectlang(Locale projectlang) {
        this.projectlang = projectlang;
    }

    @FormField
    public String getProjectname() {
        return projectname;
    }

    public void setProjectname(String projectname) {
        this.projectname = projectname;
    }

    @FormField
    public Locale getCountry() {
        return country;
    }

    public void setCountry(Locale country) {
        this.country = country;
    }

    @FormField
    public TimeZone getTimezone() {
        return timezone;
    }

    public void setTimezone(TimeZone timezone) {
        this.timezone = timezone;
    }

    /**
     * Contains the selected designId. If the id is {@literal 0} then
     * this is information about a MatList project.
     *
     * @return
     */
    @FormField
    public Long getDesign() {
        return designId;
    }

    public void setDesign(Long designId) {
        this.designId = designId;
    }
    
}
