/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2011 JFrog Ltd.
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

package org.artifactory.request;

import org.artifactory.api.md.PropertiesImpl;
import org.artifactory.api.request.InternalArtifactoryRequest;
import org.artifactory.md.Properties;
import org.artifactory.repo.RepoPath;

/**
 * Dummy implementation of the request context. It doesn't contain the original request. Used in internal requests and
 * for testing.
 *
 * @author Yossi Shaul
 */
public class NullRequestContext implements RequestContext {

    private final ArtifactoryRequest request;

    public NullRequestContext(RepoPath repoPath) {
        this.request = new InternalArtifactoryRequest(repoPath);
    }

    public boolean isFromAnotherArtifactory() {
        return false;
    }

    public String getResourcePath() {
        final RepoPath repoPath = request.getRepoPath();
        return repoPath.getPath();
    }

    public String getServletContextUrl() {
        return "";
    }

    public Properties getProperties() {
        return new PropertiesImpl();
    }

    public ArtifactoryRequest getRequest() {
        return request;
    }
}
