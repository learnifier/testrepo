/*
 * (c) Dabox AB 2012 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.module.report.lineenhancer;

import net.unixdeveloper.druwa.RequestCycle;
import se.dabox.cocobox.cpweb.module.report.lineenhancer.impl.OrderInfoEnhancer;

/**
 *
 * @author Jerker Klang (jerker.klang@dabox.se)
 */
public class StandardLineEnhancersFactory {

    public static LineEnhancers newInstance(final RequestCycle cycle) {
        final LineEnhancers enhancers = new LineEnhancers();
        enhancers.addEnhancer(new OrderInfoEnhancer(cycle));

        return enhancers;
    }
}
