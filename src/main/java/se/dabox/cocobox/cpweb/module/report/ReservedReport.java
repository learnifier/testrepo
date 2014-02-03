/*
 * (c) Dabox AB 2012 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.module.report;

import net.unixdeveloper.druwa.RequestCycle;
import se.dabox.service.tokenmanager.client.Token;
import se.dabox.service.tokenmanager.client.TokenStatus;

/**
 *
 * @author Jerker Klang <jerker.klang@dabox.se>
 */
public class ReservedReport extends AbstractResCoReport {

    public ReservedReport(RequestCycle cycle) {
        super(cycle);
    }

    @Override
    protected boolean isCandidate(Token token) {
        return TokenStatus.RESERVED.equals(token.getStatus());
    }

}
