/*
 * (c) Dabox AB 2012 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.formdata.project;

import net.unixdeveloper.druwa.formbean.annotation.FormField;

/**
 *
 * @author Jerker Klang <jerker.klang@dabox.se>
 */
public class SetRegPasswordForm {

    private String password;
    private Boolean passwordEnabled;

    @FormField(required=false)
    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @FormField
    public Boolean getPasswordEnabled() {
        return passwordEnabled;
    }

    public void setPasswordEnabled(Boolean passwordEnabled) {
        this.passwordEnabled = passwordEnabled;
    }
 
}
