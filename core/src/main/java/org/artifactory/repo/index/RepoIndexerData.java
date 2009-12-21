/*
 * This file is part of Artifactory.
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

package org.artifactory.repo.index;


import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.lucene.store.Directory;
import org.artifactory.api.maven.MavenNaming;
import org.artifactory.api.repo.RepoPath;
import org.artifactory.common.ConstantValues;
import org.artifactory.common.ResourceStreamHandle;
import org.artifactory.io.NullResourceStreamHandle;
import org.artifactory.io.TempFileStreamHandle;
import org.artifactory.jcr.fs.JcrFile;
import org.artifactory.jcr.fs.JcrFolder;
import org.artifactory.log.LoggerFactory;
import org.artifactory.repo.LocalRepo;
import org.artifactory.repo.RealRepo;
import org.artifactory.repo.RemoteRepo;
import org.artifactory.repo.jcr.StoringRepo;
import org.slf4j.Logger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;

/**
 * User: freds Date: Aug 13, 2008 Time: 12:08:30 PM
 */
public class RepoIndexerData {
    private static final Logger log = LoggerFactory.getLogger(RepoIndexerData.class);

    final RealRepo indexedRepo;
    IndexStatus indexStatus = IndexStatus.NOT_CREATED;
    StoringRepo indexStorageRepo;
    ResourceStreamHandle indexHandle;
    ResourceStreamHandle propertiesHandle;

    private enum IndexStatus {
        NOT_CREATED, NEEDS_SAVING, SKIP
    }

    RepoIndexerData(RealRepo indexedRepo) {
        if (indexedRepo == null) {
            throw new IllegalArgumentException("Repository for indexing cannot be null.");
        }
        this.indexedRepo = indexedRepo;
        indexStatus = IndexStatus.NOT_CREATED;
    }

    public RepoIndexerData(RepoIndexer indexer, Directory exitingIndexedDir) throws IOException {
        this.indexStorageRepo = indexer.getRepo();
        this.indexHandle = indexer.createIndex(exitingIndexedDir, false);
        this.propertiesHandle = indexer.getProperties();
        indexedRepo = null;
        indexStatus = IndexStatus.NEEDS_SAVING;
    }

    boolean fetchRemoteIndex() {
        if (indexedRepo.isLocal()) {
            indexStorageRepo = (LocalRepo) indexedRepo;
            return false;
        } else {
            //For remote repositories, try to download the remote cache. If failes - index locally
            RemoteRepo remoteRepo = (RemoteRepo) indexedRepo;
            if (remoteRepo.isOffline()) {
                log.debug("Not retrieving index for remote repository '{}'.", indexedRepo.getKey());
                return false;
            }
            if (remoteRepo.isStoreArtifactsLocally()) {
                indexStorageRepo = remoteRepo.getLocalCacheRepo();
            }

            /*DefaultIndexUpdater indexUpdater = new DefaultIndexUpdater();
            Maven maven = InternalContextHelper.get().beanForType(Maven.class);
            WagonManager wagonManager = maven.getWagonManager();
            FieldUtils.setProtectedFieldValue("wagonManager", indexUpdater, wagonManager);*/
            //ProxyInfo proxyInfo = new ProxyInfo();
            //Update the proxy info
            //indexUpdater.fetchIndexProperties(ic, tl, proxyInfo);

            File tempIndex = null;
            File tempProperties = null;
            ResourceStreamHandle remoteIndexHandle = null;
            ResourceStreamHandle remotePropertiesHandle = null;
            try {
                //Never auto-fetch the index from central if it cannot be stored locally
                if (!shouldFetchRemoteIndex(remoteRepo)) {
                    //Return true so that we don't attempt to index locally as a fallback
                    return true;
                }

                //If we receive a non-modified response (with a null handle) - don't re-download the index
                remoteIndexHandle = remoteRepo.conditionalRetrieveResource(MavenNaming.NEXUS_INDEX_ZIP_PATH);
                if (remoteIndexHandle instanceof NullResourceStreamHandle) {
                    log.debug("No need to fetch unmodified index for remote repository '{}'.", indexedRepo.getKey());
                    indexStatus = IndexStatus.SKIP;
                    return true;
                }
                //Save into temp files
                tempIndex = File.createTempFile(MavenNaming.NEXUS_INDEX_ZIP, null);
                IOUtils.copy(remoteIndexHandle.getInputStream(), new FileOutputStream(tempIndex));

                remotePropertiesHandle = remoteRepo.downloadResource(MavenNaming.NEXUS_INDEX_PROPERTIES_PATH);
                tempProperties = File.createTempFile(MavenNaming.NEXUS_INDEX_PROPERTIES, null);
                IOUtils.copy(remotePropertiesHandle.getInputStream(), new FileOutputStream(tempProperties));

                //Return the handle to the zip file (will be removed when the handle is closed)
                indexHandle = new TempFileStreamHandle(tempIndex);
                propertiesHandle = new TempFileStreamHandle(tempProperties);
                indexStatus = IndexStatus.NEEDS_SAVING;
                return true;
            } catch (IOException e) {
                closeHandles();
                FileUtils.deleteQuietly(tempIndex);
                FileUtils.deleteQuietly(tempProperties);
                log.warn("Could not retrieve remote nexus index '" + MavenNaming.NEXUS_INDEX_ZIP +
                        "' for repo '" + indexedRepo + "': " + e.getMessage());
                return false;
            } finally {
                if (remoteIndexHandle != null) {
                    remoteIndexHandle.close();
                }
                if (remotePropertiesHandle != null) {
                    remotePropertiesHandle.close();
                }
            }
        }
    }

    void createLocalIndex(Date fireTime, boolean remoteIndexExists) {
        if (indexStatus != IndexStatus.NOT_CREATED) {
            return;
        }
        //For remote repositories, only index locally if not already fecthed remotely before and if has local
        //storage
        if (!indexedRepo.isLocal() && remoteIndexExists) {
            if (!((RemoteRepo) indexedRepo).isStoreArtifactsLocally()) {
                log.debug("Skipping index creation for remote repo '{}': repo does not store artifacts locally",
                        indexedRepo.getKey());
            }
            return;
        }
        RepoIndexer repoIndexer = new RepoIndexer(indexStorageRepo);
        try {
            indexHandle = repoIndexer.index(fireTime);
            propertiesHandle = repoIndexer.getProperties();
            indexStatus = IndexStatus.NEEDS_SAVING;
        } catch (Exception e) {
            closeHandles();
            throw new RuntimeException("Failed to index repository '" + indexedRepo + "'.", e);
        }
    }

    boolean saveIndexFiles() {
        try {
            //Might be virtual
            if (!indexStorageRepo.isLocal() && indexStorageRepo.isReal()) {
                if (!((RemoteRepo) indexStorageRepo).isStoreArtifactsLocally()) {
                    log.debug("Skipping index saving for remote repo '{}': repo does not store artifacts locally",
                            indexStorageRepo.getKey());
                    return false;
                }
            }

            if (indexStatus != IndexStatus.NEEDS_SAVING) {
                return false;
            }
            //Create the new jcr files for index and properties
            //Create the index dir
            JcrFolder targetIndexDir = indexStorageRepo.getLockedJcrFolder(getIndexFolderRepoPath(), true);
            targetIndexDir.mkdirs();
            InputStream indexInputStream = indexHandle.getInputStream();
            InputStream propertiesInputStream = propertiesHandle.getInputStream();
            //Copy to jcr, acquiring the lock as latest as possible
            JcrFile indexFile = indexStorageRepo.getLockedJcrFile(new RepoPath(
                    targetIndexDir.getRepoPath(), MavenNaming.NEXUS_INDEX_ZIP), true);
            indexFile.fillData(indexInputStream);
            JcrFile propertiesFile = indexStorageRepo.getLockedJcrFile(new RepoPath(
                    targetIndexDir.getRepoPath(), MavenNaming.NEXUS_INDEX_PROPERTIES), true);
            propertiesFile.fillData(propertiesInputStream);
            log.info("Successfully saved index file '{}' and index info '{}'.",
                    indexFile.getAbsolutePath(), propertiesFile.getAbsolutePath());
            return true;
        } catch (Exception e) {
            throw new RuntimeException("Failed to save index file for repo '" + indexStorageRepo + "'.", e);
        } finally {
            closeHandles();
        }
    }

    private void closeHandles() {
        if (indexHandle != null) {
            indexHandle.close();
        }
        if (propertiesHandle != null) {
            propertiesHandle.close();
        }
    }

    RepoPath getIndexFolderRepoPath() {
        return new RepoPath(indexStorageRepo.getRootFolder().getRepoPath(), MavenNaming.NEXUS_INDEX_DIR);
    }

    private boolean shouldFetchRemoteIndex(RemoteRepo remoteRepo) {
        if (!remoteRepo.isStoreArtifactsLocally() &&
                remoteRepo.getUrl().contains(ConstantValues.mvnCentralHostPattern.getString())) {
            log.debug("Central index cannot be periodically fetched.Remote repository '{}' does not support " +
                    "local index storage.", remoteRepo.getUrl());
            return false;
        }
        return true;
    }
}
