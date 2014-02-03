/*
 * (c) Dabox AB 2012 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.module.project;

import java.io.Serializable;

/**
 *
 * @author Jerker Klang <jerker.klang@dabox.se>
 */
public class DeleteFailure implements Serializable {
    private static final long serialVersionUID = 1L;

    private final long participationId;

    private final String name;

    private final String reason;

    public DeleteFailure(long participationId, String name, String reason) {
        this.participationId = participationId;
        this.name = name;
        this.reason = reason;
    }

    public long getParticipationId() {
        return participationId;
    }

    public String getName() {
        return name;
    }

    public String getReason() {
        return reason;
    }

    @Override
    public String toString() {
        return "DeleteFailure{" + "participationId=" + participationId + ", name=" + name +
                ", reason=" + reason + '}';
    }
    
}
