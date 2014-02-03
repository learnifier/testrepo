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
public class UsedReport extends AbstractResCoReport {

    public UsedReport(RequestCycle cycle) {
        super(cycle);
    }

    @Override
    protected boolean isCandidate(Token token) {
        return TokenStatus.USED.equals(token.getStatus()) ||
                TokenStatus.RESERVED.equals(token.getStatus());
    }

}
