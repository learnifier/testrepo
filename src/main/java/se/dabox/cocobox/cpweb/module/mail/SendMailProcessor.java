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

    public void processSendMail(RequestCycle cycle, SendMailSession sms,
            SendMailTemplate smt);

    /**
     * Returns the mail sender if the information is known. 
     * If the sender is not known <code>null</code> is returned
     * 
     * @param cycle The current request cycle.
     *
     * @return The information about the sender or <code>null</code> if the information is not
     *         known.
     */
    public MailSender getMailSender(RequestCycle cycle);
}
