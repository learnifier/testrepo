/*
 * (c) Dabox AB 2012 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.formdata.mail;

import java.io.Serializable;
import java.util.Locale;
import net.unixdeveloper.druwa.formbean.annotation.FormField;
import net.unixdeveloper.druwa.formbean.annotation.MatchPattern;

/**
 *
 * @author Jerker Klang (jerker.klang@dabox.se)
 */
public class EmailSettingForm implements Serializable {
    private static final long serialVersionUID = 1L;

    private String name;
    private String description;
    private Locale lang;

   
    @FormField
    @MatchPattern("^.{1,96}$")
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @FormField
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @FormField
    public Locale getLang() {
        return lang;
    }

    public void setLang(Locale lang) {
        this.lang = lang;
    }

    
    
}
