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

package org.artifactory.api.search.archive;

import org.artifactory.api.fs.ItemInfo;
import org.artifactory.api.maven.MavenArtifactInfo;
import org.artifactory.api.search.artifact.ArtifactSearchResult;

/**
 * Holds archive content search result data. Inherits from SearchResult and adds two fields: -Entry name -Entry path
 *
 * @author Noam Tenne
 */
public class ArchiveSearchResult extends ArtifactSearchResult {

    private String entry;
    private String entryPath;

    /**
     * Default constructor
     *
     * @param itemInfo Item info
     * @param artifact Artifact info
     */
    public ArchiveSearchResult(ItemInfo itemInfo, MavenArtifactInfo artifact) {
        super(itemInfo, artifact);
    }

    /**
     * Extended constructor
     *
     * @param itemInfo  Item info
     * @param artifact  Artifact info
     * @param entry     Entry name
     * @param entryPath Entry path
     */
    public ArchiveSearchResult(ItemInfo itemInfo, MavenArtifactInfo artifact, String entry, String entryPath) {
        super(itemInfo, artifact);
        this.entry = entry;
        this.entryPath = entryPath;
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
     * Returns the entry path
     *
     * @return String - entry path
     */
    public String getEntryPath() {
        return entryPath;
    }
}
