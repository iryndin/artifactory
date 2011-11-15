/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2011 JFrog Ltd.
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

package org.artifactory.traffic.entry;

import org.artifactory.traffic.TrafficAction;

/**
 * Artifact upload traffic entry
 *
 * @author Noam Tenne
 */
public class UploadEntry extends TransferEntry {

    /**
     * Parses the given textual entry and sets the object fields accordingly
     *
     * @param entry Textual entry
     */
    public UploadEntry(String entry) {
        super(entry);
    }

    /**
     * Sets the given entry data in the relevant fields
     *
     * @param repoPath      Requested artifact repo path
     * @param contentLength Requested artifact size
     */
    public UploadEntry(String repoPath, long contentLength, long duration) {
        super(repoPath, contentLength, duration);
    }

    @Override
    public TrafficAction getAction() {
        return TrafficAction.UPLOAD;
    }
}