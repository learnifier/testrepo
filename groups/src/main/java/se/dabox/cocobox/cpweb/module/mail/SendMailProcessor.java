/*
 * (c) Dabox AB 2012 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.module.mail;

import java.io.Serializable;
import net.unixdeveloper.druwa.RequestCycle;
import se.dabox.cocobox.cpweb.state.SendMailSession;
import se.dabox.cocobox.cpweb.state.SendMailTemplate;

/**
 * Interface for classes that processes the send mail operation.
 * Note that implementations must be serializable.
 *
 * @author Jerker Klang (jerker.klang@dabox.se)
 */
public interface SendMailProcessor extends Serializable {

    /**
     * Called to perform the send mail operation. After this method has been called
     * the SendMailSession is unbound from the session and the completed RequestTargetGenerator
     * is invoked
     *
     * @param cycle The current RequestCycle
     * @param sms The current send mail session
     * @param smt The send mail template (which contains the user edited email).
     */
    public void processSendMail(RequestCycle cycle, SendMailSession sms,
            SendMailTemplate smt);

    /**
     * Returns the mail sender if the information is known. 
     * If the sender is not known <code>null</code> should be returned.
     * 
     * @param cycle The current request cycle.
     *
     * @return The information about the sender or <code>null</code> if the information is not
     *         known.
     */
    public MailSender getMailSender(RequestCycle cycle);
}
