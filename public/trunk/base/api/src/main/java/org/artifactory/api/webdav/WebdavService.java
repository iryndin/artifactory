/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2012 JFrog Ltd.
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

package org.artifactory.api.webdav;

import com.google.common.collect.Sets;
import org.artifactory.api.request.ArtifactoryResponse;
import org.artifactory.request.ArtifactoryRequest;
import org.artifactory.sapi.common.Lock;
import org.artifactory.util.PathUtils;

import java.io.IOException;
import java.util.Set;

/**
 * User: freds Date: Jul 27, 2008 Time: 9:26:56 PM
 */
public interface WebdavService {

    /**
     * Supported web dav methods. (post method is not supported)
     */
    Set<String> WEBDAV_METHODS = Sets.newHashSet("propfind", "mkcol", "move", "delete", "options"/*, "post"*/);

    /**
     * The supported webdav methods as a comma separated list
     */
    String WEBDAV_METHODS_LIST = PathUtils.collectionToDelimitedString(WEBDAV_METHODS);

    /**
     * PROPFIND Method.
     */
    void handlePropfind(ArtifactoryRequest request, ArtifactoryResponse response) throws IOException;

    @Lock
    void handleMkcol(ArtifactoryRequest request, ArtifactoryResponse response) throws IOException;

    void handleDelete(ArtifactoryRequest request, ArtifactoryResponse response) throws IOException;

    void handleOptions(ArtifactoryResponse response) throws IOException;

    void handlePost(ArtifactoryRequest request, ArtifactoryResponse response);

    void handleMove(ArtifactoryRequest request, ArtifactoryResponse response) throws IOException;
}
