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

package org.artifactory.api.search.metadata.pom;

import org.artifactory.api.repo.RepoPath;
import org.artifactory.api.search.metadata.MetadataSearchControls;

/**
 * Holds the POM search parameters
 *
 * @author Noam Tenne
 */
public class PomSearchControls<T> extends MetadataSearchControls<T> {

    private RepoPath repoPath;

    public PomSearchControls(RepoPath repoPath) {
        this.repoPath = repoPath;
        addRepoToSearch(repoPath.getRepoKey());
        setMetadataName("*.pom");
    }

    public RepoPath getRepoPath() {
        return repoPath;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }
}
