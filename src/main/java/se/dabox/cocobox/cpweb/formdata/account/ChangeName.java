/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package se.dabox.cocobox.cpweb.formdata.account;

import java.io.Serializable;
import net.unixdeveloper.druwa.formbean.annotation.FormField;

/**
 *
 * @author borg321
 */
public class ChangeName implements Serializable {

    private String firstname;
    private String lastname;

    @FormField
    public String getFirstname() {
        return firstname;
    }

    public void setFirstname(String firstname) {
        this.firstname = firstname;
    }

    @FormField
    public String getLastname() {
        return lastname;
    }

    public void setLastname(String lastname) {
        this.lastname = lastname;
    }
}
