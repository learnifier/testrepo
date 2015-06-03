/*
 * (c) Dabox AB 2015 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.module;

import net.unixdeveloper.druwa.RequestCycle;
import net.unixdeveloper.druwa.RequestTarget;
import net.unixdeveloper.druwa.annotation.WebAction;
import net.unixdeveloper.druwa.annotation.mount.WebModuleMountpoint;
import se.dabox.cocobox.cpweb.NavigationUtil;
import se.dabox.cocobox.cpweb.module.core.AbstractModule;

/**
 * Module used for various goto operations. The gotos are high level actions that this module can
 * determine in detail where the actual target is.
 *
 * @author Jerker Klang (jerker.klang@dabox.se)
 */
@WebModuleMountpoint("/goto")
public class CpGotoModule extends AbstractModule {
    //Do not extend from an auth class. We wan't to be able to run this without security

    @WebAction
    public RequestTarget onProject(RequestCycle cycle, String strProjectId) {
        return NavigationUtil.toProjectPage(Long.valueOf(strProjectId));
    }
}
