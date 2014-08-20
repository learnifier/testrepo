/*
 * (c) Dabox AB 2012 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.state;

import net.unixdeveloper.druwa.RequestCycle;

/**
 *
 * @author Jerker Klang (jerker.klang@dabox.se)
 */
public interface SendMailVerifier {

    public boolean verifySendMail(RequestCycle cycle, SendMailSession session,
            SendMailTemplate mailTemplate);

}
