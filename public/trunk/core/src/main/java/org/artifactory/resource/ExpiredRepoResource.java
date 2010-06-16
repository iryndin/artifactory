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

import org.artifactory.api.fs.RepoResourceInfo;
import org.artifactory.api.repo.RepoPath;

public class ExpiredRepoResource implements RepoResource {

    private RepoResource wrappedResource;

    public ExpiredRepoResource(RepoResource wrappedResource) {
        this.wrappedResource = wrappedResource;
    }

    public RepoPath getRepoPath() {
        return wrappedResource.getRepoPath();
    }

    public RepoPath getResponseRepoPath() {
        return wrappedResource.getResponseRepoPath();
    }

    public void setResponseRepoPath(RepoPath responsePath) {
        wrappedResource.setResponseRepoPath(responsePath);
    }

    public RepoResourceInfo getInfo() {
        return wrappedResource.getInfo();
    }

    public long getSize() {
        return wrappedResource.getSize();
    }

    public long getCacheAge() {
        return wrappedResource.getCacheAge();
    }

    public long getLastModified() {
        return wrappedResource.getLastModified();
    }

    public String getMimeType() {
        return wrappedResource.getMimeType();
    }

    public boolean isFound() {
        return false;
    }

    public boolean isExactQueryMatch() {
        return wrappedResource.isExactQueryMatch();
    }

    public boolean isExpired() {
        return true;
    }

    public boolean isMetadata() {
        return wrappedResource.isMetadata();
    }

    @Override
    public String toString() {
        return wrappedResource.getRepoPath().toString();
    }
}