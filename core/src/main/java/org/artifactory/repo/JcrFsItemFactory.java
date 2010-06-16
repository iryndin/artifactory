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

import org.artifactory.api.repo.RepoPath;
import org.artifactory.api.repo.exception.FileExpectedException;
import org.artifactory.api.repo.exception.FolderExpectedException;
import org.artifactory.jcr.fs.JcrFile;
import org.artifactory.jcr.fs.JcrFolder;
import org.artifactory.jcr.fs.JcrFsItem;

import javax.jcr.Node;

/**
 * @author freds
 * @date Nov 17, 2008
 */
public interface JcrFsItemFactory {
    /**
     * Given a relative path, returns the file or folder node in the repository. Acquires a read lock, if item exists.
     *
     * @param relPath the relative path in this repository
     * @return the file system item or null if does not exists in repo
     * @throws org.artifactory.api.repo.exception.RepositoryRuntimeException
     *          if some repository errors occurred
     */
    JcrFsItem getLocalJcrFsItem(String relPath);

    /**
     * Given a repo path, returns the file or folder node in the repository. Acquires a read lock, if item exists.
     *
     * @param repoPath the repo path
     * @return the file system item or null if does not exists in repo
     * @throws org.artifactory.api.repo.exception.RepositoryRuntimeException
     *                                  if some repository errors occurred
     * @throws IllegalArgumentException if the repo path is invalid
     */
    JcrFsItem getJcrFsItem(RepoPath repoPath);

    /**
     * Create the JcrFile or JcrFolder object out of the given JCR node. The JCR access will be done in this method, and
     * the JcrFsItem will be usable after (no more need for JcrSession). This method acquire a read lock.
     *
     * @param node The JCR node to create the Fs Item from
     * @return The JcrFile or JcrFolder from the node
     */
    JcrFsItem getJcrFsItem(Node node);

    JcrFile getLocalJcrFile(String relPath) throws FileExpectedException;

    JcrFile getJcrFile(RepoPath repoPath) throws FileExpectedException;

    JcrFolder getLocalJcrFolder(String relPath) throws FolderExpectedException;

    JcrFolder getJcrFolder(RepoPath repoPath) throws FolderExpectedException;

    /**
     * Given a repo path, returns the file or folder node in the repository. Acquire a write lock, if item exists. If
     * item doesn't exist return null.
     *
     * @param repoPath The item's path
     * @return the locked fs item or null if item doesn't exist
     */
    JcrFsItem getLockedJcrFsItem(RepoPath repoPath);

    JcrFsItem getLockedJcrFsItem(String relPath);

    JcrFile getLockedJcrFile(String relPath, boolean createIfMissing) throws FileExpectedException;

    JcrFile getLockedJcrFile(RepoPath repoPath, boolean createIfMissing) throws FileExpectedException;

    JcrFolder getLockedJcrFolder(String relPath, boolean createIfMissing) throws FolderExpectedException;

    JcrFolder getLockedJcrFolder(RepoPath repoPath, boolean createIfMissing) throws FolderExpectedException;

    JcrFsItem getLockedJcrFsItem(Node node);
}
