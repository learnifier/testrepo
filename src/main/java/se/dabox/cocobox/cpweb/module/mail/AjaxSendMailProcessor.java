/*
 * (c) Dabox AB 2015 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.module.mail;

import net.unixdeveloper.druwa.RequestCycle;
import net.unixdeveloper.druwa.RequestTarget;
import se.dabox.cocobox.cpweb.state.SendMailSession;
import se.dabox.cocobox.cpweb.state.SendMailTemplate;

/**
 *
 * @author Jerker Klang (jerker.klang@dabox.se)
 */
public interface AjaxSendMailProcessor extends SendMailProcessor {

    /**
     * Called for SendMailSession processing when ajax mode is enabled. The
     * {@link #processSendMail(net.unixdeveloper.druwa.RequestCycle, se.dabox.cocobox.cpweb.state.SendMailSession, se.dabox.cocobox.cpweb.state.SendMailTemplate) }
     * method is never called when the ajax mode flag is set.
     *
     * @param cycle
     * @param sms
     * @param smt
     * @return
     */
    public RequestTarget processAjaxRequest(RequestCycle cycle, SendMailSession sms,
            SendMailTemplate smt);

}
