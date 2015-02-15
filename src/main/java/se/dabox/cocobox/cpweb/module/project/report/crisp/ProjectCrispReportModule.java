/*
 * (c) Dabox AB 2013 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.module.project.report.crisp;

import net.unixdeveloper.druwa.RequestCycle;
import net.unixdeveloper.druwa.RequestTarget;
import net.unixdeveloper.druwa.annotation.WebAction;
import net.unixdeveloper.druwa.annotation.mount.WebModuleMountpoint;

/**
 *
 * @author Jerker Klang (jerker.klang@dabox.se)
 */
@WebModuleMountpoint("/project.crispreport")
public class ProjectCrispReportModule {

    @WebAction
    public RequestTarget onReport(RequestCycle cycle, String productId, String reportId) {
        return null;
    }
}
