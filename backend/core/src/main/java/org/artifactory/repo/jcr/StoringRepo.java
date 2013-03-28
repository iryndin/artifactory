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

package org.artifactory.repo.jcr;

import org.artifactory.api.repo.exception.RepoRejectException;
import org.artifactory.descriptor.repo.RepoDescriptor;
import org.artifactory.fs.RepoResource;
import org.artifactory.io.checksum.policy.ChecksumPolicy;
import org.artifactory.jcr.factory.JcrFsItemFactory;
import org.artifactory.jcr.fs.JcrFolder;
import org.artifactory.jcr.fs.JcrFsItem;
import org.artifactory.repo.Repo;
import org.artifactory.repo.RepoPath;
import org.artifactory.repo.SaveResourceContext;
import org.artifactory.sapi.fs.VfsFolder;

import java.io.IOException;
import java.util.List;

public interface StoringRepo<T extends RepoDescriptor> extends Repo<T>, JcrFsItemFactory {

    VfsFolder getRootFolder();

    @Override
    boolean itemExists(String relPath);

    /**
     * Save the resource in the repository.
     */
    RepoResource saveResource(SaveResourceContext context) throws IOException, RepoRejectException;

    void undeploy(RepoPath repoPath);

    void undeploy(RepoPath repoPath, boolean calcMavenMetadata);

    boolean shouldProtectPathDeletion(String relPath, boolean overwrite);

    List<String> getChildrenNames(String relPath);

    String getRepoRootPath();

    JcrFolder getLockedRootFolder();

    @Override
    ChecksumPolicy getChecksumPolicy();

    void onCreate(JcrFsItem fsItem);

    @Override
    void onDelete(JcrFsItem fsItem);

    boolean willOrIsWriteLocked(RepoPath path);

    StoringRepo<T> getStorageMixin();

    void clearCaches();
}