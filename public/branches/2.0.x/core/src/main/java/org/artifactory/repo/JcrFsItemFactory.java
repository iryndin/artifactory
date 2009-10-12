/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
     * Given a relative path, returns the file or folder node in the repository. Acquire a read lock, if item exists.
     *
     * @param relPath the relative path in this repository
     * @return the file system item or null if does not exists in repo
     * @throws org.artifactory.api.repo.exception.RepositoryRuntimeException
     *          if some repository errors occured
     * @deprecated Please use RepoPath methods
     */
    @Deprecated
    JcrFsItem getJcrFsItem(String relPath);

    /**
     * Given a repo path, returns the file or folder node in the repository. Acquire a read lock, if item exists.
     *
     * @param repoPath the repo path
     * @return the file system item or null if does not exists in repo
     * @throws org.artifactory.api.repo.exception.RepositoryRuntimeException
     *                                  if some repository errors occured
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

    @Deprecated
    JcrFile getJcrFile(String relPath) throws FileExpectedException;

    JcrFile getJcrFile(RepoPath repoPath) throws FileExpectedException;

    @Deprecated
    JcrFolder getJcrFolder(String relPath) throws FolderExpectedException;

    JcrFolder getJcrFolder(RepoPath repoPath) throws FolderExpectedException;

    /**
     * Given a repo path, returns the file or folder node in the repository. Acquire a write lock, if item exists.
     *
     * @param repoPath
     * @return the locked fs item
     */
    JcrFsItem getLockedJcrFsItem(RepoPath repoPath);

    @Deprecated
    JcrFsItem getLockedJcrFsItem(String relPath);

    @Deprecated
    JcrFile getLockedJcrFile(String relPath, boolean createIfMissing) throws FileExpectedException;

    JcrFile getLockedJcrFile(RepoPath repoPath, boolean createIfMissing) throws FileExpectedException;

    @Deprecated
    JcrFolder getLockedJcrFolder(String relPath, boolean createIfMissing) throws FolderExpectedException;

    JcrFolder getLockedJcrFolder(RepoPath repoPath, boolean createIfMissing) throws FolderExpectedException;

    JcrFsItem getLockedJcrFsItem(Node node);
}
