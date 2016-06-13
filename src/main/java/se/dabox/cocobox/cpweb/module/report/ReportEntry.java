/*
 * (c) Dabox AB 2015 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.module.report;

/**
 *
 * @author Jerker Klang (jerker.klang@learnifier.com)
 */
public class ReportEntry {
    private final String name;
    private final String link;

    public ReportEntry(String name, String link) {
        this.name = name;
        this.link = link;
    }

    public String getName() {
        return name;
    }

    public String getLink() {
        return link;
    }

    @Override
    public String toString() {
        return "ReportEntry{" + "name=" + name + ", link=" + link + '}';
    }
    
}
