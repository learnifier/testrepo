/*
 * (c) Dabox AB 2012 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.module.project.partdetails;

/**
 *
 * @author Jerker Klang <jerker.klang@dabox.se>
 */
class IdProjectModel {
    private final String title;
    private final String link;
    private final long projectId;
    private final int invited;
    private final int completed;

    public IdProjectModel(String title, String link, long projectId, int invited, int completed) {
        this.title = title;
        this.link = link;
        this.projectId = projectId;
        this.invited = invited;
        this.completed = completed;
    }

    public String getLink() {
        return link;
    }

    public String getTitle() {
        return title;
    }

    public long getProjectId() {
        return projectId;
    }

    public int getInvited() {
        return invited;
    }

    public int getCompleted() {
        return completed;
    }

    @Override
    public String toString() {
        return "IdProjectModel{" + "title=" + title + ", link=" + link + ", projectId=" + projectId +
                ", invited=" + invited + ", completed=" + completed + '}';
    }

}
