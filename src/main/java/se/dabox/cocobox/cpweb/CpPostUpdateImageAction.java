/*
 * (c) Dabox AB 2012 All Rights Reserved
 */
package se.dabox.cocobox.cpweb;

import net.unixdeveloper.druwa.RequestCycle;
import net.unixdeveloper.druwa.RequestTarget;
import net.unixdeveloper.druwa.request.RedirectUrlRequestTarget;
import se.dabox.cocosite.module.account.picture.PostUpdateImageAction;

/**
 *
 * @author Jerker Klang <jerker.klang@dabox.se>
 */
class CpPostUpdateImageAction implements PostUpdateImageAction {

    @Override
    public RequestTarget afterUploadImage(RequestCycle cycle) {
        int pCount = cycle.getRequest().getPathElementCount();
        String lastPath = cycle.getRequest().getPathElement(pCount-1);
        return new RedirectUrlRequestTarget(NavigationUtil.toAccountSettingsUrl(cycle, lastPath));
    }

}
