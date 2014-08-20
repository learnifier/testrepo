/*
 * (c) Dabox AB 2012 All Rights Reserved
 */
package se.dabox.cocobox.cpweb;

/**
 *
 * @author Jerker Klang (jerker.klang@dabox.se)
 */
public class CpAdminRoles {
    /**
     *
     * @deprecated Check for specific roles now instead
     */
    @Deprecated
    public static final String CLIENT_ADMIN = "clientadmin";

    /**
     * Special role value that means that the user has no client admin roles.    
     */
    public static final String NONE = "none";

    private CpAdminRoles() {
    }

}
