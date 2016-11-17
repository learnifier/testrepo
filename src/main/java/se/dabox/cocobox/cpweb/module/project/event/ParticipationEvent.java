package se.dabox.cocobox.cpweb.module.project.event;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Date;

/**
 *
 * @author Magnus Andersson (magnus.andersson@learnifier.com)
 */
public class ParticipationEvent {
    private final ParticipationEventState state;
    private final Date updated;
    private final ParticipationEventChannel channel;
    private final String cid;

    public ParticipationEvent(@JsonProperty("cid") String cid,
                              @JsonProperty("state") ParticipationEventState state,
                              @JsonProperty("updated") Date updated,
                              @JsonProperty("channel") ParticipationEventChannel channel) {
        this.cid = cid;
        this.state = state;
        this.updated = updated;
        this.channel = channel;
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
