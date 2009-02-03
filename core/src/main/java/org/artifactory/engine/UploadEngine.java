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
import org.apache.log4j.Logger;
import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.artifact.repository.metadata.Snapshot;
import org.apache.maven.artifact.repository.metadata.Versioning;
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Reader;
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Writer;
import org.artifactory.api.common.StatusHolder;
import org.artifactory.api.fs.FileInfo;
import org.artifactory.api.repo.RepoPath;
import org.artifactory.api.repo.exception.FileExpectedException;
import org.artifactory.api.request.ArtifactoryRequest;
import org.artifactory.api.request.ArtifactoryResponse;
import org.artifactory.api.request.UploadService;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.descriptor.repo.SnapshotVersionBehavior;
import org.artifactory.jcr.fs.JcrFile;
import org.artifactory.jcr.md.MetadataService;
import org.artifactory.maven.MavenUtils;
import org.artifactory.repo.LocalRepo;
import org.artifactory.repo.service.InternalRepositoryService;
import org.artifactory.resource.ArtifactResource;
import org.artifactory.resource.SimpleRepoResource;
import org.artifactory.security.AccessLogger;
import org.artifactory.spring.InternalContextHelper;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.util.Date;

@Service
public class UploadEngine implements UploadService {
    private static final Logger LOGGER = Logger.getLogger(UploadEngine.class);

    @Autowired
    private AuthorizationService authService;

    @Autowired
    private InternalRepositoryService repoService;


    @SuppressWarnings({"OverlyComplexMethod"})
    @Transactional
    public void process(ArtifactoryRequest request, ArtifactoryResponse response)
            throws IOException {
        LOGGER.debug("Request: source=" + request.getSourceDescription()
                + ", path=" + request.getPath() + ", lastModified=" + request.getLastModified()
                + ", ifModifiedSince=" + request.getIfModifiedSince());
        //Get the proper file repository for deployment from the path
        String repoKey = request.getRepoKey();
        //Sanity check
        if (repoKey == null) {
            LOGGER.error("No target local repository specified in deploy request.");
            response.sendError(HttpStatus.SC_BAD_REQUEST);
            return;
        }
        final LocalRepo repo = repoService.localOrCachedRepositoryByKey(repoKey);
        if (repo == null) {
            LOGGER.warn("Could not find a local repository named " + repoKey + " to deploy to.");
            response.sendError(HttpStatus.SC_BAD_REQUEST);
            return;
        }
        String path = request.getPath();
        StatusHolder statusHolder = repoService.assertValidDeployPath(repo, path);
        if (statusHolder.isError()) {
            LOGGER.warn("Upload rejected: " + statusHolder.getStatusMsg());
            response.sendError(statusHolder.getStatusCode());
            return;
        }
        //Check security
        RepoPath repoPath = new RepoPath(repo.getKey(), path);
        if (!authService.canDeploy(repoPath)) {
            LOGGER.warn("Upload rejected: not permitted to deploy '" + path + "' into '" +
                    repoKey + "'.");
            response.sendError(HttpStatus.SC_FORBIDDEN);
            AccessLogger.deployDenied(repoPath);
            return;
        }
        // Lock early, and will rollback
        MetadataService md = InternalContextHelper.get().beanForType(MetadataService.class);
        md.lockCreateIfEmpty(FileInfo.class, repo.getRepoRootPath() + "/" + path);
        InputStream is = null;
        ArtifactResource ar = new ArtifactResource(repo, path);
        boolean snapshot = ar.isSnapshot();
        if (snapshot) {
            boolean realArtifact = !MavenUtils.isMetadata(path) && !MavenUtils.isChecksum(path);
            //See if the user requested deployment path is for a non-unique snapshot
            boolean deployerUsedNonUniqueSnapshot = MavenUtils.isNonUniqueSnapshot(path);
            SnapshotVersionBehavior snapshotVersionBehavior = repo.getSnapshotVersionBehavior();
            boolean nonUniqueSnapshotVersions =
                    snapshotVersionBehavior.equals(SnapshotVersionBehavior.NONUNIQUE) ||
                            (deployerUsedNonUniqueSnapshot &&
                                    snapshotVersionBehavior
                                            .equals(SnapshotVersionBehavior.DEPLOYER));
            if (realArtifact) {
                String artifactId = ar.getArtifactId();
                String version = ar.getVersion();
                int lastSepIdx = path.lastIndexOf('/');
                String type = ar.getType();
                String classifier = ar.getClassifier();
                if (nonUniqueSnapshotVersions) {
                    //Replace version timestamp with SNAPSHOT for non-unique snapshot repo
                    path = path.substring(0, lastSepIdx + 1) + artifactId + "-" + version +
                            (StringUtils.isEmpty(classifier) ? "" : "-" + classifier) + "." + type;
                } else if (snapshotVersionBehavior.equals(SnapshotVersionBehavior.UNIQUE)) {
                    //Replace SNAPSHOT with version timestamp for unique snapshot repo
                    String timestamp = MavenUtils.dateToTimestamp(new Date());
                    //Get the latest build number from the metadata
                    final String versionMetadataPath =
                            path.substring(0, path.lastIndexOf("-SNAPSHOT/") + 10) +
                                    "maven-metadata.xml";
                    int buildNum = getLastBuildNumber(repo, versionMetadataPath);
                    String uniqueVersion =
                            version.substring(0, version.lastIndexOf("SNAPSHOT")) + timestamp +
                                    "-" + (buildNum + 1);
                    path = path.substring(0, lastSepIdx + 1) + artifactId + "-" + uniqueVersion +
                            (StringUtils.isEmpty(classifier) ? "" : "-" + classifier) + "." + type;
                }
            }
            if (MavenUtils.isSnapshotMetadata(path)) {
                InputStream requestInputStream = null;
                try {
                    MetadataXpp3Reader reader = new MetadataXpp3Reader();
                    requestInputStream = request.getInputStream();
                    Metadata metadata;
                    try {
                        metadata = reader.read(new InputStreamReader(requestInputStream, "utf-8"));
                    } catch (XmlPullParserException e) {
                        throw new IOException(e.getMessage());
                    }
                    Versioning versioning = metadata.getVersioning();
                    if (versioning != null) {
                        Snapshot snap = versioning.getSnapshot();
                        if (nonUniqueSnapshotVersions && snap != null &&
                                snap.getTimestamp() != null) {
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
                            int buildNum = getLastBuildNumber(repo, path);
                            snap.setBuildNumber(buildNum + 1);
                        }
                    }
                    MetadataXpp3Writer writer = new MetadataXpp3Writer();
                    StringWriter stringWriter = new StringWriter();
                    writer.write(stringWriter, metadata);
                    is = new ByteArrayInputStream(stringWriter.toString().getBytes("utf-8"));
                } finally {
                    IOUtils.closeQuietly(requestInputStream);
                }
            } else if (MavenUtils.isChecksum(path)) {
                //Ignore checksum uploads.
                //We will end up with broken signatures here since we crafted the metadtata (for
                //snapshots, at least): We will reurn the calculated checksum created upon
                //file storage in jcr
                response.sendOk();
                return;
            }
        }
        if (is == null) {
            is = request.getInputStream();
        }
        SimpleRepoResource res = new SimpleRepoResource(repo, path);
        try {
            res.setLastModified(System.currentTimeMillis());
            repo.saveResource(res, is);
        } finally {
            IOUtils.closeQuietly(is);
        }
        //Send ok. Also for those artifacts that the wagon sent and we ignore (md5's etc.)
        response.sendOk();
    }

    private static int getLastBuildNumber(final LocalRepo repo, final String versionMetadataPath) {
        int buildNumber = 0;
        JcrFile file = null;
        try {
            file = repo.getLockedJcrFile(versionMetadataPath);
        } catch (FileExpectedException e) {
            LOGGER.error("Cannot obtain build number from a folder " + versionMetadataPath, e);
        }
        //If there is an item from which a buildNumber can be obtained use it, else we start at 0
        if (file != null) {
            InputStream inputStream = null;
            try {
                MetadataXpp3Reader reader = new MetadataXpp3Reader();
                inputStream = file.getStream();
                Metadata metadata =
                        reader.read(new InputStreamReader(inputStream, "utf-8"));
                Versioning versioning = metadata.getVersioning();
                if (versioning != null) {
                    Snapshot snap = versioning.getSnapshot();
                    if (snap != null) {
                        buildNumber = snap.getBuildNumber();
                    }
                }
            } catch (Exception e) {
                LOGGER.warn("Cannot obtain build number from metadata.", e);
            } finally {
                IOUtils.closeQuietly(inputStream);
            }
        }
        return buildNumber;
    }
}
