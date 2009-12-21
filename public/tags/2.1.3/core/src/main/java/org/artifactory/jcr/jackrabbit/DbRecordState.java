package org.artifactory.jcr.jackrabbit;

import org.artifactory.concurrent.State;

/**
 * @author freds
 */
enum DbRecordState implements State {
    NEW,
    // In DB with the 3 state of the GC scanner
    IN_DB_FOUND, IN_DB_USED, IN_DB_MARK_FOR_DELETION,
    DELETED, IN_ERROR;

    public boolean canTransitionTo(State newState) {
        if (newState == IN_ERROR) {
            // Always can go to error
            return true;
        }
        switch (this) {
            case NEW:
                return (newState == IN_DB_FOUND) || (newState == IN_DB_USED) || (newState == DELETED);
            case IN_DB_FOUND:
                return (newState == DELETED) || (newState == IN_DB_USED) || (newState == IN_DB_MARK_FOR_DELETION);
            case IN_DB_USED:
                return (newState == IN_DB_FOUND);
            case IN_DB_MARK_FOR_DELETION:
                return (newState == DELETED);
            case DELETED:
                return (newState == NEW);
            case IN_ERROR:
                return (newState == NEW) || (newState == IN_DB_USED);
        }
        throw new IllegalStateException("Could not be reached");
    }

}
