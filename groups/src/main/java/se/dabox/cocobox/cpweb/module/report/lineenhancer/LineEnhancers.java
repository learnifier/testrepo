/*
 * (c) Dabox AB 2012 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.module.report.lineenhancer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Jerker Klang (jerker.klang@dabox.se)
 */
public class LineEnhancers {
    private LineEnhancerContext ctx = new StandardLineEnhancerContext();

    private List<LineEnhancer> enhancers = new ArrayList<LineEnhancer>();

    public void addEnhancer(LineEnhancer lineEnhancer) {
        checkOpen();
        
        if (lineEnhancer == null) {
            throw new IllegalArgumentException("lineEnhancer is null");
        }
        
        enhancers.add(lineEnhancer);
    }

    public void enhance(Map<String,Object> line) {
        checkOpen();

        //Line start

        for (LineEnhancer lineEnhancer : enhancers) {
            lineEnhancer.enhance(ctx, line);
        }

        //Line end
    }

    public void close() {
        ctx = null;
    }

    private void checkOpen() throws IllegalStateException {
        if (ctx == null) {
            throw new IllegalStateException("This instance is closed");
        }
    }

}
