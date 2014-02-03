/*
 * (c) Dabox AB 2012 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.module.report;

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
public class ExpiredReport extends AbstractReport {
    private static final Logger LOGGER =
            LoggerFactory.getLogger(ExpiredReport.class);

    public ExpiredReport(RequestCycle cycle) {
        super(cycle);
    }

    @Override
    protected void processToken(Token token) {
        LOGGER.debug("Token: {}", token);
        super.processToken(token);
        Map<String, Object> line = getLine();
        addOrderAndLineId(line, token);
        getEnhancers().enhance(line);
    }

    @Override
    protected Iterable<Token> tokenFilter(Iterable<Token> tokens) {
        return TokenFilter.unbalancedGreaterThan(0).filter(
                TokenFilter.expired().filter(
                TokenFilter.greaterThan(0).filter(tokens)));
    }

}
