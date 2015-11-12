/*
 * (c) Dabox AB 2015 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.module.core;

import java.net.URI;
import net.unixdeveloper.druwa.linkencoder.LinkEncoder;
import net.unixdeveloper.druwa.linkencoder.LinkEncoderContext;
import net.unixdeveloper.druwa.util.UrlBuilder;

/**
 *
 * @author Jerker Klang (jerker.klang@dabox.se)
 */
class TpHybridModeLinkEncoder implements LinkEncoder {

    public TpHybridModeLinkEncoder() {
    }

    @Override
    public void encodeURL(LinkEncoderContext context) {
        URI uri = context.getURI();

        String query = uri.getRawQuery();

        //Test if it already is in place
        if (query != null && query.contains("_cpmode=")) {
            return;
        }

        if (!shouldEncode(context)) {
            return;
        }

        UrlBuilder ub = new UrlBuilder(uri.toASCIIString());
        ub.addParameter("_cpmode", "tp");

        context.setURI(URI.create(ub.toString()));
    }

    private boolean shouldEncode(LinkEncoderContext context) {
        String currentHost = context.getCycle().getRequest().getServerName();

        String uriHost = context.getURI().getHost();

        return uriHost == null || currentHost.equalsIgnoreCase(uriHost);
    }

}
