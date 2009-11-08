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

package org.artifactory.api.search;

/**
 * A base implementation for all search control classes
 *
 * @author Noam Y. Tenne
 */
public abstract class SearchControlsBase implements SearchControls {

    private boolean limitSearchResults = true;

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
}
