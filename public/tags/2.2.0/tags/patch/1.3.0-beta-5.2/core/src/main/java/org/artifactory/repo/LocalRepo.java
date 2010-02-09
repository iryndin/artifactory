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

import org.apache.maven.model.Model;
import org.artifactory.api.config.ImportableExportable;
import org.artifactory.api.fs.FileInfo;
import org.artifactory.api.fs.ItemInfo;
import org.artifactory.api.repo.exception.FileExpectedException;
import org.artifactory.descriptor.repo.LocalRepoDescriptor;
import org.artifactory.descriptor.repo.SnapshotVersionBehavior;
import org.artifactory.jcr.fs.JcrFile;
import org.artifactory.jcr.fs.JcrFolder;
import org.artifactory.jcr.fs.JcrFsItem;
import org.artifactory.resource.ArtifactResource;
import org.artifactory.resource.RepoResource;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public interface LocalRepo<T extends LocalRepoDescriptor>
        extends RealRepo<T>, ImportableExportable {

    /**
     * Retrieve a File System object item for the path. No lock of the item is done.
     *
     * @param relPath relative Path in this repo
     * @return the file system item never null
     * @throws org.artifactory.api.repo.exception.RepositoryRuntimeException
     *          if the node does not exists or other repository errors
     */
    JcrFsItem getFsItem(String relPath);

    /**
     * Return a JcrFile object if the node exists and is a File, null otherwise. This method will do
     * a transactional lock and test.
     *
     * @param relPath relative Path in this repo
     * @return the file item if ok, null otherwise
     * @throws org.artifactory.api.repo.exception.RepositoryRuntimeException
     *          if some repository errors happens
     */
    JcrFile getLockedJcrFile(String relPath) throws FileExpectedException;

    JcrFolder getRootFolder();

    boolean itemExists(String relPath);

    SnapshotVersionBehavior getSnapshotVersionBehavior();

    Model getModel(ArtifactResource pa);

    String getPomContent(ArtifactResource pa);

    void saveResource(RepoResource res, InputStream stream) throws IOException;

    void undeploy(String relPath);

    String getRepoRootPath();

    void delete();

    boolean isAnonAccessEnabled();

    String getPomContent(ItemInfo itemInfo);

    boolean shouldProtectPathDeletion(String relPath);

    List<String> getChildrenNames(String relPath);

    JcrFile getJcrFile(String relPath) throws FileExpectedException;

    FileInfo getFileInfo(String relPath) throws FileExpectedException;
}