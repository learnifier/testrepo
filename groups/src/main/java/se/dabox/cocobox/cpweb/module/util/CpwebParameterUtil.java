/*
 * (c) Dabox AB 2013 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.module.util;

import net.unixdeveloper.druwa.RetargetException;
import net.unixdeveloper.druwa.request.ErrorCodeRequestTarget;

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

    /**
     * Converts a string to a long. If the string is null or not a valid long number
     * a RetargetException with an ErrorCodeRequestTarget(400) (bad request) is thrown.
     *
     * @param str A String
     * @return The parsed long number
     *
     * @throws RetargetException Thrown if the string was null or not a valid long number.
     */
    public static long stringToLongMandatory(String str) throws RetargetException {
        if (str != null) {
            try {
                return Long.valueOf(str);
            } catch(NumberFormatException nfe) {
                //Ignore
            }
        }

        throw new RetargetException(new ErrorCodeRequestTarget(400));
    }

    private CpwebParameterUtil() {
    }

}
