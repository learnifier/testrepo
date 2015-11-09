/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package se.dabox.cocobox.cpweb.formdata;

import java.io.Serializable;
import net.unixdeveloper.druwa.formbean.annotation.FormField;

/**
 *
 * @author borg321
 */
public class SendEmailForm implements Serializable {
    private static final long serialVersionUID = 1L;

    private String subject;
    private String body;
    private String mtype;

    @FormField(required=false)
    public String getMtype() {
        return mtype;
    }

    public void setMtype(String mtype) {
        this.mtype = mtype;
    }


    @FormField()
    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    @FormField
    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }
}
