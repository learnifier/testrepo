/*
 * (c) Dabox AB 2013 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.module.util;

/**
 * Utility class with (request) parameter related utils.
 *
 * @author Jerker Klang (jerker.klang@dabox.se)
 */
public class CpwebParameterUtil {

    /**
     * Returns the parsed string as a long value or {@code null} if the valid is invalid.
     *
     * @param value A String or {@code null}
     * @return The parsed long value or {@code null}
     */
    public static Long stringToLong(final String value) {
        if (value == null) {
            return null;
        }
        try {
            return Long.valueOf(value);
        } catch(NumberFormatException nfe) {
            return null;
        }
    }

    private CpwebParameterUtil() {
    }

}
