/*
 * (c) Dabox AB 2013 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.module.project.details;

import java.net.URI;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import se.dabox.cocobox.cpweb.module.project.VerifyProjectDesignModule;
import se.dabox.service.common.ccbc.project.OrgProject;
import se.dabox.service.common.coursedesign.v1.DataType;
import se.dabox.util.ParamUtil;

/**
 *
 * @author Jerker Klang <jerker.klang@dabox.se>
 */
public class ProjectTypeValueValidator {

    public static boolean valid(OrgProject project, DataType dataType, String value) {
        ParamUtil.required(project, "project");
        ParamUtil.required(dataType, "dataType");
        ParamUtil.required(value, "value");

        switch (dataType) {
            case DATETIME:
                try {
                    SimpleDateFormat sdf =
                            VerifyProjectDesignModule.getDatePickerSimpleDateFormat(project);
                    Date date = sdf.parse(value);
                    return true;
                } catch (ParseException ex) {
                    return false;
                }
            case NUMBER:
                try {
                    Long.valueOf(value);
                    return true;
                } catch (NumberFormatException nfe) {
                    return false;
                }
            case URL:
                try {
                    new URI(value);
                } catch (URISyntaxException ex) {
                    return false;
                }
        }

        return true;
    }

    private ProjectTypeValueValidator() {
    }
}
