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

package org.artifactory.resource;

import org.apache.commons.httpclient.HttpStatus;
import org.artifactory.repo.RepoPath;

public class UnfoundRepoResource extends FileResource {
    private final String reason;
    private final int statusCode;

    public UnfoundRepoResource(RepoPath repoPath, String reason) {
        this(repoPath, reason, HttpStatus.SC_NOT_FOUND);
    }

    public UnfoundRepoResource(RepoPath repoPath, String reason, int statusCode) {
        super(repoPath);
        this.reason = reason;
        this.statusCode = statusCode > 0 ? statusCode : HttpStatus.SC_NOT_FOUND;
    }

    @Override
    public boolean isFound() {
        return false;
    }

    public String getReason() {
        return reason;
    }

    public int getStatusCode() {
        return statusCode;
    }
}