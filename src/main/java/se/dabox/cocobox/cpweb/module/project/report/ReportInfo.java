/*
 * (c) Dabox AB 2013 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.module.project.report;

/**
 *
 * @author Jerker Klang (jerker.klang@dabox.se)
 */
class ReportInfo {
    private final String url;
    private final String title;
    private final boolean ownWindow;

    public ReportInfo(String url, String title, boolean ownWindow) {
        this.url = url;
        this.title = title;
        this.ownWindow = ownWindow;
    }

    public String getUrl() {
        return url;
    }

    public String getTitle() {
        return title;
    }

    public boolean isOwnWindow() {
        return ownWindow;
    }
    
}
