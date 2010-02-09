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

package org.artifactory.api.search;

import com.google.common.collect.Lists;

import java.util.List;

/**
 * A base implementation for all search control classes
 *
 * @author Noam Y. Tenne
 */
public abstract class SearchControlsBase implements SearchControls {

    private boolean limitSearchResults = true;
    protected List<String> selectedRepoForSearch;

    public List<String> getSelectedRepoForSearch() {
        return selectedRepoForSearch;
    }

    public void setSelectedRepoForSearch(List<String> selectedRepoForSearch) {
        this.selectedRepoForSearch = selectedRepoForSearch;
    }

    public boolean isLimitSearchResults() {
        return limitSearchResults;
    }

    /**
     * Sets the search result limit indicator
     *
     * @param limitSearchResults True if the search results should be limited
     */
    public void setLimitSearchResults(boolean limitSearchResults) {
        this.limitSearchResults = limitSearchResults;
    }

    public void resetResultLimit() {
        limitSearchResults = true;
    }

    public boolean isSpecificRepoSearch() {
        return selectedRepoForSearch != null && !selectedRepoForSearch.isEmpty();
    }

    public void addRepoToSearch(String repoKey) {
        if (selectedRepoForSearch == null) {
            selectedRepoForSearch = Lists.newArrayList();
        }
        selectedRepoForSearch.add(repoKey);
    }
}
