/*
 * (c) Dabox AB 2012 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.formdata.project;

import java.io.Serializable;
import net.unixdeveloper.druwa.formbean.annotation.FormField;
import se.dabox.service.common.ccbc.mailfilter.MailFilterTarget;

/**
 *
 * @author Jerker Klang <jerker.klang@dabox.se>
 */
public class AddTaskForm implements Serializable {

    private MailFilterTarget tasktarget;
    private String taskdate;

    @FormField
    public String getTaskdate() {
        return taskdate;
    }
    
    public void setTaskdate(String taskdate) {
        this.taskdate = taskdate;
    }

    @FormField
    public MailFilterTarget getTasktarget() {
        return tasktarget;
    }

    public void setTasktarget(MailFilterTarget tasktarget) {
        this.tasktarget = tasktarget;
    }

    
}
