/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2010 JFrog Ltd.
 *
 * Artifactory is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Artifactory is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Artifactory.  If not, see <http://www.gnu.org/licenses/>.
 */

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
