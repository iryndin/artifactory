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

package org.artifactory.api.webdav;

import org.artifactory.api.repo.Lock;
import org.artifactory.api.request.ArtifactoryResponse;
import org.artifactory.request.ArtifactoryRequest;
import org.artifactory.util.PathUtils;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * User: freds Date: Jul 27, 2008 Time: 9:26:56 PM
 */
public interface WebdavService {

    Set<String> WEBDAV_METHODS = new HashSet<String>() {{
        add("propfind");
        add("mkcol");
        add("move");
        add("delete");
        add("options");
    }};
    String WEBDAV_METHODS_LIST = PathUtils.collectionToDelimitedString(WEBDAV_METHODS);

    /**
     * PROPFIND Method.
     *
     * @throws java.io.IOException
     */
    @Lock(transactional = true)
    void handlePropfind(ArtifactoryRequest request, ArtifactoryResponse response) throws IOException;

    @Lock(transactional = true)
    void handleMkcol(ArtifactoryRequest request, ArtifactoryResponse response) throws IOException;

    @Lock(transactional = true)
    void handleDelete(ArtifactoryRequest request, ArtifactoryResponse response) throws IOException;

    void handleOptions(ArtifactoryResponse response) throws IOException;

    void handlePost(ArtifactoryRequest request, ArtifactoryResponse response);

    @Lock(transactional = true)
    void handleMove(ArtifactoryRequest request, ArtifactoryResponse response) throws IOException;
}
