/*
 * (c) Dabox AB 2012 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.formdata.project;

import java.io.Serializable;
import net.unixdeveloper.druwa.FileUpload;
import net.unixdeveloper.druwa.formbean.annotation.FormField;

/**
 *
 * @author Jerker Klang <jerker.klang@dabox.se>
 */
public class UploadRosterForm implements Serializable {
    private static final long serialVersionUID = 1L;

    private FileUpload file;

    @FormField
    public FileUpload getFile() {
        return file;
    }

    public void setFile(FileUpload file) {
        this.file = file;
    }

}
