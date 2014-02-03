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
public class ChangePassword implements Serializable {

    private String currentpw;
    private String newpw;
    private String repeatnewpw;

    @FormField
    public String getCurrentpw() {
        return currentpw;
    }

    public void setCurrentpw(String currentpw) {
        this.currentpw = currentpw;
    }

    @FormField
    public String getNewpw() {
        return newpw;
    }

    public void setNewpw(String newpw) {
        this.newpw = newpw;
    }

    @FormField
    public String getRepeatnewpw() {
        return repeatnewpw;
    }

    public void setRepeatnewpw(String repeatnewpw) {
        this.repeatnewpw = repeatnewpw;
    }
}
