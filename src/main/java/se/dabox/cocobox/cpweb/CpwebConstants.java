/*
 * (c) Dabox AB 2012 All Rights Reserved
 */
package se.dabox.cocobox.cpweb;

/**
 *
 * @author Jerker Klang (jerker.klang@dabox.se)
 */
public final class CpwebConstants {

    public static final String ADMIN_WELCOME_MAIL_HINT = "mail.admin.welcome";
    public static final String ADMIN_REGISTRATION_MAIL_HINT = "mail.admin.registration";

    public static final String PRJADMIN_WELCOME_MAIL_HINT = "mail.projectadmin.welcome";
    public static final String PRJADMIN_REGISTRATION_MAIL_HINT = "mail.projectadmin.registration";
    public static final String PRJADMIN_ROLE_ASSIGNED_HINT = "cpweb.project.roleassigned";

    public static final String MAILEDITOR_SKIN = "CPAuth3";

    public static final String CREDIT_ALLOC_FLASH = "creditAllocationFailures";
    public static final String DELETE_FAILURE_FLASH = "projectDeleteFailures";
    public static final String MISSING_PRODUCTS_FLASH = "missingProducts";
    /**
     * Constants for flash attribute that is a {@code Set<Long>} containing participation ids
     * for the last project sendout.
     */
    public static final String SEND_PARTICIPATIONS_FLASH = "sendParticipationsFlash";

    public static final String SKIN_MODAL_MAIL = "bootstrap_barebone";

    /**
     * Thumbnail to use when session doen't have one set. The path is relative from
     * the cdn
     *
     */
    public static final String SESSION_DEFAULT_THUMBNAIL = "/cocobox/img/catalog/bag-and-hands-800.jpg";

    private CpwebConstants() {
    }

}
