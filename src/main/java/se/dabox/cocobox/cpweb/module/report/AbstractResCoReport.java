/*
 * (c) Dabox AB 2012 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.module.report;

import java.util.HashMap;
import java.util.Map;
import net.unixdeveloper.druwa.RequestCycle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.dabox.service.tokenmanager.client.Token;
import se.dabox.service.tokenmanager.client.TokenFilter;

/**
 *
 * @author Jerker Klang <jerker.klang@dabox.se>
 */
public abstract class AbstractResCoReport extends AbstractReport {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(AvailableReport.class);
    private Map<String, TokenAmount> amountMap = new HashMap<String, TokenAmount>();

    public AbstractResCoReport(RequestCycle cycle) {
        super(cycle);
    }

    @Override
    protected void processToken(Token token) {
        LOGGER.debug("Token: {}", token);

        if (TokenUtil.isOrderToken(token)) {
            return;
        }

        if (!isCandidate(token)) {
            return;
        }

        String source = extractSource(token);
        getTokenAmount(source).addAmount(Math.abs(token.getAmount()));
    }

    @Override
    protected Iterable<Token> tokenFilter(Iterable<Token> tokens) {
        return TokenFilter.lessThan(0).filter(tokens);
    }

    private TokenAmount getTokenAmount(String sourceId) {
        TokenAmount ta = amountMap.get(sourceId);
        if (ta == null) {
            ta = new TokenAmount(sourceId);
            amountMap.put(sourceId, ta);
        }

        return ta;
    }

    @Override
    protected void afterLoop() {
        super.afterLoop();
        LOGGER.debug("Result: {}", amountMap);

        for (TokenAmount tokenAmount : amountMap.values()) {
            processAmount(tokenAmount);
        }
    }

    private void processAmount(TokenAmount tokenAmount) {
        String name = tokenAmount.getName();

        String[] split = name.split(":");

        if (split[1].equals("p")) {
            processProjectReservation(tokenAmount, split);
        } else if (split[1].equals("l")) {
            processDeeplinkReservation(tokenAmount, split);
        } else {
            LOGGER.warn("Unknown reservation source: {}", name);
            processUnknownReservation(tokenAmount, split);
        }
    }

    private void processProjectReservation(TokenAmount tokenAmount, String[] split) {
        String projectId = split[2];

        Map<String, Object> line = getLine();

        line.put(LineConstants.AMOUNT, tokenAmount.getAmount());

        line.put("targetType", "project");
        line.put("projectId", projectId);

        newLine();
    }

    private String extractSource(Token token) {
        if (TokenUtil.isProjectToken(token)) {
            String[] split = token.getSourceId().split(":");
            return "ccbc:p:"+split[2];
        } else if (TokenUtil.isDeeplinkToken(token)) {
            String[] split = token.getSourceId().split(":");
            return "ccbc:l:"+split[2];
        } else {
            LOGGER.warn("Unknown token source: {}", token);
            return "unknown:unknown:unknown";
        }

        
    }

    private void processDeeplinkReservation(TokenAmount tokenAmount, String[] split) {
        String deeplinkId = split[2];

        Map<String, Object> line = getLine();

        line.put(LineConstants.AMOUNT, tokenAmount.getAmount());

        line.put("targetType", "deeplink");
        line.put("deeplinkId", deeplinkId);

        newLine();
    }

    protected abstract boolean isCandidate(Token token);

    private void processUnknownReservation(TokenAmount tokenAmount, String[] split) {
        Map<String, Object> line = getLine();

        line.put(LineConstants.AMOUNT, tokenAmount.getAmount());

        line.put("targetType", "unknown");

        newLine();
    }
}
