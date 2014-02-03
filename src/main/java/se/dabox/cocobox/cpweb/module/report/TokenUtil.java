/*
 * (c) Dabox AB 2012 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.module.report;

import se.dabox.service.tokenmanager.client.Token;

/**
 *
 * @author Jerker Klang <jerker.klang@dabox.se>
 */
class TokenUtil {

    static boolean isOrderToken(Token token) {
        return "kbcolp".equals(token.getSource());
    }

    static boolean isProjectToken(Token token) {
        String sourceId = token.getSourceId();
        return sourceId != null && sourceId.startsWith("ccbc:p:");
    }
    
    static boolean isDeeplinkToken(Token token) {
        String sourceId = token.getSourceId();
        return sourceId != null && sourceId.startsWith("ccbc:l:");
    }
}
