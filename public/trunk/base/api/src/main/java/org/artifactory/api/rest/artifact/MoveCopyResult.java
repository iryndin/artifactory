/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2012 JFrog Ltd.
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

package org.artifactory.api.rest.artifact;

import com.google.common.collect.Lists;
import org.artifactory.common.StatusEntry;
import org.artifactory.common.StatusEntryLevel;

import java.util.List;

/**
 * Wrapper class that holds the messages for a move or copy operation via the REST API. The response will be sent back
 * to the user in JSON format.
 *
 * @author Tomer Cohen
 */
@Deprecated
public class MoveCopyResult {

    public List<MoveCopyMessages> messages = Lists.newArrayList();

    public static class MoveCopyMessages {
        public String level;
        public String message;

        public MoveCopyMessages(StatusEntry statusEntry) {
            this.level = statusEntry.getLevel().name();
            this.message = statusEntry.getMessage();
        }

        public MoveCopyMessages(StatusEntryLevel level, String message) {
            this.level = level.name();
            this.message = message;
        }

        private MoveCopyMessages() {
        }

        @Override
        public String toString() {
            return level + ":" + message;
        }
    }
}