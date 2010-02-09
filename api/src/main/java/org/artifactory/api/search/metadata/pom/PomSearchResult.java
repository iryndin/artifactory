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

package org.artifactory.api.search.metadata.pom;

import org.artifactory.api.fs.ItemInfo;
import org.artifactory.api.search.metadata.MetadataSearchResult;

/**
 * Holds POM search result data.
 *
 * @author Noam Tenne
 */
public class PomSearchResult extends MetadataSearchResult {

    /**
     * Default constructor
     *
     * @param itemInfo     Item info
     * @param metadataName Metadata name
     */
    public PomSearchResult(ItemInfo itemInfo, String metadataName) {
        super(itemInfo, metadataName, null);
    }

    @Override
    public String getName() {
        return getItemInfo().getName();
    }
}
