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
package org.artifactory.engine;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.artifact.repository.metadata.Snapshot;
import org.apache.maven.artifact.repository.metadata.Versioning;
import org.artifactory.api.common.StatusHolder;
import org.artifactory.api.fs.FileInfo;
import org.artifactory.api.fs.MetadataInfo;
import org.artifactory.api.maven.MavenArtifactInfo;
import org.artifactory.api.maven.MavenNaming;
import org.artifactory.api.mime.NamingUtils;
import org.artifactory.api.repo.RepoPath;
import org.artifactory.api.request.ArtifactoryRequest;
import org.artifactory.api.request.ArtifactoryResponse;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.descriptor.repo.SnapshotVersionBehavior;
import org.artifactory.maven.MavenUtils;
import org.artifactory.repo.LocalRepo;
import org.artifactory.repo.service.InternalRepositoryService;
import org.artifactory.resource.ArtifactResource;
import org.artifactory.resource.FileResource;
import org.artifactory.resource.MetadataResource;
import org.artifactory.resource.RepoResource;
import org.artifactory.spring.InternalContextHelper;
import org.artifactory.util.PathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.ui.basicauth.BasicProcessingFilterEntryPoint;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.List;

@Service
public class UploadServiceImpl implements InternalUploadService {
    private static final Logger log = LoggerFactory.getLogger(UploadServiceImpl.class);

    @Autowired
    private AuthorizationService authService;

    @Autowired
    private InternalRepositoryService repoService;

    @Autowired
    private BasicProcessingFilterEntryPoint authenticationEntryPoint;

    @SuppressWarnings({"OverlyComplexMethod"})
    public void process(ArtifactoryRequest request, ArtifactoryResponse response) throws IOException {
        log.debug("Request: source=" + request.getSourceDescription()
                + ", path=" + request.getPath() + ", lastModified=" + request.getLastModified()
                + ", ifModifiedSince=" + request.getIfModifiedSince());
        //Get the proper file repository for deployment from the path
        String repoKey = request.getRepoKey();
        //Sanity check
        if (repoKey == null) {
            String msg = "No target local repository specified in deploy request.";
            response.sendError(HttpStatus.SC_BAD_REQUEST, msg, log);
            return;
        }
        final LocalRepo repo = repoService.localOrCachedRepositoryByKey(repoKey);
        if (repo == null) {
            String msg = "Could not find a local repository named " + repoKey + " to deploy to.";
            response.sendError(HttpStatus.SC_BAD_REQUEST, msg, log);
            return;
        }
        String path = request.getPath();
        StatusHolder statusHolder = repoService.assertValidDeployPath(repo, path);
        if (statusHolder.isError()) {
            //Test if we need to require http authorization
            int returnCode = statusHolder.getStatusCode();
            if (returnCode == HttpStatus.SC_FORBIDDEN && authService.isAnonymous()) {
                //Transform a frobidden to unauthorized if received for an anonymous user
                String msg = statusHolder.getStatusMsg();
                String realmName = authenticationEntryPoint.getRealmName();
                response.sendAuthorizationRequired(msg, realmName);
            } else {
                response.sendError(statusHolder.getStatusCode(), statusHolder.getStatusMsg(), log);
            }
            return;
        }
        InternalUploadService transactionalMe = InternalContextHelper.get().beanForType(InternalUploadService.class);
        transactionalMe.doProcess(request, response, repo, path);
    }

    public void doProcess(ArtifactoryRequest request, ArtifactoryResponse response, LocalRepo repo, String path)
            throws IOException {
        if (NamingUtils.isChecksum(path)) {
            log.trace("Skipping deployment of checksum file {}", path);
            response.sendOk();
            return;
        }

        InputStream is = null;
        FileInfo dummyInfo = new FileInfo(new RepoPath(repo.getKey(), path));
        ArtifactResource ar = new ArtifactResource(dummyInfo);
        MavenArtifactInfo mavenInfo = ar.getMavenInfo();
        if (mavenInfo.isValid() && mavenInfo.isSnapshot()) {
            //See if the user requested deployment path is for a non-unique snapshot
            SnapshotVersionBehavior snapshotVersionBehavior = repo.getSnapshotVersionBehavior();
            boolean deployerUsedNonUniqueSnapshot = MavenNaming.isNonUniqueSnapshot(path);
            boolean nonUniqueSnapshotVersions =
                    snapshotVersionBehavior.equals(SnapshotVersionBehavior.NONUNIQUE) ||
                            (deployerUsedNonUniqueSnapshot &&
                                    snapshotVersionBehavior.equals(SnapshotVersionBehavior.DEPLOYER));
            path = adjustSnapshotPath(repo, path, mavenInfo, snapshotVersionBehavior, nonUniqueSnapshotVersions);
            if (NamingUtils.isSnapshotMetadata(path)) {
                Metadata metadata = adjustSnapshotMetadata(request, repo, path,
                        snapshotVersionBehavior, nonUniqueSnapshotVersions);
                String metadataStr = MavenUtils.mavenMetadataToString(metadata);
                is = new ByteArrayInputStream(metadataStr.getBytes("utf-8"));
            }
        }
        if (is == null) {
            is = request.getInputStream();
        }
        RepoResource res;
        RepoPath repoPath = new RepoPath(repo.getKey(), path);
        if (NamingUtils.isMetadata(path)) {
            res = new MetadataResource(new MetadataInfo(repoPath));
        } else {
            FileInfo fileInfo = new FileInfo(repoPath);
            fileInfo.createTrustedChecksums();
            res = new FileResource(fileInfo);
        }
        //Update the last modified
        long lastModified = request.getLastModified();
        if (lastModified > 0) {
            res.getInfo().setLastModified(lastModified);
        }
        try {
            repo.saveResource(res, is);
        } finally {
            IOUtils.closeQuietly(is);
        }
        //Send ok. Also for those artifacts that the wagon sent and we ignore (checksums etc.)
        response.sendOk();
    }

    private String adjustSnapshotPath(LocalRepo repo, String filePath, MavenArtifactInfo mavenInfo,
            SnapshotVersionBehavior snapshotVersionBehavior, boolean nonUniqueSnapshotVersions) {
        boolean realArtifact = !NamingUtils.isMetadata(filePath) && !NamingUtils.isChecksum(filePath);
        String adjustedPath = filePath;
        if (realArtifact) {
            //Check if needs to update a snapshot path according to the snapshot policy
            if (nonUniqueSnapshotVersions) {
                //Replace version timestamp with SNAPSHOT for non-unique snapshot repo
                String classifier = mavenInfo.getClassifier();
                adjustedPath = filePath.substring(0, getLastPathSeperatorIndex(filePath) + 1) +
                        mavenInfo.getArtifactId() + "-" + mavenInfo.getVersion() +
                        (StringUtils.isEmpty(classifier) ? "" : "-" + classifier) + "." + mavenInfo.getType();
            } else if (snapshotVersionBehavior.equals(SnapshotVersionBehavior.UNIQUE)) {
                //Replace SNAPSHOT with version timestamp for unique snapshot repo
                String fileName = PathUtils.getName(filePath);
                if (!MavenNaming.isUniqueSnapshotFileName(fileName)) {
                    adjustedPath = adjustNonUniqueSnapshotToUnique(repo, filePath, mavenInfo);
                }
            }
        }
        if (!adjustedPath.equals(filePath)) {
            log.debug("File path {} adjusted to: {}", filePath, adjustedPath);
        }

        return adjustedPath;
    }

    private int getLastPathSeperatorIndex(String filePath) {
        return filePath.lastIndexOf('/');
    }

    private String adjustNonUniqueSnapshotToUnique(LocalRepo repo, String filePath, MavenArtifactInfo mavenInfo) {
        //Get the latest build number from the metadata
        String versionMetadataPath = PathUtils.getParent(filePath) + "/maven-metadata.xml";
        int metadataBuildNumber = getLastBuildNumber(repo, versionMetadataPath);
        int nextBuildNumber = metadataBuildNumber + 1;

        RepoPath parentPath = new RepoPath(repo.getKey(), PathUtils.getParent(filePath));

        // determine if the next build number should be the one read from the metadata
        String classifier = mavenInfo.getClassifier();
        if (metadataBuildNumber > 0 && !StringUtils.isEmpty(classifier)) {
            // artifacts with clasifier are always deployed after the maven-metadata.xml
            // so use the metadata build number
            nextBuildNumber = metadataBuildNumber;
        }
        if (metadataBuildNumber > 0 && MavenNaming.isPom(filePath)) {
            // the metadata might already represent an existing main artifact (in case the
            // maven-metadata.xml was deployed after the main artifact and before the pom/classifier)
            // so first check if there's already a file with the next build number
            if (getSnapshotFile(parentPath, metadataBuildNumber + 1) == null) {
                // no files deployed with the next build number so either this is a pom deployment (parent pom)
                // or this is a pom of a main artifact for which the maven-metadata.xml was deployed before this pom
                if (getSnapshotPomFile(parentPath, metadataBuildNumber) == null) {
                    // this is a pom attached to a main artifact deployed after maven-metadata.xml
                    nextBuildNumber = metadataBuildNumber;
                }
            }
        }

        String timestamp = getSnapshotTimestamp(parentPath, nextBuildNumber);
        if (timestamp == null) {
            // probably the first deployed file for this build, use now for the timestamp
            timestamp = MavenUtils.dateToTimestamp(new Date());
        }

        // replace the SNAPSHOT string with timestamp-buildNumber
        String version = mavenInfo.getVersion();
        String uniqueVersion = version.substring(0, version.lastIndexOf("SNAPSHOT")) +
                timestamp + "-" + nextBuildNumber;
        String adjustedPath = filePath.substring(0, getLastPathSeperatorIndex(filePath) + 1) +
                mavenInfo.getArtifactId() + "-" + uniqueVersion +
                (StringUtils.isEmpty(classifier) ? "" : "-" + classifier) + "." + mavenInfo.getType();
        return adjustedPath;
    }

    private Metadata adjustSnapshotMetadata(ArtifactoryRequest request, LocalRepo repo, String snapshotMetadataPath,
            SnapshotVersionBehavior snapshotVersionBehavior, boolean nonUniqueSnapshotVersions) throws IOException {
        Metadata metadata = MavenUtils.toMavenMetadata(request.getInputStream());
        Versioning versioning = metadata.getVersioning();
        if (versioning != null) {
            Snapshot snap = versioning.getSnapshot();
            if (nonUniqueSnapshotVersions && snap != null && snap.getTimestamp() != null) {
                //For specific version metadata, remove the <timestamp> tag. This is
                //necessary for plugin dependencies with no specified version, to be
                //able to resovle the latest artifact (otherwise they will try to locate
                //a file name based on the timestamp).
                snap.setTimestamp(null);
            } else if (snapshotVersionBehavior.equals(SnapshotVersionBehavior.UNIQUE)) {
                //Update the build number if we force unique snapshot versions
                if (snap == null) {
                    snap = new Snapshot();
                }

                int metadataBuildNumber = getLastBuildNumber(repo, snapshotMetadataPath);
                int nextBuildNumber = metadataBuildNumber + 1;
                snap.setBuildNumber(nextBuildNumber);

                // If the timestamp is null (for example if distribution management uniqueVersion set to false)
                if (snap.getTimestamp() == null) {
                    String parentPath = PathUtils.getParent(snapshotMetadataPath);
                    RepoPath repoPath = new RepoPath(repo.getKey(), parentPath);
                    // get the timestamp from the deployed filename with the same build number
                    String timestamp = getSnapshotTimestamp(repoPath, nextBuildNumber);
                    snap.setTimestamp(timestamp);
                }
            }
        }
        return metadata;
    }

    /**
     * @param repo                The repository to deploy to
     * @param versionMetadataPath Path to a snapshot maven metadata (ie ends with '-SNAPSHOT/maven-metadata.xml')
     * @return The last build number for snapshot version. 0 if maven-metadata not found for the path.
     */
    private int getLastBuildNumber(final LocalRepo repo, final String versionMetadataPath) {
        int buildNumber = 0;
        try {
            // get the parent path (remove the maven-metadata.xml)
            String parentPath = PathUtils.getParent(versionMetadataPath);
            RepoPath repoPath = new RepoPath(repo.getKey(), parentPath);
            if (repoService.exists(repoPath) &&
                    repoService.hasXmlMetdata(repoPath, MavenNaming.MAVEN_METADATA_NAME)) {
                String mavenMetadataStr = repoService.getXmlMetadata(repoPath, MavenNaming.MAVEN_METADATA_NAME);
                Metadata metadata = MavenUtils.toMavenMetadata(mavenMetadataStr);
                Versioning versioning = metadata.getVersioning();
                if (versioning != null) {
                    Snapshot snapshot = versioning.getSnapshot();
                    if (snapshot != null) {
                        buildNumber = snapshot.getBuildNumber();
                    }
                }
            } else {
                // ok probably not found. just log
                log.debug("No maven metadata found for {}.", versionMetadataPath);
            }
        } catch (Exception e) {
            log.error("Cannot obtain build number from metadata.", e);
        }
        return buildNumber;
    }

    /**
     * @param snapshotDirectoryPath Path to a repository snapshot directory (eg, /a/path/1.0-SNAPSHOT)
     * @param buildNum              The file with build number to search for
     * @return The timestamp of the unique snapshot file with the input build number.
     */
    private String getSnapshotTimestamp(RepoPath snapshotDirectoryPath, int buildNum) {
        String snapshotFile = getSnapshotFile(snapshotDirectoryPath, buildNum);
        if (snapshotFile != null) {
            int childBuildNumber = MavenNaming.getUniqueSnapshotVersionBuildNumber(snapshotFile);
            if (childBuildNumber == buildNum) {
                String timestamp = MavenNaming.getUniqueSnapshotVersionTimestamp(snapshotFile);
                log.debug("Extracted timestamp {} from {}", timestamp, snapshotFile);
                return timestamp;
            }
        }
        log.debug("Snapshot timestamp not found in {} for build number {}", snapshotDirectoryPath, buildNum);
        return null;
    }

    /**
     * @param snapshotDirectoryPath Path to a repository snapshot directory (eg, /a/path/1.0-SNAPSHOT)
     * @param buildNum              The file with build number to search for
     * @return The path of the first unique snapshot file with the input build number.
     */
    private String getSnapshotFile(RepoPath snapshotDirectoryPath, int buildNum) {
        return getSnapshotFile(snapshotDirectoryPath, buildNum, null);
    }

    /**
     * @param snapshotDirectoryPath Path to a repository snapshot directory (eg, /a/path/1.0-SNAPSHOT)
     * @param buildNum              The file with build number to search for
     * @return The path of the unique snapshot pom file with the input build number.
     */
    private String getSnapshotPomFile(RepoPath snapshotDirectoryPath, int buildNum) {
        return getSnapshotFile(snapshotDirectoryPath, buildNum, "pom");
    }

    /**
     * @param snapshotDirectoryPath Path to a repository snapshot directory (eg, /a/path/1.0-SNAPSHOT)
     * @param buildNum              The file with build number to search for
     * @param fileType              The file type to search for. Use null for any type
     * @return The path of the first unique snapshot file with the input build number.
     */
    private String getSnapshotFile(RepoPath snapshotDirectoryPath, int buildNum, String fileType) {
        log.debug("Searching for unique snapshot file in {} with build number {}",
                snapshotDirectoryPath, buildNum);
        if (repoService.exists(snapshotDirectoryPath)) {
            List<String> chidren = repoService.getChildrenNames(snapshotDirectoryPath);
            for (String child : chidren) {
                if (fileType == null || fileType.equals(PathUtils.getExtension(child)) &&
                        MavenNaming.isUniqueSnapshotFileName(child)) {
                    int childBuildNumber = MavenNaming.getUniqueSnapshotVersionBuildNumber(child);
                    if (childBuildNumber == buildNum) {
                        log.debug("Found unique snapshot with build number {}: {}", buildNum, child);
                        return child;
                    }
                }
            }
        }
        log.debug("Unique snapshot file not found in {} for build number {}", snapshotDirectoryPath, buildNum);
        return null;
    }
}
