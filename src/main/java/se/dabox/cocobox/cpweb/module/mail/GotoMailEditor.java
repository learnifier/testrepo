/*
 * (c) Dabox AB 2012 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.module.mail;

import net.unixdeveloper.druwa.RequestCycle;
import se.dabox.cocobox.maileditor.initdata.MailEditorActivationUrl;
import se.dabox.cocobox.maileditor.initdata.MeInitData;

/**
 *
 * @author Jerker Klang <jerker.klang@dabox.se>
 */
public class GotoMailEditor {

    public static String process(RequestCycle cycle, MeInitData initData) {

        return MailEditorActivationUrl.generate(cycle, initData);
    }

    private GotoMailEditor() {
    }
}
