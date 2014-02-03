/*
 * (c) Dabox AB 2012 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.formdata.project;

import net.unixdeveloper.druwa.formbean.annotation.FormField;
import net.unixdeveloper.druwa.formbean.annotation.NumConstraint;

/**
 *
 * @author Jerker Klang <jerker.klang@dabox.se>
 */
public class SetRegCreditLimitForm {

    private Integer creditLimit;
    private Boolean creditLimitEnabled;
   
    @FormField(required=false)
    @NumConstraint(min=1)
    public Integer getCreditLimit() {
        return creditLimit;
    }

    public void setCreditLimit(Integer creditLimit) {
        this.creditLimit = creditLimit;
    }

    @FormField
    public Boolean getCreditLimitEnabled() {
        return creditLimitEnabled;
    }

    public void setCreditLimitEnabled(Boolean creditLimitEnabled) {
        this.creditLimitEnabled = creditLimitEnabled;
    }
    
}
