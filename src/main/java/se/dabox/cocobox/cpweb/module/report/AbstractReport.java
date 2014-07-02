/*
 * (c) Dabox AB 2012 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.module.report;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.unixdeveloper.druwa.RequestCycle;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.dabox.cocobox.cpweb.module.report.lineenhancer.LineEnhancers;
import se.dabox.cocobox.cpweb.module.report.lineenhancer.StandardLineEnhancersFactory;
import se.dabox.service.client.CacheClients;
import se.dabox.service.tokenmanager.client.Token;
import se.dabox.service.tokenmanager.client.TokenManagerClient;

/**
 *
 * @author Jerker Klang <jerker.klang@dabox.se>
 */
public abstract class AbstractReport {
    private static final Logger LOGGER =
            LoggerFactory.getLogger(AbstractReport.class);

    private final RequestCycle cycle;
    private LineEnhancers enhancers;
    private ArrayList<Map<String, Object>> reportLines;
    private boolean newLine = true;
    private HashMap<String, Object> currentLine;

    public AbstractReport(RequestCycle cycle) {
        this.cycle = cycle;
    }

    public List<Map<String, Object>> generateReport(long accountId) {
        final List<Token> tokens = getTokens(accountId);

        enhancers =
                StandardLineEnhancersFactory.newInstance(cycle);

        try {
            reportLines = new ArrayList<>();

            beforeLoop();

            for (Token token : tokenFilter(tokens)) {
                processToken(token);
                newLine();
            }

            afterLoop();

        } finally {
            enhancers.close();
        }

        return reportLines;
    }

    protected TokenManagerClient getTokenManagerClient() {
        TokenManagerClient tmc = CacheClients.getClient(cycle, TokenManagerClient.class);

        return tmc;
    }

    private List<Token> getTokens(long accountId) {
        TokenManagerClient tmc = getTokenManagerClient();
        return tmc.listTokens(accountId);
    }

    protected void processToken(Token token) {
        Map<String, Object> l = getLine();
        l.put(LineConstants.AMOUNT, getTokenAmount(token));
        l.put(LineConstants.CREATED, token.getCreated());
        l.put(LineConstants.ORDER_SOURCE, token.getSource());
    }

    protected long getTokenAmount(final Token token) {
        return token.getUnbalancedAmount();
    }

    protected void addOrderAndLineId(Map<String, Object> line, Token token) {
        String sourceId = token.getSourceId();

        if (StringUtils.isEmpty(sourceId)) {
            return;
        }

        String[] splitted = sourceId.split(":");

        if (splitted.length == 0) {
            return;
        }

        if ("kbcolp".equals(splitted[0])) {
            line.put(LineConstants.ORDER_ID, Long.valueOf(splitted[2]));
            line.put(LineConstants.ORDER_LINE, Long.valueOf(splitted[3]));
            return;
        }

        LOGGER.warn("Unknown token source: {}", sourceId);
    }

    protected Map<String, Object> getLine() {
        if (newLine) {
            newLine = false;
            currentLine = new HashMap<String, Object>();
            reportLines.add(currentLine);
        }

        return currentLine;
    }

    protected void newLine() {
        newLine = true;
    }

    protected abstract Iterable<Token> tokenFilter(Iterable<Token> tokens);

    protected LineEnhancers getEnhancers() {
        return enhancers;
    }

    /**
     * Method called before token loop is started. The default implementation does nothing.
     */
    protected void beforeLoop() {
    }

    /**
     * Method called after token loop is complete. The default implementation does nothing.
     */
    protected void afterLoop() {
    }
    
}
