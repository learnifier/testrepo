package se.dabox.cocobox.cpweb.module.project.event;

import java.util.Date;

/**
 *
 * @author Magnus Andersson (magnus.andersson@learnifier.com)
 */
public class ParticipationEvent {
    private ParticipationEventState state;
    private Date updated;
    private ParticipationEventChannel channel;
    private String cid;
    public ParticipationEvent() {

    }

    public ParticipationEvent(String cid, ParticipationEventState state, Date updated, ParticipationEventChannel channel) {
        this.cid = cid;
        this.state = state;
        this.updated = updated;
        this.channel = channel;
    }

    public void setState(ParticipationEventState state) {
        this.state = state;
    }

    public void setUpdated(Date updated) {
        this.updated = updated;
    }

    public void setChannel(ParticipationEventChannel channel) {
        this.channel = channel;
    }

    public void setCid(String cid) {
        this.cid = cid;
    }

    public String getCid() {
        return cid;
    }

    public ParticipationEventState getState() {
        return state;
    }

    public Date getUpdated() {
        return updated;
    }

    public ParticipationEventChannel getChannel() {
        return channel;
    }

    @Override
    public String toString() {
        return "ParticipationEvent{" +
                "state=" + state +
                ", updated=" + updated +
                ", channel=" + channel +
                ", cid='" + cid + '\'' +
                '}';
    }
}
