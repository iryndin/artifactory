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

package org.artifactory.search.archive;

/**
 * Holds entry names and paths for collection while search archive contents
 *
 * @author Noam Tenne
 */
public class ArchiveSearchEntry {

    private String entryName;
    private String entryPath;

    /**
     * Constructor
     *
     * @param entryName Name of entry
     * @param entryPath Path of entry
     */
    public ArchiveSearchEntry(String entryName, String entryPath) {
        this.entryName = entryName;
        this.entryPath = entryPath;
    }

    /**
     * Constructor
     *
     * @param entryPath Path of entry
     */
    public ArchiveSearchEntry(String entryPath) {
        this.entryPath = entryPath;
    }

    /**
     * Returns the entry name
     *
     * @return String - entry name
     */
    public String getEntryName() {
        return entryName;
    }

    /**
     * Returns the entry path
     *
     * @return String - entry path
     */
    public String getEntryPath() {
        return entryPath;
    }
}
