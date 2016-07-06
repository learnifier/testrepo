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
    };

    public abstract <E> E accept( SessionFieldVisitor<E> visitor );

    public interface SessionFieldVisitor<E> {
        E visitDescription();
        E visitVisibility();
    }
}
