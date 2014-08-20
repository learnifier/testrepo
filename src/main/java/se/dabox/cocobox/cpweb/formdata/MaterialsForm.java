/*
 * (c) Dabox AB 2012 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.formdata;

import java.io.Serializable;
import java.util.Locale;
import net.unixdeveloper.druwa.FileUpload;
import net.unixdeveloper.druwa.formbean.annotation.FormField;

/**
 *
 * @author Jerker Klang (jerker.klang@dabox.se)
 */
public class MaterialsForm implements Serializable {

    private String type;
    private String title;
    private String description;
    private FileUpload file;
    private String link;
    private Locale lang;

    @FormField(required=false)
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @FormField(required=false)
    public FileUpload getFile() {
        return file;
    }

    public void setFile(FileUpload file) {
        this.file = file;
    }

    @FormField
    public Locale getLang() {
        return lang;
    }

    public void setLang(Locale lang) {
        this.lang = lang;
    }

    @FormField(required=false)
    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    @FormField
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    @FormField
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    

}
