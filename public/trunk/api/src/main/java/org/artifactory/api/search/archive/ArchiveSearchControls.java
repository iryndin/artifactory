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

import org.apache.commons.lang.StringUtils;
import org.artifactory.api.search.SearchControlsBase;

/**
 * Holds the archive content search parameters
 *
 * @author Noam Tenne
 */
public class ArchiveSearchControls extends SearchControlsBase {
    private String query;
    private boolean excludeInnerClasses;
    private boolean shouldCalcEntries;

    /**
     * Default constructor
     */
    public ArchiveSearchControls() {
        excludeInnerClasses = false;
        shouldCalcEntries = true;
    }

    /**
     * Copy constructor
     *
     * @param archiveSearchControls Controls to copy
     */
    public ArchiveSearchControls(ArchiveSearchControls archiveSearchControls) {
        this.query = archiveSearchControls.query;
        this.selectedRepoForSearch = archiveSearchControls.selectedRepoForSearch;
        setLimitSearchResults(archiveSearchControls.isLimitSearchResults());
        this.excludeInnerClasses = archiveSearchControls.excludeInnerClasses;
        this.shouldCalcEntries = archiveSearchControls.shouldCalcEntries;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public boolean isEmpty() {
        return StringUtils.isEmpty(query);
    }

    public boolean isExcludeInnerClasses() {
        return excludeInnerClasses;
    }

    public void setExcludeInnerClasses(boolean excludeInnerClasses) {
        this.excludeInnerClasses = excludeInnerClasses;
    }

    public boolean shouldCalcEntries() {
        return shouldCalcEntries;
    }

    public void setShouldCalcEntries(boolean shouldCalcEntries) {
        this.shouldCalcEntries = shouldCalcEntries;
    }
}