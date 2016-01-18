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

package org.artifactory.sapi.search;

import com.google.common.base.Strings;

/**
 * Date: 4/30/14 6:28 PM
 *
 * @author freds
 */
public enum VfsDateFieldName {
    LAST_MODIFIED("modified"),
    CREATED("created"),
    LAST_DOWNLOADED("last_downloaded"),
    LAST_REMOTE_DOWNLOADED("remote_last_downloaded", "last_downloaded");

    private final String propName;
    private final String propActualName;

    /**
     * If different tables have same column names,
     * we'd like to decorate propName with propNameAlias
     * which will be used as decoration reference to the
     * actual column name
     *
     * @param propNameAlias the alias to the actual propName
     * @param propName the actual propName
     */
    VfsDateFieldName(String propNameAlias, String propName) {
        this.propName = propNameAlias;
        this.propActualName = propName;
    }

    VfsDateFieldName(String propName) {
        this.propName = propName;
        this.propActualName = null;
    }

    public static VfsDateFieldName byPropertyName(String fieldName) {
        for (VfsDateFieldName dateFieldName : values()) {
            if (dateFieldName.propName.equals(fieldName)) {
                return dateFieldName;
            }
        }
        return null;
    }

    public String getPropName() {
        return propName;
    }

    /**
     * @return Actual propName (if propNameAlias is set)
     */
    public String getPropActualName() {
        return propActualName;
    }

    public boolean hasPropAlias() {
        return !Strings.isNullOrEmpty(propActualName);
    }
}
