/*
 * (c) Dabox AB 2012 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.module.coursedesign;

/**
 *
 * @author Jerker Klang <jerker.klang@dabox.se>
 */
public final class DesignTechInfo {

    private DesignTechInfo() {
    }

    public static String createOrgTechInfo(long orgid) {
        return String.format("cpweb:o:%d", orgid);
    }

    public static String createLiveTechInfo(long projectId) {
        return String.format("cpweb:p:%d:live", projectId);
    }

    public static String createStageTechInfo(long projectId) {
        return String.format("cpweb:p:%d:stage", projectId);
    }

}
