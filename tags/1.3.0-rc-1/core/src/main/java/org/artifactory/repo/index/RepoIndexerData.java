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
package org.artifactory.repo.index;


import org.apache.commons.io.IOUtils;
import org.apache.maven.artifact.manager.WagonManager;
import org.apache.maven.wagon.proxy.ProxyInfo;
import org.artifactory.api.maven.MavenNaming;
import org.artifactory.api.repo.RepoPath;
import org.artifactory.common.ResourceStreamHandle;
import org.artifactory.io.NullResourceStreamHandle;
import org.artifactory.io.TempFileStreamHandle;
import org.artifactory.jcr.fs.JcrFile;
import org.artifactory.jcr.fs.JcrFolder;
import org.artifactory.maven.Maven;
import org.artifactory.repo.LocalRepo;
import org.artifactory.repo.RealRepo;
import org.artifactory.repo.RemoteRepo;
import org.artifactory.spring.InternalContextHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.nexus.index.updater.DefaultIndexUpdater;
import org.springframework.security.util.FieldUtils;

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
    boolean indexed = false;
    LocalRepo localRepo;
    ResourceStreamHandle indexHandle;
    ResourceStreamHandle propertiesHandle;

    RepoIndexerData(RealRepo indexedRepo) {
        if (indexedRepo == null) {
            throw new IllegalArgumentException("Repository to index cannot be null");
        }
        this.indexedRepo = indexedRepo;
    }

    void findIndex() {
        //For remote repositories, try to download the remote cache, if failes index locally
        if (!indexedRepo.isLocal()) {
            RemoteRepo remoteRepo = (RemoteRepo) indexedRepo;
            if (remoteRepo.isOffline()) {
                log.debug("Not retrieving index for remote repository '{}'", indexedRepo.getKey());
                return;
            }

            DefaultIndexUpdater indexUpdater = new DefaultIndexUpdater();
            Maven maven = InternalContextHelper.get().beanForType(Maven.class);
            WagonManager wagonManager = maven.getWagonManager();
            FieldUtils.setProtectedFieldValue("wagonManager", indexUpdater, wagonManager);
            ProxyInfo proxyInfo = new ProxyInfo();
            //Update the proxy info
            //indexUpdater.fetchIndexProperties(ic, tl, proxyInfo);

            localRepo = remoteRepo.getLocalCacheRepo();
            String indexPath = ".index/" + MavenNaming.NEXUS_INDEX_ZIP;
            String propertiesPath = ".index/" + MavenNaming.NEXUS_INDEX_PROPERTIES;
            File tempIndex = null;
            File tempProperties = null;
            ResourceStreamHandle remoteIndexHandle = null;
            ResourceStreamHandle remotePropertiesHandle = null;
            try {
                //If we receive a non-modified response (with a null handle) - don't re-download the index
                remoteIndexHandle = remoteRepo.conditionalRetrieveResource(indexPath);
                if (remoteIndexHandle instanceof NullResourceStreamHandle) {
                    log.debug("No need to fetch unmodified index for remote repository '{}'.", indexedRepo.getKey());
                    return;
                }
                remotePropertiesHandle = remoteRepo.retrieveResource(propertiesPath);
                //Save into temp files
                tempIndex = File.createTempFile(MavenNaming.NEXUS_INDEX_ZIP, null);
                tempProperties = File.createTempFile(MavenNaming.NEXUS_INDEX_PROPERTIES, null);
                IOUtils.copy(remoteIndexHandle.getInputStream(), new FileOutputStream(tempIndex));
                IOUtils.copy(remotePropertiesHandle.getInputStream(), new FileOutputStream(tempProperties));
                //Return the handle to the zip file (will be removed when the handle is closed)
                indexHandle = new TempFileStreamHandle(tempIndex);
                propertiesHandle = new TempFileStreamHandle(tempProperties);
                indexed = true;
            } catch (IOException e) {
                if (indexHandle != null) {
                    indexHandle.close();
                }
                if (propertiesHandle != null) {
                    propertiesHandle.close();
                }
                if (tempIndex != null) {
                    tempIndex.delete();
                }
                if (tempProperties != null) {
                    tempProperties.delete();
                }
                log.warn("Could not retrieve remote nexus index '" + indexPath + "' for repo '" + indexedRepo + "': " +
                        e.getMessage());
            } finally {
                if (remoteIndexHandle != null) {
                    remoteIndexHandle.close();
                }
                if (remotePropertiesHandle != null) {
                    remotePropertiesHandle.close();
                }
            }
        } else {
            localRepo = (LocalRepo) indexedRepo;
        }
    }

    void createIndex(Date fireTime) {
        if (!indexed) {
            RepoIndexer repoIndexer = new RepoIndexer(localRepo);
            try {
                indexHandle = repoIndexer.index(fireTime);
                propertiesHandle = repoIndexer.getProperties();
                indexed = true;
            } catch (Exception e) {
                if (indexHandle != null) {
                    indexHandle.close();
                }
                if (propertiesHandle != null) {
                    propertiesHandle.close();
                }
                throw new RuntimeException("Failed to index repository '" + indexedRepo + "'.", e);
            }
        }
    }

    boolean saveIndexFiles() {
        if (!indexed) {
            return false;
        }
        //Create the new jcr files for index and properties
        try {
            //Create the index dir
            JcrFolder targetIndexDir = localRepo.getLockedJcrFolder(getIndexFolderRepoPath(), true);
            targetIndexDir.mkdirs();
            InputStream indexInputStream = indexHandle.getInputStream();
            InputStream propertiesInputStream = propertiesHandle.getInputStream();
            //Copy to jcr, acquiring the lock as late as possible
            JcrFile indexFile = localRepo.getLockedJcrFile(new RepoPath(
                    targetIndexDir.getRepoPath(), MavenNaming.NEXUS_INDEX_ZIP), true);
            indexFile.fillData(indexInputStream);
            JcrFile propertiesFile = localRepo.getLockedJcrFile(new RepoPath(
                    targetIndexDir.getRepoPath(), MavenNaming.NEXUS_INDEX_PROPERTIES), true);
            propertiesFile.fillData(propertiesInputStream);
            log.info("Successfully saved index file '" + indexFile.getAbsolutePath() +
                    "' and index info '" + propertiesFile.getAbsolutePath() + "'.");
            return true;
        } catch (Exception e) {
            throw new RuntimeException("Failed to save index file for repo '" + localRepo + "'.", e);
        } finally {
            indexHandle.close();
            propertiesHandle.close();
        }
    }

    RepoPath getIndexFolderRepoPath() {
        return new RepoPath(
                localRepo.getRootFolder().getRepoPath(), MavenNaming.NEXUS_INDEX_DIR);
    }
}
