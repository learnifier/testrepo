package se.dabox.cocobox.cpweb.module.project.event;

// TODO: This will be moved, probably to ccbc-client.


/**
 *
 * @author Magnus Andersson (magnus.andersson@learnifier.com)
 */
public enum ParticipationEventState {
    CONFIRMED("CONFIRMED"),
    CANCELLED("CANCELLED"),
    TENTATIVE("TENTATIVE");

    private final String state;

    ParticipationEventState(String state) {
        this.state = state;
    }
}
