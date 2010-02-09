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

package org.artifactory.resource;

import org.artifactory.api.fs.MetadataInfo;

/**
 * String resource is an already resolved metadata resource (no need to get it's handle for its content) that holds the
 * resource content in a string.
 *
 * @author Yossi Shaul
 */
public class StringResource extends MetadataResource {

    private String content;

    public StringResource(MetadataInfo info, String content) {
        super(info);
        this.content = content;
    }

    public String getContent() {
        return content;
    }
}
