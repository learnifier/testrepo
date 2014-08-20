/*
 * (c) Dabox AB 2013 All Rights Reserved
 */

package se.dabox.cocobox.cpweb.model.project.task;

import java.io.Serializable;
import java.util.Locale;
import net.unixdeveloper.druwa.RequestCycle;
import se.dabox.cocobox.cpweb.module.mail.SendMailProcessor;
import se.dabox.cocobox.cpweb.state.SendMailTemplate;
import se.dabox.cocosite.login.CocositeUserHelper;
import se.dabox.service.common.mailsender.pmt.PortableMailTemplate;

/**
 *
 * @author Jerker Klang (jerker.klang@dabox.se)
 */
abstract class AbstractMailSendMailProcessor implements SendMailProcessor, Serializable {
    private static final long serialVersionUID = 1L;

    protected PortableMailTemplate createPortableMailTemplate(RequestCycle cycle, SendMailTemplate smt) {
        Locale locale = CocositeUserHelper.getUserLocale(cycle);

        return PortableMailTemplate.createMinimal(locale, smt.getSubject(), smt.getBody());
    }

}
