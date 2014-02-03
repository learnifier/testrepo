/*
 * (c) Dabox AB 2012 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.excel;

/**
 *
 * @author Jerker Klang <jerker.klang@dabox.se>
 */
public class RosterException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    /**
     * Creates a new instance of
     * <code>RosterException</code> without detail message.
     */
    public RosterException() {
    }

    /**
     * Constructs an instance of
     * <code>RosterException</code> with the specified detail message.
     *
     * @param msg the detail message.
     */
    public RosterException(String msg) {
        super(msg);
    }

    public RosterException(String message, Throwable cause) {
        super(message, cause);
    }
    
}
