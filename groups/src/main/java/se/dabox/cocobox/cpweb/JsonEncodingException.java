/*
 * (c) Dabox AB 2012 All Rights Reserved
 */
package se.dabox.cocobox.cpweb;

/**
 *
 * @author Jerker Klang (jerker.klang@dabox.se)
 */
public class JsonEncodingException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public JsonEncodingException(String message, Throwable cause) {
        super(message, cause);
    }
    
}
