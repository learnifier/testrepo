/*
 * (c) Dabox AB 2012 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.state;

import java.io.Serializable;
import net.unixdeveloper.druwa.RequestCycle;
import net.unixdeveloper.druwa.RequestTarget;
import se.dabox.cocobox.cpweb.formdata.project.MatListProjectDetailsForm;

/**
 *
 * @author Jerker Klang <jerker.klang@dabox.se>
 */
public interface NewProjectSessionProcessor extends Serializable {

    public RequestTarget processSession(RequestCycle cycle, NewProjectSession nps,
            MatListProjectDetailsForm matListDetails);

}
