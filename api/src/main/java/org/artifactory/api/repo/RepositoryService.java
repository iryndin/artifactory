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

package org.artifactory.api.repo;

import org.artifactory.api.common.MoveMultiStatusHolder;
import org.artifactory.api.common.MultiStatusHolder;
import org.artifactory.api.common.StatusHolder;
import org.artifactory.api.config.ExportSettings;
import org.artifactory.api.config.ImportSettings;
import org.artifactory.api.config.ImportableExportable;
import org.artifactory.api.fs.DeployableUnit;
import org.artifactory.api.fs.FileInfo;
import org.artifactory.api.fs.FolderInfo;
import org.artifactory.api.fs.ItemInfo;
import org.artifactory.api.maven.MavenArtifactInfo;
import org.artifactory.api.search.SavedSearchResults;
import org.artifactory.api.tree.fs.ZipEntriesTree;
import org.artifactory.descriptor.repo.LocalCacheRepoDescriptor;
import org.artifactory.descriptor.repo.LocalRepoDescriptor;
import org.artifactory.descriptor.repo.RemoteRepoDescriptor;
import org.artifactory.descriptor.repo.RepoDescriptor;
import org.artifactory.descriptor.repo.VirtualRepoDescriptor;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * User: freds Date: Jul 21, 2008 Time: 8:07:50 PM
 */
public interface RepositoryService extends ImportableExportable {
    /**
     * @param repoPath Path to a virtual repo item.
     * @return Virtual repo item with all real repo keys the real item exists and accessible to the current user. Null
     *         if not found or user has no permissions.
     */
    @Lock(transactional = true)
    public VirtualRepoItem getVirtualRepoItem(RepoPath repoPath);

    /**
     * @param virtualFolderPath Path to a virtual repo folder.
     * @return List of virtual items (files and folders) under the virtual folders. Will return empty list if item
     *         doesn't exist, not a file of no permissions.
     */
    @Lock(transactional = true)
    public List<VirtualRepoItem> getVirtualRepoItems(RepoPath virtualFolderPath);

    /**
     * @return null if repoPath invalid (equivalent to 404)
     */
    @Lock(transactional = true)
    List<DirectoryItem> getDirectoryItems(RepoPath repoPath, boolean withPseudoUpDirItem);

    List<LocalRepoDescriptor> getLocalRepoDescriptors();

    List<LocalCacheRepoDescriptor> getCachedRepoDescriptors();

    List<VirtualRepoDescriptor> getVirtualRepoDescriptors();

    /**
     * @return Gets a list of local and remote repositories, no cahces
     */
    List<RepoDescriptor> getLocalAndRemoteRepoDescriptors();

    List<LocalRepoDescriptor> getLocalAndCachedRepoDescriptors();

    List<RemoteRepoDescriptor> getRemoteRepoDescriptors();

    /**
     * Gets a local or cache repository by key.
     *
     * @param repoKey The key for a cache can either be the remote repository one or the cache one(ends with "-cache")
     */
    LocalRepoDescriptor localOrCachedRepoDescriptorByKey(String repoKey);

    LocalRepoDescriptor localRepoDescriptorByKey(String key);

    RemoteRepoDescriptor remoteRepoDescriptorByKey(String repoKey);

    VirtualRepoDescriptor virtualRepoDescriptorByKey(String repoKey);

    @Lock(transactional = true)
    String getTextFileContent(FileInfo fileInfo);

    /**
     * @param archivePath     Repository path of the archive file
     * @param sourceEntryPath File path inside the archive
     * @return The source entry details (including content if found)
     * @throws IOException On failure reading to archive or the sources file (will not fail if not found)
     */
    @Lock(transactional = true)
    public ArchiveFileContent getArchiveFileContent(RepoPath archivePath, String sourceEntryPath) throws IOException;

    /**
     * Import all the repositories under the passed folder which matches local or cached repository declared in the
     * configuration. Having empty directory for each repository is allowed and not an error. Nothing will be imported
     * for those.
     *
     * @param settings
     */
    void importAll(ImportSettings settings);

    /**
     * Import the artifacts under the folder passed directly in the repository named "repoKey". If no repository with
     * this repo key exists or if the folder passed is empty, the status will be set to error.
     *
     * @param repoKey
     * @param settings
     */
    @Lock(transactional = false)
    void importRepo(String repoKey, ImportSettings settings);

    @Lock(transactional = true)
    ItemInfo getItemInfo(RepoPath repoPath);

    /**
     * Returns the available metadata names which are not internal
     *
     * @param repoPath The full path of the object having metadata
     * @return A list of metadata names that exists on this element
     */
    @Lock(transactional = true)
    List<String> getMetadataNames(RepoPath repoPath);

    /**
     * Returns the metadata of the given type class.<br>
     * To be used only with non-generic metadata classes.<br>
     * Generic (String class) will be ignored.<br>
     *
     * @param repoPath      A repo path (usually pointing to an JcrFsItem)
     * @param metadataClass Class of metadata type. Cannot be generic or null
     * @param <MD>          Metadata type
     * @return Requested metadata if found. Null if not
     * @throws IllegalArgumentException If given a null metadata class
     */
    @Lock(transactional = true)
    <MD> MD getMetadata(RepoPath repoPath, Class<MD> metadataClass);

    /**
     * Returns the metadata of the given name.
     *
     * @param repoPath     A repo path (usually pointing to an JcrFsItem)
     * @param metadataName Name of metadata to return. Cannot be null
     * @return Requested metadata if found. Null if not
     * @throws IllegalArgumentException If given a blank metadata name
     */
    @Lock(transactional = true)
    String getXmlMetadata(RepoPath repoPath, String metadataName);

    /**
     * Indicates whether this item adorns the given metadata
     *
     * @param repoPath     A repo path (usually pointing to an JcrFsItem)
     * @param metadataName Name of metadata to locate
     * @return True if annotated by the given metadata. False if not
     * @throws IllegalArgumentException If given a blank metadata name
     */
    @Lock(transactional = true)
    boolean hasMetadata(RepoPath repoPath, String metadataName);

    /**
     * Sets the given metadata on the supplied repo path.<br>
     * To be used only with non-generic metadata classes.<br>
     * Generic (String class) will be ignored.<br>
     *
     * @param repoPath      Path to targeted item
     * @param metadataClass Type class of metadata to set
     * @param metadata      Value of metadata to set. Cannot be null
     * @throws IllegalArgumentException When given a null metadata value
     */
    @Lock(transactional = true)
    <MD> void setMetadata(RepoPath repoPath, Class<MD> metadataClass, MD metadata);

    /**
     * Sets the given metadata on the supplied repo path.
     *
     * @param repoPath        Path to targeted item
     * @param metadataName    The metadata name to set under
     * @param metadataContent The metadata content to add. Cannot be null
     * @throws IllegalArgumentException When given a null metadata value
     */
    @Lock(transactional = true)
    void setXmlMetadata(RepoPath repoPath, String metadataName, @Nonnull String metadataContent);

    /**
     * Removes the metadata of the given name
     *
     * @param repoPath     Path to targeted item
     * @param metadataName Name of metadata to remove
     */
    @Lock(transactional = true)
    void removeMetadata(RepoPath repoPath, String metadataName);

    @Request
    @Lock(transactional = true)
    StatusHolder undeploy(RepoPath repoPath);

    @Request
    @Lock(transactional = true)
    StatusHolder undeploy(RepoPath repoPath, boolean calcMavenMetadata);

    @Request
    StatusHolder undeployPaths(List<RepoPath> repoPath);

    /**
     * Moves repository path (pointing to a folder) to another local repository. The move will only move paths the user
     * has permissions to move and paths that are accepted by the target repository. Maven metadata will be recalculated
     * for both the source and target folders.
     *
     * @param repoPath           Repository path to move. This path must represent a folder in a local repository.
     * @param targetLocalRepoKey Key of the target local non-cached repository to move the path to.
     * @param dryRun             If true the method will just report the expected result but will not move any file
     * @return MoveMultiStatusHolder holding the errors and warnings
     */
    @Lock(transactional = true)
    MoveMultiStatusHolder move(RepoPath repoPath, String targetLocalRepoKey, boolean dryRun);

    /**
     * Moves repository path (pointing to a folder) to another absolute target. The move will only move paths the user
     * has permissions to move and paths that are accepted by the target repository. Maven metadata will be recalculated
     * for both the source and target folders.
     *
     * @param repoPath   Repository path to move. This path must represent a folder in a local repository.
     * @param targetPath The target local non-cached repository to move the path to.
     * @param dryRun     If true the method will just report the expected result but will not move any file
     * @return MoveMultiStatusHolder holding the errors and warnings
     */
    @Lock(transactional = true)
    MoveMultiStatusHolder move(RepoPath fromRepoPath, RepoPath targetPath, boolean dryRun);


    /**
     * Moves set of paths to another local repository.
     * <p/>
     * This method will only move paths the user has permissions to move and paths that are accepted by the target
     * repository.
     * <p/>
     * Maven metadata will be recalculated for both the source and target folders after all items has been moved. If a
     * path already belongs to the target repository it will be skipped.
     *
     * @param pathsToMove   Paths to move, each pointing to file or folder.
     * @param targetRepoKey Key of the target local non-cached repository to move the path to.
     * @param dryRun        If true the method will just report the expected result but will not move any file
     * @return MoveMultiStatusHolder holding the errors and warnings
     */
    @Lock(transactional = true)
    MoveMultiStatusHolder move(Set<RepoPath> pathsToMove, String targetRepoKey, boolean dryRun);

    /**
     * Copies repository path (pointing to a folder) to another local repository. The copy will only move paths the user
     * has permissions to move and paths that are accepted by the target repository. Maven metadata will be recalculated
     * for both the source and target folders.
     *
     * @param fromRepoPath       Repository path to copy. This path must represent a folder in a local repository.
     * @param targetLocalRepoKey Key of the target local non-cached repository to copy the path to.
     * @param dryRun             If true the method will just report the expected result but will not copy any file
     * @return MoveMultiStatusHolder holding the errors and warnings
     */
    @Lock(transactional = true)
    MoveMultiStatusHolder copy(RepoPath fromRepoPath, String targetLocalRepoKey, boolean dryRun);

    /**
     * Copies repository path (pointing to a folder) to another absolute path. The copy will only move paths the user
     * has permissions to move and paths that are accepted by the target repository. Maven metadata will be recalculated
     * for both the source and target folders.
     *
     * @param fromRepoPath   Repository path to copy. This path must represent a folder in a local repository.
     * @param targetRepoPath Path of the target local non-cached repository to copy the path to.
     * @param dryRun         If true the method will just report the expected result but will not copy any file
     * @return MoveMultiStatusHolder holding the errors and warnings
     */
    @Lock(transactional = true)
    MoveMultiStatusHolder copy(RepoPath fromRepoPath, RepoPath targetRepoPath, boolean dryRun);

    /**
     * Copies a set of paths to another local repository.
     * <p/>
     * This method will only copy paths the user has permissions to move and paths that are accepted by the target
     * repository.
     * <p/>
     * Maven metadata will be recalculated for both the source and target folders after all items has been copied. If a
     * path already belongs to the target repository it will be skipped.
     *
     * @param pathsToCopy        Paths to copy, each pointing to file or folder.
     * @param targetLocalRepoKey Key of the target local non-cached repository to move the path to.
     * @param dryRun             If true the method will just report the expected result but will not copy any file
     * @return MoveMultiStatusHolder holding the errors and warnings
     */
    @Lock(transactional = true)
    MoveMultiStatusHolder copy(Set<RepoPath> pathsToCopy, String targetLocalRepoKey, boolean dryRun);

    /**
     * Expire expirable resources (folders, snapshot artifacts, maven metadata, etc.)
     *
     * @param repoPath Cache repository path of a folder of file to zap. If it is a folder the zap is recursively
     *                 applied.
     * @return A count of the items affected by the zap
     */
    @Lock(transactional = true)
    int zap(RepoPath repoPath);

    @Lock(transactional = true)
    MavenArtifactInfo getMavenArtifactInfo(ItemInfo itemInfo);

    @Lock(transactional = true)
    List<FolderInfo> getWithEmptyChildren(FolderInfo folderInfo);

    Set<String> getAllRepoKeys();

    @Lock(transactional = true)
    boolean exists(RepoPath repoPath);

    List<String> getChildrenNames(RepoPath repoPath);

    @Lock(transactional = true)
    boolean hasChildren(RepoPath repoPath);

    @Lock(transactional = false)
    void exportRepo(String repoKey, ExportSettings settings);

    /**
     * Export the selected search result into a target directory
     *
     * @param searchResults The search results to export
     * @param baseDir       The base directory where files should be exported to
     * @param archive
     * @return The status of the procedure
     */
    @Lock(transactional = true)
    MultiStatusHolder exportSearchResults(SavedSearchResults searchResults, File baseDir, boolean includeMetadata,
            boolean m2Compatible, boolean archive);

    /**
     * Returns all the deployable units under a certain path.
     *
     * @param repoPath The repository path (might be repository root with no sub-path)
     * @return deployable units under a certain path
     */
    @Lock(transactional = true)
    List<DeployableUnit> getDeployableUnitsUnder(RepoPath repoPath);

    /**
     * Returns the number of artifacts currently being served
     *
     * @return ArtifactCount
     */
    ArtifactCount getArtifactCount();

    /**
     * Returns the number of artifacts currently being served from the specified repository
     *
     * @param repoKey Repository to query
     * @return ArtifactCount
     */
    ArtifactCount getArtifactCount(String repoKey);

    /**
     * Returns a list of local repo descriptors that the user is permitted to deploy on
     *
     * @return List<LocalRepoDescriptor> - List of deploy-permitted local repos
     */
    List<LocalRepoDescriptor> getDeployableRepoDescriptors();

    boolean isRepoPathHandled(RepoPath repoPath);

    boolean isRepoPathAccepted(RepoPath repoPath);

    /**
     * Note: you should call the markBaseForMavenMetadataRecalculation() before calling this method to recover in case
     * this task is interrupted in the middle.
     *
     * @param baseFolderPath A path to a folder to start calculating metadata from. Must be a local non-cache repository
     *                       path.
     */
    @Async(delayUntilAfterCommit = true, transactional = false)
    public void calculateMavenMetadataAsync(RepoPath baseFolderPath);

    /**
     * Calculate the maven plugins metadata asynchronously after the current transaction is committed. The reason is the
     * metadata calculator uses xpath queries for its job and since the move is not committed yet, the xpath query
     * result might not be accurate (for example when moving plugins from one repo to another the query on the source
     * repository will return the moved plugins while the target repo will not return them). <p/> Note: you should call
     * the markBaseForMavenMetadataRecalculation() before calling this method to recover in case this task is
     * interrupted in the middle.
     *
     * @param localRepoKey Key of the local non-cache repository to calculate maven plugins metadata on.
     */
    @Async(delayUntilAfterCommit = true, transactional = true)
    public void calculateMavenPluginsMetadataAsync(String localRepoKey);

    public void calculateMavenMetadata(RepoPath baseFolderPath);

    /**
     * Marks a folder for maven metadata recalculation.
     *
     * @param basePath Base folder path for the recalculation. Must be a local non-cache repository path.
     */
    @Lock(transactional = true)
    void markBaseForMavenMetadataRecalculation(RepoPath basePath);

    /**
     * For internal use by the JR WebDAV debugging servlet - should not be really exposed here
     *
     * @return
     */
    Object getJcrHandle();

    /**
     * @return List of virtual repositories that include the repository in their list.
     */
    List<VirtualRepoDescriptor> getVirtualReposContainingRepo(RepoDescriptor repoDescriptor);

    /**
     * Inidicates if the given virtual repo path exists
     *
     * @param repoPath Virtual repo path
     * @return True if repo path exists, false if not
     */
    boolean virtualItemExists(RepoPath repoPath);

    /**
     * Returns the shared remote repository list from the given Artifactory instance URL
     *
     * @param remoteUrl  URL of remote Artifactory instance
     * @param headersMap Header-map to add to the request
     * @return List of shared remote repositories
     */
    List<RemoteRepoDescriptor> getSharedRemoteRepoConfigs(String remoteUrl, Map<String, String> headersMap);

    /**
     * @param zipPath Path to a zip like file
     * @return Tree representation of the entries in the zip.
     * @throws IOException On error retrieving or parsing the zip file
     */
    @Lock(transactional = true)
    ZipEntriesTree zipEntriesToTree(RepoPath zipPath) throws IOException;
}
