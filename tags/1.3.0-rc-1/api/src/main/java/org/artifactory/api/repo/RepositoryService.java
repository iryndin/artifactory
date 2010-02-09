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
package org.artifactory.api.repo;

import org.artifactory.api.common.StatusHolder;
import org.artifactory.api.config.ExportSettings;
import org.artifactory.api.config.ImportSettings;
import org.artifactory.api.config.ImportableExportable;
import org.artifactory.api.fs.DeployableUnit;
import org.artifactory.api.fs.FolderInfo;
import org.artifactory.api.fs.ItemInfo;
import org.artifactory.api.maven.MavenArtifactInfo;
import org.artifactory.api.repo.exception.RepoAccessException;
import org.artifactory.descriptor.repo.LocalRepoDescriptor;
import org.artifactory.descriptor.repo.RepoDescriptor;
import org.artifactory.descriptor.repo.VirtualRepoDescriptor;

import javax.jcr.Repository;
import java.io.File;
import java.util.List;

/**
 * User: freds Date: Jul 21, 2008 Time: 8:07:50 PM
 */
@Lock(transactional = true)
public interface RepositoryService extends ImportableExportable {
    @Lock(transactional = true)
    public List<VirtualRepoItem> getVirtualRepoItems(RepoPath repoPath);

    Repository getRepository();

    /**
     * @return null if repoPath invalid (equivalent to 404)
     */
    @Lock(transactional = true)
    List<DirectoryItem> getDirectoryItems(RepoPath repoPath, boolean withPseudoUpDirItem);

    List<LocalRepoDescriptor> getLocalRepoDescriptors();

    @Lock(transactional = true)
    DeployableArtifact getDeployableArtifact(File uploadedFile);

    @Lock(transactional = true)
    void deploy(RepoDescriptor targetRepo, DeployableArtifact deployableArtifact, boolean forceDeployPom,
            File uploadedFile) throws RepoAccessException;

    @Lock(transactional = true)
    boolean pomExists(RepoDescriptor targetRepo, DeployableArtifact deployableArtifact);

    List<VirtualRepoDescriptor> getVirtualRepoDescriptors();

    LocalRepoDescriptor localOrCachedRepoDescriptorByKey(String key);

    List<RepoDescriptor> getLocalAndRemoteRepoDescriptors();

    List<LocalRepoDescriptor> getLocalAndCachedRepoDescriptors();

    VirtualRepoDescriptor virtualRepoDescriptorByKey(String repoKey);

    @Lock(transactional = true)
    String getPomContent(ItemInfo itemInfo);

    /**
     * Import all the repositories under the passed folder which matches local or cached repository declared in the
     * configuration. Having empty directory for each repository is allowed and not an error. Nothing will be imported
     * for those.
     *
     * @param settings
     * @param status
     */
    void importAll(ImportSettings settings, StatusHolder status);

    /**
     * Import the artifacts under the folder passed directly in the repository named "repoKey". If no repository with
     * this rpo key exists or if the folder passed is empty, the status will be set to error.
     *
     * @param repoKey
     * @param settings
     * @param status
     */
    @Lock(transactional = false)
    void importRepo(String repoKey, ImportSettings settings, StatusHolder status);

    @Lock(transactional = true)
    ItemInfo getItemInfo(RepoPath repoPath);

    @Lock(transactional = true)
    <MD> MD getXmlMetdataObject(RepoPath repoPath, Class<MD> metadataClass);

    @Lock(transactional = true)
    String getXmlMetadata(RepoPath repoPath, String metadataName);

    @Lock(transactional = true)
    boolean hasXmlMetdata(RepoPath repoPath, String metadataName);


    @Lock(transactional = false)
    void undeploy(RepoPath repoPath);

    @Lock(transactional = true)
    void zap(RepoPath repoPath);

    @Lock(transactional = true)
    MavenArtifactInfo getMavenArtifactInfo(ItemInfo itemInfo);

    @Lock(transactional = true)
    List<FolderInfo> getWithEmptyChildren(FolderInfo folderInfo);

    List<String> getAllRepoKeys();

    @Lock(transactional = true)
    boolean exists(RepoPath repoPath);

    List<String> getChildrenNames(RepoPath repoPath);

    @Lock(transactional = true)
    boolean hasChildren(RepoPath repoPath);

    @Lock(transactional = false)
    void exportRepo(String repoKey, ExportSettings settings, StatusHolder status);


    /**
     * Returns all the deployable units under a certain path.
     *
     * @param repoPath The repository path (might be repository root with no sub-path)
     * @return deployable units under a certain path
     */
    List<DeployableUnit> getDeployableUnitsUnder(RepoPath repoPath);

    /**
     * Returns the number of artifacts currently being served
     *
     * @return ArtifactCount
     */
    ArtifactCount getArtifactCount();

}
