/*
 * (c) Dabox AB 2012 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.excel;

import org.apache.commons.lang3.StringUtils;

/**
 *
 * @author Jerker Klang (jerker.klang@dabox.se)
 */
public class Contact {
    private String givenName;
    private String surname;
    private String email;

    public Contact(String givenName, String surname, String email) {
        this.givenName = givenName;
        this.surname = surname;
        this.email = StringUtils.trim(email);
    }

    public String getGivenName() {
        return givenName;
    }

    public void setGivenName(String givenName) {
        this.givenName = givenName;
    }

    public String getSurname() {
        return surname;
    }

    public void setSurname(String surname) {
        this.surname = surname;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    @Override
    public String toString() {
        return "Contact{" + "givenName=" + givenName + ", surname=" + surname + ", email=" + email +
                '}';
    }
    
}
