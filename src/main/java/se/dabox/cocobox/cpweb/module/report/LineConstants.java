/*
 * (c) Dabox AB 2012 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.module.report;

/**
 *
 * @author Jerker Klang <jerker.klang@dabox.se>
 */
public class LineConstants {
    /**
     * A Long with the order handler order id
     *
     */
    public static final String ORDER_ID = "orderId";
    
    /**
     * A Long with the order handler order line id
     */
    public static final String ORDER_LINE = "orderLine";

    /**
     * A string with the line source. Current likely values are kbcolp, boweb or unknown)
     *
     */
    public static final String ORDER_SOURCE = "source";

    public static final String SOURCE_BOWEB = "boweb";
    public static final String SOURCE_KBCOLP = "kbcolp";
    public static final String SOURCE_UNKNOWN = "unknown";

    /**
     * A Long with the amount of the line
     *
     */
    public static final String AMOUNT = "amount";

    /**
     * A java.util.Date with the line's/token's create date
     */
    public static final String CREATED = "created";

    /**
     * The order number (user defined string)
     */
    public static final String ORDER_NO = "orderNo";

    /**
     * The product id (string) of the line product. 
     */
    public static final String PRODUCT = "productId";

    /**
     * The product id of the base product that the line came from. Might differ from
     * the actual product of the line.
     */
    public static final String BASEPRODUCT = "baseproduct";
    
}
