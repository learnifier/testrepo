/*
 * (c) Dabox AB 2012 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.formdata.user;

import java.io.Serializable;
import java.util.Locale;
import net.unixdeveloper.druwa.formbean.annotation.FormField;

/**
 *
 * @author Jerker Klang (jerker.klang@dabox.se)
 */
public class CreateUser implements Serializable {

    private String firstname;
    private String lastname;
    private String email;
    private Locale lang;
    private String role;
    private Long organization;

    @FormField(type = "simpleEmail")
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    @FormField
    public String getFirstname() {
        return firstname;
    }

    public void setFirstname(String firstname) {
        this.firstname = firstname;
    }

    @FormField
    public Locale getLang() {
        return lang;
    }

    public void setLang(Locale lang) {
        this.lang = lang;
    }

    @FormField
    public String getLastname() {
        return lastname;
    }

    public void setLastname(String lastname) {
        this.lastname = lastname;
    }

    @FormField
    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    @FormField
    public Long getOrganization() {
        return organization;
    }

    public void setOrganization(Long organization) {
        this.organization = organization;
    }

    @Override
    public String toString() {
        return "CreateUser{" + "firstname=" + firstname + ", lastname=" + lastname + ", email=" + email + ", lang=" + lang + ", role=" + role + ", organization=" + organization + '}';
    }
    
}
