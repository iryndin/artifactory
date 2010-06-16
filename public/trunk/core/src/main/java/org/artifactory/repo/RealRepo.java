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

package org.artifactory.repo;

import org.artifactory.api.common.StatusHolder;
import org.artifactory.api.repo.RepoPath;
import org.artifactory.descriptor.repo.RealRepoDescriptor;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public interface RealRepo<T extends RealRepoDescriptor> extends Repo<T> {

    boolean isHandleReleases();

    boolean isHandleSnapshots();

    boolean isBlackedOut();

    StatusHolder checkDownloadIsAllowed(RepoPath repoPath);

    boolean handles(String path);

    int getMaxUniqueSnapshots();

    StatusHolder assertValidPath(RepoPath repoPath);

    boolean accepts(RepoPath repoPath);

}
