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
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Reader;
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Writer;
import org.artifactory.api.common.StatusHolder;
import org.artifactory.api.fs.FileInfo;
import org.artifactory.api.fs.MetadataInfo;
import org.artifactory.api.maven.MavenArtifactInfo;
import org.artifactory.api.maven.MavenNaming;
import org.artifactory.api.mime.NamingUtils;
import org.artifactory.api.repo.RepoPath;
import org.artifactory.api.repo.exception.FileExpectedException;
import org.artifactory.api.request.ArtifactoryRequest;
import org.artifactory.api.request.ArtifactoryResponse;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.descriptor.repo.SnapshotVersionBehavior;
import org.artifactory.jcr.fs.JcrFile;
import org.artifactory.maven.MavenUtils;
import org.artifactory.repo.LocalRepo;
import org.artifactory.repo.service.InternalRepositoryService;
import org.artifactory.resource.ArtifactResource;
import org.artifactory.resource.FileResource;
import org.artifactory.resource.MetadataResource;
import org.artifactory.resource.RepoResource;
import org.artifactory.spring.InternalContextHelper;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.ui.basicauth.BasicProcessingFilterEntryPoint;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.util.Date;

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
        InputStream is = null;
        FileInfo dummyInfo = new FileInfo(new RepoPath(repo.getKey(), path));
        ArtifactResource ar = new ArtifactResource(dummyInfo);
        MavenArtifactInfo mavenInfo = ar.getMavenInfo();
        if (mavenInfo.isValid() && mavenInfo.isSnapshot()) {
            //See if the user requested deployment path is for a non-unique snapshot
            boolean deployerUsedNonUniqueSnapshot = MavenNaming.isNonUniqueSnapshot(path);
            SnapshotVersionBehavior snapshotVersionBehavior = repo.getSnapshotVersionBehavior();
            boolean nonUniqueSnapshotVersions =
                    snapshotVersionBehavior.equals(SnapshotVersionBehavior.NONUNIQUE) ||
                            (deployerUsedNonUniqueSnapshot &&
                                    snapshotVersionBehavior.equals(SnapshotVersionBehavior.DEPLOYER));
            path = adjustSnapshotPath(repo, path, mavenInfo, snapshotVersionBehavior, nonUniqueSnapshotVersions);
            if (NamingUtils.isSnapshotMetadata(path)) {
                Metadata metadata =
                        adjustSnapshotMetadata(request, repo, path, snapshotVersionBehavior, nonUniqueSnapshotVersions);
                MetadataXpp3Writer writer = new MetadataXpp3Writer();
                StringWriter stringWriter = new StringWriter();
                writer.write(stringWriter, metadata);
                is = new ByteArrayInputStream(stringWriter.toString().getBytes("utf-8"));
            } else if (NamingUtils.isChecksum(path)) {
                response.sendOk();
                return;
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

    private String adjustSnapshotPath(LocalRepo repo, String path, MavenArtifactInfo mavenInfo,
            SnapshotVersionBehavior snapshotVersionBehavior, boolean nonUniqueSnapshotVersions) {
        boolean realArtifact = !NamingUtils.isMetadata(path) && !NamingUtils.isChecksum(path);
        if (realArtifact) {
            //Check if needs to update a snapshot path according to the snapshot policy
            String artifactId = mavenInfo.getArtifactId();
            String version = mavenInfo.getVersion();
            int lastSepIdx = path.lastIndexOf('/');
            String type = mavenInfo.getType();
            String classifier = mavenInfo.getClassifier();
            if (nonUniqueSnapshotVersions) {
                //Replace version timestamp with SNAPSHOT for non-unique snapshot repo
                path = path.substring(0, lastSepIdx + 1) + artifactId + "-" + version +
                        (StringUtils.isEmpty(classifier) ? "" : "-" + classifier) + "." + type;
            } else if (snapshotVersionBehavior.equals(SnapshotVersionBehavior.UNIQUE)) {
                //Replace SNAPSHOT with version timestamp for unique snapshot repo
                String timestamp = MavenUtils.dateToTimestamp(new Date());
                //Get the latest build number from the metadata
                final String versionMetadataPath = path.substring(0, path.lastIndexOf("-SNAPSHOT/") + 10) +
                        "maven-metadata.xml";
                int buildNum = getLastBuildNumber(repo, versionMetadataPath);
                String uniqueVersion = version.substring(0, version.lastIndexOf("SNAPSHOT")) + timestamp +
                        "-" + (buildNum + 1);
                path = path.substring(0, lastSepIdx + 1) + artifactId + "-" + uniqueVersion +
                        (StringUtils.isEmpty(classifier) ? "" : "-" + classifier) + "." + type;
            }
        }
        return path;
    }

    private Metadata adjustSnapshotMetadata(ArtifactoryRequest request, LocalRepo repo, String path,
            SnapshotVersionBehavior snapshotVersionBehavior, boolean nonUniqueSnapshotVersions) throws IOException {
        InputStream requestInputStream = null;
        Metadata metadata;
        try {
            MetadataXpp3Reader reader = new MetadataXpp3Reader();
            requestInputStream = request.getInputStream();
            try {
                metadata = reader.read(new InputStreamReader(requestInputStream, "utf-8"));
            } catch (XmlPullParserException e) {
                throw new IOException(e.getMessage());
            }
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
                    int buildNum = getLastBuildNumber(repo, path);
                    snap.setBuildNumber(buildNum + 1);
                }
            }
        } finally {
            IOUtils.closeQuietly(requestInputStream);
        }
        return metadata;
    }

    private static int getLastBuildNumber(final LocalRepo repo, final String versionMetadataPath) {
        int buildNumber = 0;
        JcrFile file = null;
        try {
            file = repo.getLockedJcrFile(versionMetadataPath, false);
        } catch (FileExpectedException e) {
            log.error("Cannot obtain build number from a folder " + versionMetadataPath, e);
        }
        //If there is an item from which a buildNumber can be obtained use it, else we start at 0
        if (file != null) {
            InputStream inputStream = null;
            try {
                MetadataXpp3Reader reader = new MetadataXpp3Reader();
                inputStream = file.getStream();
                Metadata metadata = reader.read(new InputStreamReader(inputStream, "utf-8"));
                Versioning versioning = metadata.getVersioning();
                if (versioning != null) {
                    Snapshot snap = versioning.getSnapshot();
                    if (snap != null) {
                        buildNumber = snap.getBuildNumber();
                    }
                }
            } catch (Exception e) {
                log.warn("Cannot obtain build number from metadata.", e);
            } finally {
                IOUtils.closeQuietly(inputStream);
            }
        }
        return buildNumber;
    }
}
