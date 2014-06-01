/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2014 JFrog Ltd.
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

package org.artifactory.api.rest.search.common;

/**
 * Date: 5/11/14 3:46 PM
 * <p/>
 * public String created;
 * public String createdBy;
 * public String lastModified;
 * public String modifiedBy;
 * public String lastUpdated;
 * public Map<String, String[]> properties;
 * public String remoteUrl;
 * public String mimeType;
 * public String size;
 * public Checksums checksums;
 * public Checksums originalChecksums;
 * public List<DirItem> children;
 *
 * @author freds
 */
public enum RestResultFieldName {
    // Repo, path, type mandatory
    LAST_MODIFIED("lastModified"), CREATED("created"), LAST_DOWNLOADED("lastDownloaded");

    private String fieldName;

    RestResultFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public static RestResultFieldName byFieldName(String fieldName) {
        for (RestResultFieldName dateFieldName : values()) {
            if (dateFieldName.fieldName.equals(fieldName)) {
                return dateFieldName;
            }
        }
        return null;
    }
}
