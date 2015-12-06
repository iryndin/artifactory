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

package org.artifactory.service;

import org.artifactory.fs.RepoResource;
import org.springframework.stereotype.Service;

/**
 * Provides smart repo services
 *
 * @author Michael Pasternak
 */
@Service
public interface SmartRepoService {
    /**
     * Triggered on file download
     *
     * @param resource
     */
    void onFileDownload(RepoResource resource);

    /**
     * Updates properties from remote repository (if expired)
     *
     * @param resource an resource to update properties for
     *
     * @return success/failure
     */
    boolean updateProperties(RepoResource resource);

    /**
     * Checks whether remote (the source) was deleted
     *
     * @param repoResource
     *
     * @return true/false
     */
    boolean isArtifactMarkedAsRemoteDeleted(RepoResource repoResource);
}
