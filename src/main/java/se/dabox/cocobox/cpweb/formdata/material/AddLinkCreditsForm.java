/*
 * (c) Dabox AB 2012 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.formdata.material;

import java.io.Serializable;
import net.unixdeveloper.druwa.formbean.annotation.FormField;

/**
 *
 * @author Jerker Klang <jerker.klang@dabox.se>
 */
public class AddLinkCreditsForm implements Serializable {

    private Long credits;
    private Long oplid;
    private Long orgId;

    @FormField
    public Long getCredits() {
        return credits;
    }

    public void setCredits(Long credits) {
        this.credits = credits;
    }

    @FormField
    public Long getOplid() {
        return oplid;
    }

    public void setOplid(Long oplid) {
        this.oplid = oplid;
    }

    @FormField
    public Long getOrgId() {
        return orgId;
    }

    public void setOrgId(Long orgId) {
        this.orgId = orgId;
    }
}
