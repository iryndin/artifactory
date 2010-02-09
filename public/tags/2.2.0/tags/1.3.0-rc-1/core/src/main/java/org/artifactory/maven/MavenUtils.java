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
package org.artifactory.maven;

import org.apache.maven.artifact.transform.SnapshotTransformation;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.api.maven.MavenNaming;
import org.artifactory.common.ConstantsValue;
import org.artifactory.repo.LocalRepo;
import org.artifactory.repo.service.InternalRepositoryService;
import org.artifactory.resource.ArtifactResource;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import javax.jcr.RepositoryException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.util.Date;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public class MavenUtils {
    private static final Logger log = LoggerFactory.getLogger(MavenUtils.class);

    public static void validatePomTargetPath(InputStream in, String relPath) throws IOException, RepositoryException {
        MavenXpp3Reader reader = new MavenXpp3Reader();
        try {
            Model model = reader.read(new InputStreamReader(in, "utf-8"));
            String groupId = model.getGroupId();
            if (StringUtils.hasLength(groupId)) {
                //Do not verify if the pom's groupid does not exist (inherited)
                String modelVersion = model.getVersion();
                //Version may come from the parent
                if (!StringUtils.hasLength(modelVersion)) {
                    Parent parent = model.getParent();
                    if (parent != null) {
                        modelVersion = parent.getVersion();
                    }
                }
                //For snapshots with unique snapshot version, do not include the model version in
                //the path
                boolean snapshot = MavenNaming.isSnapshot(relPath);
                boolean versionSnapshot = MavenNaming.isVersionSnapshot(modelVersion);
                String pathPrefix = null;
                if (snapshot && !versionSnapshot) {
                    pathPrefix = groupId.replace('.', '/') + "/" + model.getArtifactId() + "/";
                } else if (StringUtils.hasLength(modelVersion)) {
                    pathPrefix = groupId.replace('.', '/') + "/" + model.getArtifactId() + "/" + modelVersion;
                }
                //Do not validate paths that contain property references
                if (pathPrefix != null && !pathPrefix.contains("${")
                        && !StringUtils.startsWithIgnoreCase(relPath, pathPrefix)) {
                    final String msg = "The target deployment path '" + relPath +
                            "' does not match the POM's expected path prefix '" + pathPrefix +
                            "'. Please validate your POM's correctness and make sure the source " +
                            "path is a valid Maven 2 repository root.";
                    if (ConstantsValue.suppressPomConsistencyChecks.getBoolean()) {
                        log.error(
                                msg + " Using suppressed POM consistency checks: broken " +
                                        "artifacts might have been stored in your repository - " +
                                        "you should resolve this manually.");
                    } else {
                        throw new RepositoryException(
                                msg + " Some artifacts might have been incorrectly imported - " +
                                        "please remove them manually.");
                    }
                }
            }
        } catch (XmlPullParserException e) {
            throw new RepositoryException("Failed to read POM for '" + relPath + "'.");
        }
    }

    public static String dateToTimestamp(Date date) {
        return SnapshotTransformation.getUtcDateFormatter().format(date);
    }

    public static Date timestampToDate(String timestamp) {
        try {
            return SnapshotTransformation.getUtcDateFormatter().parse(timestamp);
        } catch (ParseException e) {
            throw new RuntimeException("Failed to transfor timestamp to date.", e);
        }
    }

    public static String getArtifactMetadataContent(ArtifactResource pa) {
        String repositoryKey = pa.getRepoPath().getRepoKey();
        InternalRepositoryService repositoryService =
                (InternalRepositoryService) ContextHelper.get().getRepositoryService();
        LocalRepo repo = repositoryService.localOrCachedRepositoryByKey(repositoryKey);
        String pom = repo.getPomContent(pa);
        if (pom == null) {
            pom = "No POM file found for '" + pa.getRepoPath().getName() + "'.";
        }
        String artifactMetadata = pa.getMavenInfo().getXml();
        StringBuilder result = new StringBuilder();
        if (artifactMetadata != null && artifactMetadata.trim().length() > 0) {
            result.append("------ ARTIFACT EFFECTIVE METADATA BEGIN ------\n")
                    .append(artifactMetadata)
                    .append("------- ARTIFACT EFFECTIVE METADATA END -------\n\n");
        }
        result.append(pom);
        return result.toString();
    }
}
