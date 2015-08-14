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
public class CreateClientUserGroup implements Serializable {

    private String name;
    private Long parent;

    @FormField
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @FormField(required = false)
    public Long getParent() {
        return parent;
    }

    public void setParent(Long parent) {
        this.parent = parent;
    }

    @Override
    public String toString() {
        return "CreateClientUserGroup{" + "name=" + name + ", parent=" + parent + '}';
    }
}
