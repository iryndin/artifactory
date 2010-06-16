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

package org.artifactory.repo.context;

import org.artifactory.api.md.Properties;
import org.artifactory.api.request.ArtifactoryRequest;
import org.artifactory.util.PathUtils;

/**
 * Implementation based on a download http request.
 *
 * @author Yossi Shaul
 */
public class DownloadRequestContext implements RequestContext {
    private final ArtifactoryRequest artifactoryRequest;

    public DownloadRequestContext(ArtifactoryRequest artifactoryRequest) {
        this.artifactoryRequest = artifactoryRequest;
    }

    public boolean isFromAnotherArtifactory() {
        return artifactoryRequest.isFromAnotherArtifactory();
    }

    public String getResourcePath() {
        String path = artifactoryRequest.getPath();
        return artifactoryRequest.isChecksum() ? PathUtils.stripExtension(path) : path;
    }

    public String getServletContextUrl() {
        return artifactoryRequest.getServletContextUrl();
    }

    public Properties getProperties() {
        return artifactoryRequest.getProperties();
    }

    public ArtifactoryRequest getRequest() {
        return artifactoryRequest;
    }
}
