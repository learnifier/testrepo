/*
 * (c) Dabox AB 2015 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.formdata.cug;

import java.io.Serializable;
import net.unixdeveloper.druwa.formbean.annotation.FormField;

/**
 *
 * @author Magnus Andersson (magnus.andersson@learnifier.com)
 */
public class AddCugMemberForm implements Serializable {
    private static final long serialVersionUID = 1L;

    private String memberemail;

    @FormField
    public String getMemberemail() {
        return memberemail;
    }

    public void setMemberemail(String memberemail) {
        this.memberemail = memberemail;
    }

    @Override
    public String toString() {
        return "AddCugMemberForm{" + "memberemail=" + memberemail + '}';
    }


}
