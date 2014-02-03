/*
 * (c) Dabox AB 2012 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.formdata.project;

import java.io.Serializable;
import net.unixdeveloper.druwa.formbean.annotation.FormField;

/**
 *
 * @author Jerker Klang <jerker.klang@dabox.se>
 */
public class AddMemberForm implements Serializable {

    private String memberfirstname;
    private String memberlastname;
    private String memberemail;

    @FormField
    public String getMemberemail() {
        return memberemail;
    }

    public void setMemberemail(String memberemail) {
        this.memberemail = memberemail;
    }

    @FormField
    public String getMemberfirstname() {
        return memberfirstname;
    }

    public void setMemberfirstname(String memberfirstname) {
        this.memberfirstname = memberfirstname;
    }

    @FormField
    public String getMemberlastname() {
        return memberlastname;
    }

    public void setMemberlastname(String memberlastname) {
        this.memberlastname = memberlastname;
    }




}
