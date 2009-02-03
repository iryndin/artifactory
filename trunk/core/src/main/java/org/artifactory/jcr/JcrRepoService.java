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
package org.artifactory.jcr;

import org.artifactory.api.common.StatusHolder;
import org.artifactory.api.config.ExportSettings;
import org.artifactory.api.config.ImportSettings;
import org.artifactory.api.repo.Lock;
import org.artifactory.api.repo.RepoPath;
import org.artifactory.jcr.fs.JcrFile;
import org.artifactory.jcr.fs.JcrFolder;
import org.artifactory.jcr.fs.JcrFsItem;
import org.artifactory.repo.LocalRepo;

import javax.jcr.Node;
import java.io.File;
import java.io.InputStream;
import java.util.List;

/**
 * This interface should be used by JcrRepoBase only. It contains all the methods Tx or not, that are needed to manage
 * JcrFsItem
 *
 * @author freds
 * @date Oct 24, 2008
 */
public interface JcrRepoService {
    @Lock(transactional = true)
    JcrFile importStream(
            JcrFolder parentFolder, String name, long lastModified, InputStream in);

    @Lock(transactional = true)
    JcrFile importFileViaWorkingCopy(
            JcrFolder parentFolder, File file, ImportSettings settings, StatusHolder status);

    @Lock(transactional = true)
    RepoPath importFolder(LocalRepo repo, RepoPath jcrFolder, ImportSettings settings,
            StatusHolder status);

    @Lock(transactional = true)
    JcrFsItem getFsItem(RepoPath repoPath, LocalRepo repo);

    JcrFsItem getFsItem(Node node, LocalRepo repo);

    /**
     * Create the children list of file system items for the given folder. If withLock is true, all children will be
     * create with write lock acquired, If false the read lock will be acquired.
     *
     * @param folder   the JcrFolder parent of all resulting children
     * @param withLock true to acquire write lock
     * @return The list of children
     */
    @Lock(transactional = true, readOnly = true)
    List<JcrFsItem> getChildren(JcrFolder folder, boolean withLock);

    @Lock(transactional = true)
    boolean delete(String absPath);

    /**
     * Copied from JcrService. TODO: Check how to remove the duplication
     *
     * @return
     */
    JcrSession getManagedSession();

    /**
     * Create an unstructure node under the parent node paased Copied from JcrService. TODO: Check how to remove the
     * duplication
     *
     * @param parent     the parent node where this folder name should be
     * @param folderName the new or existing folder name
     * @return the new or current node for the folder
     */
    Node getOrCreateUnstructuredNode(Node parent, String folderName);

    /**
     * Create an unstructure node under the root node of the jcr repository Copied from JcrService. TODO: Check how to
     * remove the duplication
     *
     * @param folderName the new or existing folder name
     * @return the new or current node for the folder
     */
    Node getOrCreateUnstructuredNode(String folderName);

    Node getOrCreateNode(Node parent, String name, String type);

    @Lock(transactional = true)
    void exportFile(JcrFile jcrFile, ExportSettings settings, StatusHolder status);

    @Lock(transactional = true)
    List<String> getChildrenNames(String absPath);
}
