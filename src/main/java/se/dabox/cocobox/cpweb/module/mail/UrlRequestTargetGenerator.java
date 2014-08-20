/*
 * (c) Dabox AB 2012 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.module.mail;

import net.unixdeveloper.druwa.RequestCycle;
import net.unixdeveloper.druwa.RequestTarget;
import net.unixdeveloper.druwa.request.RedirectUrlRequestTarget;

/**
 *
 * @author Jerker Klang (jerker.klang@dabox.se)
 */
public class UrlRequestTargetGenerator implements RequestTargetGenerator {
    private final String url;

    public UrlRequestTargetGenerator(String url) {
        this.url = url;
    }

    @Override
    public RequestTarget generateTarget(RequestCycle cycle) {
        return new RedirectUrlRequestTarget(url);
    }

}
