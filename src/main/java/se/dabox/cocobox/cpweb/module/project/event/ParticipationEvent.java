package se.dabox.cocobox.cpweb.module.project.event;

import java.util.Date;

/**
 *
 * @author Magnus Andersson (magnus.andersson@learnifier.com)
 */
public class ParticipationEvent {
    private final ParticipationEventState state;
    private final Date updated;
    private final ParticipationEventChannel channel;

    public ParticipationEvent(ParticipationEventState state, Date updated, ParticipationEventChannel channel) {
        this.state = state;
        this.updated = updated;
        this.channel = channel;
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
                '}';
    }
}
