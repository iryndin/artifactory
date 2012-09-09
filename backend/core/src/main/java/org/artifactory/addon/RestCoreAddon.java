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

package org.artifactory.addon;

import org.artifactory.api.request.ArtifactoryResponse;
import org.artifactory.repo.LocalRepo;
import org.artifactory.request.ArtifactoryRequest;

import java.io.IOException;

/**
 * Exposed REST addon features to the core package.
 *
 * @author Shay Yaakov
 */
public interface RestCoreAddon extends Addon {

    /**
     * Deploy an archive containing multiple artifacts and explodes it at the specified destination (taken from the request path)
     * while maintaining the archive's file structure.
     *
     * @param request  The original request
     * @param response The response to send back
     * @param repo     A local repo to explode the archive to
     */
    void deployArchiveBundle(ArtifactoryRequest request, ArtifactoryResponse response, LocalRepo repo)
            throws IOException;
}
