/*
 * (c) Dabox AB 2013 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.formdata.project;

import net.unixdeveloper.druwa.formbean.annotation.FormField;

/**
 *
 * @author Jerker Klang <jerker.klang@dabox.se>
 */
public class MatListProjectDetailsForm {

    private String userTitle;
    private String userDescription;

    @FormField
    public String getUserTitle() {
        return userTitle;
    }

    public void setUserTitle(String userTitle) {
        this.userTitle = userTitle;
    }

    @FormField(required = false)
    public String getUserDescription() {
        return userDescription;
    }

    public void setUserDescription(String userDescription) {
        this.userDescription = userDescription;
    }

}
