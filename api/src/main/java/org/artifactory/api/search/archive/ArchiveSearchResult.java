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

package org.artifactory.api.search.archive;

import org.artifactory.api.search.artifact.ArtifactSearchResult;
import org.artifactory.fs.ItemInfo;

/**
 * Holds archive content search result data. Inherits from SearchResult and adds two fields: -Entry name -Entry path
 *
 * @author Noam Tenne
 */
public class ArchiveSearchResult extends ArtifactSearchResult {

    private final String entry;
    private final String entryPath;
    private final boolean isRealEntry;

    /**
     * Extended constructor
     *
     * @param itemInfo    Item info
     * @param entry       Entry name
     * @param entryPath   Entry path
     * @param isRealEntry Whether the entry is a real one, or just an informative missing entry message
     */
    public ArchiveSearchResult(ItemInfo itemInfo, String entry, String entryPath, boolean isRealEntry) {
        super(itemInfo);
        this.entry = entry;
        this.entryPath = entryPath;
        this.isRealEntry = isRealEntry;
    }

    /**
     * Returns the entry name
     *
     * @return String - entry name
     */
    public String getEntry() {
        return entry;
    }

    /**
     * Returns the entry name in all-lower-case.<p> Currently used by the grouping methods of the groupable table, so
     * that names which are identical in all but character cases will be grouped together.
     *
     * @return Lower-cased entry name
     */
    public String getLowerCaseEntry() {
        return entry.toLowerCase();
    }

    /**
     * Returns the entry path
     *
     * @return String - entry path
     */
    public String getEntryPath() {
        return entryPath;
    }

    public boolean isRealEntry() {
        return isRealEntry;
    }
}