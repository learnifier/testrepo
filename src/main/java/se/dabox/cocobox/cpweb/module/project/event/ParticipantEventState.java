package se.dabox.cocobox.cpweb.module.project.event;

// TODO: This will be moved, probably to ccbc-client.


public enum ParticipantEventState {
        ACCEPTED("accepted"),
        DECLINED("declined"),
        UNKNOWN("unknown");

    private final String state;

    ParticipantEventState(String state) {
        this.state = state;
    }
}
