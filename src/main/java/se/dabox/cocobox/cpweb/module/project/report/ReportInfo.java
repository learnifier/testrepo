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

    public ReportInfo(String url, String title) {
        this.url = url;
        this.title = title;
    }

    public String getUrl() {
        return url;
    }

    public String getTitle() {
        return title;
    }

}
