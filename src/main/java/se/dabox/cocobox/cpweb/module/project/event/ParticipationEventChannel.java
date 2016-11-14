package se.dabox.cocobox.cpweb.module.project.event;


/**
 * Enum of who changed the participation
 *
 * @author Magnus Andersson (magnus.andersson@learnifier.com)
 */
public enum ParticipationEventChannel {
    CPWEB("CPWEB"),
    UPWEB("UPWEB"),
    ICAL("ICAL");

    private final String channel;

    ParticipationEventChannel(String channel) {
        this.channel = channel;
    }
}
