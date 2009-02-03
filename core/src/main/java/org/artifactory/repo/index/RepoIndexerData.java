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

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.artifactory.common.ResourceStreamHandle;
import org.artifactory.jcr.fs.JcrFile;
import org.artifactory.jcr.fs.JcrFolder;
import org.artifactory.maven.MavenUtils;
import org.artifactory.repo.LocalRepo;
import org.artifactory.repo.RealRepo;
import org.artifactory.repo.RemoteRepo;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;

/**
 * User: freds Date: Aug 13, 2008 Time: 12:08:30 PM
 */
public class RepoIndexerData {
    private static final Logger LOGGER =
            LogManager.getLogger(RepoIndexerData.class);

    final RealRepo indexedRepo;
    boolean indexed = false;
    LocalRepo localRepo;
    ResourceStreamHandle indexHandle;
    ResourceStreamHandle propertiesHandle;

    RepoIndexerData(RealRepo indexedRepo) {
        this.indexedRepo = indexedRepo;
    }

    void findIndex() {
        //For remote repositories, try to download the remote cache, if failes index locally
        if (!indexedRepo.isLocal()) {
            RemoteRepo remoteRepo = (RemoteRepo) indexedRepo;
            localRepo = remoteRepo.getLocalCacheRepo();
            String indexPath = ".index/" + MavenUtils.NEXUS_INDEX_ZIP;
            String propertiesPath = ".index/" + MavenUtils.NEXUS_INDEX_PROPERTIES;
            try {
                indexHandle = remoteRepo.retrieveResource(indexPath);
                propertiesHandle = remoteRepo.retrieveResource(propertiesPath);
                indexed = true;
            } catch (IOException e) {
                if (indexHandle != null) {
                    indexHandle.close();
                }
                if (propertiesHandle != null) {
                    propertiesHandle.close();
                }
                LOGGER.warn("Could not retrieve remote nexus index '" + indexPath +
                        "' for repo '" + indexedRepo + "'.");
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

    void saveIndexFiles() {
        //Create the new jcr files for index and properties
        try {
            //Create the index dir
            JcrFolder targetIndexDir =
                    new JcrFolder(localRepo.getRootFolder(), MavenUtils.NEXUS_INDEX_DIR);
            targetIndexDir.mkdirs();
            InputStream indexInputStream = indexHandle.getInputStream();
            InputStream propertiesInputStream = propertiesHandle.getInputStream();
            JcrFile indexFile = new JcrFile(
                    targetIndexDir, MavenUtils.NEXUS_INDEX_ZIP, indexInputStream);
            JcrFile propertiesFile = new JcrFile(
                    targetIndexDir, MavenUtils.NEXUS_INDEX_PROPERTIES, propertiesInputStream);
            LOGGER.info(
                    "Successfully saved index file '" + indexFile.getAbsolutePath() +
                            "' and index info '" + propertiesFile.getAbsolutePath() + "'.");
        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to save index file for repo '" + localRepo + "'.", e);
        } finally {
            indexHandle.close();
            propertiesHandle.close();
        }
    }
}
