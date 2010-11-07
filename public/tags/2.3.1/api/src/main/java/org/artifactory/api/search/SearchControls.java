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

import org.artifactory.common.Info;

public interface SearchControls extends Info {
    /**
     * Determines if the search controls are considered "empty" so that a search should not even be attempted.
     *
     * @return
     */
    boolean isEmpty();

    /**
     * Indicates if the search results should be limited as in the system spec
     *
     * @return True if the search results should be limited
     */
    boolean isLimitSearchResults();

    /**
     * Resets the result limit indicator to it's default - true
     */
    void resetResultLimit();
}