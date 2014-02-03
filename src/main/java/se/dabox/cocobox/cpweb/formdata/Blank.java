/*
 * (c) Dabox AB 2012 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.formdata;

import java.io.Serializable;
import net.unixdeveloper.druwa.formbean.annotation.FormField;

/**
 *
 * @author Jerker Klang <jerker.klang@dabox.se>
 */
public class Blank implements Serializable {

    private String orgname;
    private String orgnumber;

    @FormField
    public String getOrgname() {
        return orgname;
    }

    public void setOrgname(String orgname) {
        this.orgname = orgname;
    }

    @FormField
    public String getOrgnumber() {
        return orgnumber;
    }

    public void setOrgnumber(String orgnumber) {
        this.orgnumber = orgnumber;
    }


}
