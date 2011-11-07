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

package org.artifactory.api.repo.cleanup;

import org.artifactory.sapi.common.Lock;

/**
 * The main interface of the clean-up service
 *
 * @author Noam Tenne
 */
public interface ArtifactCleanupService {

    /**
     * Cleans the remote repository's cache from artifacts that havn't been used in the given time period
     *
     * @param repoKey     Key of remote repository cache
     * @param periodHours The period to keep unused artifacts for. Any artifacts which havn't been used for more than
     */
    @Lock(transactional = true)
    void clean(String repoKey, long periodHours);
}
