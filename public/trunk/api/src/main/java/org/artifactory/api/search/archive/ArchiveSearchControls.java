/*
 * This file is part of Artifactory.
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
    private boolean exactMatch;
    private boolean searchAllTypes;
    private boolean shouldCalcEntries;

    /**
     * Default constructor
     */
    public ArchiveSearchControls() {
        shouldCalcEntries = true;
        exactMatch = true;
    }

    /**
     * Copy contsructor
     *
     * @param archiveSearchControls Controls to copy
     */
    public ArchiveSearchControls(ArchiveSearchControls archiveSearchControls) {
        this.query = archiveSearchControls.query;
        this.exactMatch = archiveSearchControls.exactMatch;
        this.searchAllTypes = archiveSearchControls.searchAllTypes;
        setLimitSearchResults(archiveSearchControls.isLimitSearchResults());
        this.shouldCalcEntries = archiveSearchControls.shouldCalcEntries;
    }

    public boolean isExactMatch() {
        return exactMatch;
    }

    public void setExactMatch(boolean exactMatch) {
        this.exactMatch = exactMatch;
    }

    public void setSearchAllTypes(boolean searchAllTypes) {
        this.searchAllTypes = searchAllTypes;
    }

    public boolean isSearchAllTypes() {
        return searchAllTypes;
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

    public boolean shouldCalcEntries() {
        return shouldCalcEntries;
    }

    public void setShouldCalcEntries(boolean shouldCalcEntries) {
        this.shouldCalcEntries = shouldCalcEntries;
    }
}