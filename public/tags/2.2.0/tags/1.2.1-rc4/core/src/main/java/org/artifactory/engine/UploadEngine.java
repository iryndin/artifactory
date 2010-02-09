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
import org.apache.maven.artifact.deployer.ArtifactDeploymentException;
import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.artifact.repository.metadata.Snapshot;
import org.apache.maven.artifact.repository.metadata.Versioning;
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Reader;
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Writer;
import org.artifactory.maven.MavenUtils;
import org.artifactory.repo.CentralConfig;
import org.artifactory.repo.LocalRepo;
import org.artifactory.request.ArtifactoryRequest;
import org.artifactory.request.ArtifactoryResponse;
import org.artifactory.resource.ArtifactResource;
import org.artifactory.resource.SimpleRepoResource;
import org.artifactory.security.RepoPath;
import org.artifactory.security.SecurityHelper;
import org.artifactory.spring.ArtifactoryContext;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.security.NoSuchAlgorithmException;

public class UploadEngine {
    private static final Logger LOGGER = Logger.getLogger(UploadEngine.class);

    private final ArtifactoryContext context;

    public UploadEngine(ArtifactoryContext context) {
        this.context = context;
    }

    public void process(ArtifactoryRequest request, ArtifactoryResponse response)
            throws IOException, ArtifactDeploymentException, XmlPullParserException,
            NoSuchAlgorithmException {
        LOGGER.debug("Request: source=" + request.getSourceDescription()
                + ", path=" + request.getPath() + ", lastModified=" + request.getLastModified()
                + ", ifModifiedSince=" + request.getIfModifiedSince());
        //Get the proper file repository for deployment from the path
        String repoKey = request.getTargetRepoGroup();
        //Sanity check
        if (repoKey == null) {
            LOGGER.error("No target local repository specified in deploy request.");
            response.sendError(HttpStatus.SC_BAD_REQUEST);
            return;
        }
        CentralConfig cc = context.getCentralConfig();
        LocalRepo repo = cc.localOrCachedRepositoryByKey(repoKey);
        if (repo == null) {
            LOGGER.warn("Could not find a local repository named " + repoKey + " to deploy to.");
            response.sendError(HttpStatus.SC_BAD_REQUEST);
            return;
        }
        if (repo.isBlackedOut()) {
            LOGGER.warn("Upload rejected: '" + repoKey + "' is blacked out.");
            response.sendError(HttpStatus.SC_FORBIDDEN);
            return;
        }
        String path = request.getPath();
        if (!repo.accept(path)) {
            LOGGER.warn("Upload rejected: '" + path + "' rejected by '" + repoKey +
                    "' include/exclude patterns.");
            response.sendError(HttpStatus.SC_FORBIDDEN);
            return;
        }
        ArtifactResource ar = new ArtifactResource(repo, path);
        if (!repo.handle(ar)) {
            LOGGER.warn("Upload rejected: '" + path + "' not handled by '" + repoKey + "'.");
            response.sendError(HttpStatus.SC_FORBIDDEN);
            return;
        }
        //Check security
        RepoPath repoPath = new RepoPath(repo.getKey(), path);
        SecurityHelper security = context.getSecurity();
        if (!security.canDeploy(repoPath)) {
            LOGGER.warn("Upload rejected: not permitted to deploy '" + path + "' into '" +
                    repoKey + "'.");
            response.sendError(HttpStatus.SC_FORBIDDEN);
            return;
        }
        //Replace version timestamp with SNAPSHOT for non-unique snapshot repo
        boolean snapshot = ar.isSnapshot();
        boolean realArtifact = !MavenUtils.isMetaData(path) && !MavenUtils.isChecksum(path);
        if (snapshot && !repo.isUseSnapshotUniqueVersions() && realArtifact) {
            String artifactId = ar.getArtifactId();
            String version = ar.getVersion();
            int lastSepIdx = path.lastIndexOf('/');
            String type = ar.getType();
            String classifier = ar.getClassifier();
            path = path.substring(0, lastSepIdx + 1) + artifactId + "-" + version +
                    (StringUtils.isEmpty(classifier) ? "" : "-" + classifier) + "." + type;
        }
        InputStream is = null;
        if (snapshot && !repo.isUseSnapshotUniqueVersions()) {
            if (path.endsWith("-SNAPSHOT/maven-metadata.xml")) {
                //For specific version metadata, remove the <timestamp> tag. This is necessary for
                //plugin dependencies with no specified version, to be able to resovle the latest
                //artifact (otherwise they will try to locate a file name based on the timestamp).
                InputStream requestInputStream = null;
                try {
                    MetadataXpp3Reader reader = new MetadataXpp3Reader();
                    requestInputStream = request.getInputStream();
                    Metadata metadata =
                            reader.read(new InputStreamReader(requestInputStream, "utf-8"));
                    Versioning versioning = metadata.getVersioning();
                    if (versioning != null) {
                        Snapshot snap = versioning.getSnapshot();
                        if (snap != null && snap.getTimestamp() != null) {
                            snap.setTimestamp(null);
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
                //snapshots, at least): . We will reurn the calculated metadata created upon
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
            res.setLastModifiedTime(System.currentTimeMillis());
            repo.saveResource(res, is, true);
        } finally {
            IOUtils.closeQuietly(is);
        }
        //Send ok. Also for those artifacts that the wagon sent and we ignore (md5's etc.)
        response.sendOk();
    }
}
