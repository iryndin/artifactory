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

package org.artifactory.addon.replication;

import org.artifactory.repo.RepoPath;

/**
 * @author Noam Y. Tenne
 */
public abstract class ReplicationBaseSettings {

    private final RepoPath repoPath;
    private final boolean deleteExisting;
    private final boolean includeProperties;
    private final int socketTimeoutMillis;

    protected ReplicationBaseSettings(RepoPath repoPath, boolean deleteExisting, boolean includeProperties,
            int socketTimeoutMillis) {
        this.repoPath = repoPath;
        this.deleteExisting = deleteExisting;
        this.includeProperties = includeProperties;
        this.socketTimeoutMillis = socketTimeoutMillis;
    }

    public RepoPath getRepoPath() {
        return repoPath;
    }

    public boolean isDeleteExisting() {
        return deleteExisting;
    }

    public boolean isIncludeProperties() {
        return includeProperties;
    }

    public int getSocketTimeoutMillis() {
        return socketTimeoutMillis;
    }
}
