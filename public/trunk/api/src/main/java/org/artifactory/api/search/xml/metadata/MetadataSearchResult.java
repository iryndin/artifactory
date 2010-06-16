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

package org.artifactory.api.search.xml.metadata;

import org.artifactory.api.fs.ItemInfo;
import org.artifactory.api.mime.NamingUtils;
import org.artifactory.api.search.SearchResultBase;

/**
 * @author Yoav Landman
 */
public class MetadataSearchResult extends SearchResultBase {

    private String metadataName;
    private Object metadataObject;

    /**
     * Default constructor
     *
     * @param itemInfo     Item info
     * @param metadataName Metadata name
     */
    public MetadataSearchResult(ItemInfo itemInfo, String metadataName, Object metadataObject) {
        super(itemInfo);
        this.metadataName = metadataName;
        this.metadataObject = metadataObject;
    }

    @Override
    public String getName() {
        String name = NamingUtils.getMetadataPath(super.getName(), metadataName);
        return name;
    }

    public String getMetadataName() {
        return metadataName;
    }

    public Object getMetadataObject() {
        return metadataObject;
    }
}