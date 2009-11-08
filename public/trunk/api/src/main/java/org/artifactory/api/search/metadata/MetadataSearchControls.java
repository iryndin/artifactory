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

package org.artifactory.api.search.metadata;

import org.apache.commons.lang.StringUtils;
import org.artifactory.api.search.SearchControlsBase;

/**
 * @author Yoav Landman
 */
public class MetadataSearchControls<T> extends SearchControlsBase {

    private String repoToSearch;
    private String metadataName;
    private String path;
    private String value;
    private boolean exactMatch;
    private Class<? extends T> metadataObjectClass;

    /**
     * Default constructor
     */
    public MetadataSearchControls() {
    }

    /**
     * Copy constructor
     *
     * @param metadataSearchControls Controls to copy
     */
    public MetadataSearchControls(MetadataSearchControls metadataSearchControls) {
        this.repoToSearch = metadataSearchControls.repoToSearch;
        this.metadataName = metadataSearchControls.metadataName;
        this.path = metadataSearchControls.path;
        this.value = metadataSearchControls.value;
        this.exactMatch = metadataSearchControls.exactMatch;
        //noinspection unchecked
        this.metadataObjectClass = metadataSearchControls.metadataObjectClass;
        setLimitSearchResults(metadataSearchControls.isLimitSearchResults());
    }

    public String getRepoToSearch() {
        return repoToSearch;
    }

    public void setRepoToSearch(String repoToSearch) {
        this.repoToSearch = repoToSearch;
    }

    public String getMetadataName() {
        return metadataName;
    }

    public void setMetadataName(String metadataName) {
        this.metadataName = metadataName;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public boolean isExactMatch() {
        return exactMatch;
    }

    public void setExactMatch(boolean exactMatch) {
        this.exactMatch = exactMatch;
    }

    public Class<? extends T> getMetadataObjectClass() {
        return metadataObjectClass;
    }

    public void setMetadataObjectClass(Class<T> metadataObjectClass) {
        this.metadataObjectClass = metadataObjectClass;
    }

    public boolean isEmpty() {
        return StringUtils.isEmpty(metadataName);
    }
}