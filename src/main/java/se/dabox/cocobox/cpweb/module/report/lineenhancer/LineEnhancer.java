/*
 * (c) Dabox AB 2012 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.module.report.lineenhancer;

import java.util.Map;

/**
 *
 * @author Jerker Klang <jerker.klang@dabox.se>
 */
public interface LineEnhancer {

    public void enhance(LineEnhancerContext context, Map<String,Object> line);
}
