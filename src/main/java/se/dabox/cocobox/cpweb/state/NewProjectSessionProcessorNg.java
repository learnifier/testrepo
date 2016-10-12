/*
 * (c) Dabox AB 2012 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.state;

import net.unixdeveloper.druwa.RequestCycle;
import net.unixdeveloper.druwa.RequestTarget;
import se.dabox.cocobox.cpweb.formdata.project.MatListProjectDetailsForm;
import se.dabox.service.common.ccbc.project.OrgProject;
import se.dabox.service.common.ccbc.project.Project;

import java.io.Serializable;

/**
 *
 * @author Jerker Klang (jerker.klang@dabox.se)
 */
public interface NewProjectSessionProcessorNg extends Serializable {

    OrgProject processSession(RequestCycle cycle, NewProjectSessionNg nps,
                              MatListProjectDetailsForm matListDetails);

}
