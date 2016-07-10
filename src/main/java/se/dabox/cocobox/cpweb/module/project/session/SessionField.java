package se.dabox.cocobox.cpweb.module.project.session;

/**
 *
 * @author Magnus Andersson (magnus.andersson@learnifier.com)
 */
public enum SessionField {
    description {
        public <E> E accept( SessionFieldVisitor<E> visitor ) {
            return visitor.visitDescription();
        }
    },
    visibility {
        public <E> E accept( SessionFieldVisitor<E> visitor ) {
            return visitor.visitVisibility();
        }
    },
    enrollmentMode {
        public <E> E accept( SessionFieldVisitor<E> visitor ) {
            return visitor.visitEnrollmentMode();
        }
    },
    enrollmentFromDate {
        public <E> E accept( SessionFieldVisitor<E> visitor ) {
            return visitor.visitEnrollmentFromDate();
        }
    },
    enrollmentToDate {
        public <E> E accept( SessionFieldVisitor<E> visitor ) {
            return visitor.visitEnrollmentToDate();
        }
    },
    disenrollmentMode {
        public <E> E accept( SessionFieldVisitor<E> visitor ) {
            return visitor.visitDisenrollmentMode();
        }
    },
    disenrollmentFromDate {
        public <E> E accept( SessionFieldVisitor<E> visitor ) {
            return visitor.visitDisenrollmentFromDate();
        }
    },
    disenrollmentToDate {
        public <E> E accept( SessionFieldVisitor<E> visitor ) {
            return visitor.visitDisenrollmentToDate();
        }
    },
    participationEnabled {
        public <E> E accept( SessionFieldVisitor<E> visitor ) {
            return visitor.visitParticipationEnabled();
        }
    },
    participationShowName {
        public <E> E accept( SessionFieldVisitor<E> visitor ) {
            return visitor.visitParticipationShowName();
        }
    },
    participationShowThumbnail {
        public <E> E accept( SessionFieldVisitor<E> visitor ) {
            return visitor.visitParticipationShowThumbnail();
        }
    };

    public abstract <E> E accept( SessionFieldVisitor<E> visitor );

    public interface SessionFieldVisitor<E> {
        E visitDescription();
        E visitVisibility();
        E visitEnrollmentMode();
        E visitEnrollmentFromDate();
        E visitEnrollmentToDate();
        E visitDisenrollmentMode();
        E visitDisenrollmentFromDate();
        E visitDisenrollmentToDate();
        E visitParticipationEnabled();
        E visitParticipationShowName();
        E visitParticipationShowThumbnail();
    }
}
