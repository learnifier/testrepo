/*
 * (c) Dabox AB 2012 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.module.report.lineenhancer.impl;

import java.util.Map;
import net.unixdeveloper.druwa.RequestCycle;
import se.dabox.cocobox.cpweb.module.report.LineConstants;
import se.dabox.cocobox.cpweb.module.report.lineenhancer.LineEnhancer;
import se.dabox.cocobox.cpweb.module.report.lineenhancer.LineEnhancerContext;
import se.dabox.service.client.CacheClients;
import se.dabox.service.common.orderhandler.Order;
import se.dabox.service.common.orderhandler.OrderHandlerClient;
import se.dabox.service.common.orderhandler.OrderLine;

/**
 *
 * @author Jerker Klang (jerker.klang@dabox.se)
 */
public class OrderInfoEnhancer implements LineEnhancer {

    private final RequestCycle cycle;

    public OrderInfoEnhancer(RequestCycle cycle) {
        this.cycle = cycle;
    }

    @Override
    public void enhance(LineEnhancerContext context,
            Map<String, Object> line) {

        Object oOrderId = line.get(LineConstants.ORDER_ID);

        if (oOrderId == null) {
            line.put(LineConstants.ORDER_NO, "(Manual correction)");
            line.put(LineConstants.BASEPRODUCT, "");
            return;
        }

        Long orderId = Long.class.cast(oOrderId);

        OrderHandlerClient client = getOrderHandlerClient(cycle);
        Order order = client.getOrder(orderId);

        if (order != null) {
            line.put(LineConstants.ORDER_NO, order.getOrderNo());
            enhanceOrderLine(line, order);
        }
    }

    private OrderHandlerClient getOrderHandlerClient(RequestCycle cycle) {
        return CacheClients.getClient(cycle, OrderHandlerClient.class);
    }

    private void enhanceOrderLine(Map<String, Object> line, Order order) {
        Object oOrderLineId = line.get(LineConstants.ORDER_LINE);
        if (oOrderLineId == null) {
            return;
        }

        Long orderLineId = Long.class.cast(oOrderLineId);

        for (OrderLine orderLine : order.getLines()) {
            if (orderLine.getOrderLineId() == orderLineId) {
                line.put(LineConstants.BASEPRODUCT, orderLine.getProductId());
                return;
            }
        }

        line.put(LineConstants.BASEPRODUCT, "(Correction)");
    }
}
