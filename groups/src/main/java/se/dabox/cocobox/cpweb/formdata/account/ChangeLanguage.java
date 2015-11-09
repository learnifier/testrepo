/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package se.dabox.cocobox.cpweb.formdata.account;

import java.io.Serializable;
import java.util.Locale;
import net.unixdeveloper.druwa.formbean.annotation.FormField;

/**
 *
 * @author borg321
 */
public class ChangeLanguage implements Serializable {

    private Locale locale;

    @FormField
    public Locale getLang() {
        return locale;
    }

    public void setLang(Locale lang) {
        this.locale = lang;
    }
}
