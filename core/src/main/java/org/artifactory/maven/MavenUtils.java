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

import org.apache.log4j.Logger;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.transform.SnapshotTransformation;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.common.ArtifactoryConstants;
import org.artifactory.repo.LocalRepo;
import org.artifactory.repo.service.InternalRepositoryService;
import org.artifactory.resource.ArtifactResource;
import org.artifactory.utils.PathUtils;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.springframework.util.StringUtils;

import javax.jcr.RepositoryException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public class MavenUtils {
    @SuppressWarnings({"UNUSED_SYMBOL", "UnusedDeclaration"})
    private final static Logger LOGGER = Logger.getLogger(MavenUtils.class);

    //Uses lazy evaluation of the version (+?)
    //see: http://www.regular-expressions.info/reference.html
    //For testing, see: http://www.cis.upenn.edu/~matuszek/General/RegexTester/regex-tester.html
    public static final Pattern ARTIFACT_NAME_PATTERN =
            Pattern.compile("(.+?)-(\\d.+?(-SNAPSHOT)?)(-([^-\\d]+))?\\.(\\w{3,}?)");

    public static final Pattern UNIQUE_SNAPSHOT_NAME_PATTERN =
            Pattern.compile("^(.*)-(([0-9]{8}.[0-9]{6})-([0-9]+))[\\.-].+$");

    public static final String METADATA_PREFIX = "maven-metadata";
    public static final String NEXUS_INDEX_DIR = ".index";
    public static final String NEXUS_INDEX_PREFIX = "nexus-maven-repository-index";
    public static final String NEXUS_INDEX_ZIP = NEXUS_INDEX_PREFIX + ".zip";
    public static final String NEXUS_INDEX_PROPERTIES = NEXUS_INDEX_PREFIX + ".properties";

    /**
     * Returns a matcher
     *
     * @param name The file name (with no preceding path) to match against
     * @return A RE matcher. If the matcher matches, then: group(1)=artifactId; group(2)=version;
     *         group(5)=classifier; group(6)=packaging.
     */
    @SuppressWarnings({"UnnecessaryLocalVariable"})
    public static Matcher artifactMatcher(String name) {
        Matcher matcher = ARTIFACT_NAME_PATTERN.matcher(name);
        return matcher;
    }

    public static boolean isVersionSnapshot(String version) {
        return version.endsWith(Artifact.SNAPSHOT_VERSION);
    }

    public static boolean isNonUniqueSnapshot(String path) {
        int idx = path.indexOf("-SNAPSHOT.");
        return idx > 0 && idx > path.lastIndexOf('/');
    }

    public static boolean isSnapshot(String path) {
        boolean result = isNonUniqueSnapshot(path);
        if (!result) {
            int versionIdx = path.indexOf("SNAPSHOT/");
            result = versionIdx > 0 && path.lastIndexOf('/') == versionIdx + 8;
        }
        return result;
    }

    public static boolean isRelease(String path) {
        return !isSnapshot(path) && !isMetadata(path) && !isChecksum(path);
    }

    public static boolean isMetadataChecksum(String path) {
        String name = PathUtils.getName(path);
        return isChecksum(name) && name.startsWith(METADATA_PREFIX);
    }

    public static boolean isChecksum(String path) {
        String name = PathUtils.getName(path);
        return isChecksumFilename(name);
    }

    public static boolean isChecksum(File path) {
        String name = path.getName();
        return isChecksumFilename(name);
    }

    private static boolean isChecksumFilename(String name) {
        return StringUtils.endsWithIgnoreCase(name, ".sha1") ||
                StringUtils.endsWithIgnoreCase(name, ".md5") ||
                StringUtils.endsWithIgnoreCase(name, ".asc");
    }

    public static boolean isHidden(String path) {
        return path.startsWith(".");
    }

    public static boolean isIndex(String path) {
        String name = PathUtils.getName(path);
        return name.startsWith(NEXUS_INDEX_PREFIX);
    }

    public static boolean isMetadata(String path) {
        String name = PathUtils.getName(path);
        return name.startsWith(METADATA_PREFIX) &&
                StringUtils.endsWithIgnoreCase(name, ".xml");
    }

    public static boolean isSnapshotMetadata(String path) {
        //*-SNAPSHOT/*maven-metadata.xml
        String parent = new File(path).getParent();
        return parent != null && parent.endsWith("-SNAPSHOT") && isMetadata(path);
    }

    public static boolean isJarVariant(String path) {
        String ext = PathUtils.getExtension(path);
        return ext != null &&
                (StringUtils.endsWithIgnoreCase(ext, "ar") || "jam".equalsIgnoreCase(ext) ||
                        "zip".equalsIgnoreCase(ext));
    }

    public static boolean isPom(String path) {
        return StringUtils.endsWithIgnoreCase(path, ".pom");
    }

    public static boolean isXml(String path) {
        return isPom(path) || StringUtils.endsWithIgnoreCase(path, ".xml");
    }

    public static void validatePomTargetPath(InputStream in, String relPath)
            throws IOException, RepositoryException {
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
                boolean snapshot = MavenUtils.isSnapshot(relPath);
                boolean versionSnapshot = MavenUtils.isVersionSnapshot(modelVersion);
                String pathPrefix = null;
                if (snapshot && !versionSnapshot) {
                    pathPrefix = groupId.replace('.', '/') + "/" + model.getArtifactId() + "/";
                } else if (StringUtils.hasLength(modelVersion)) {
                    pathPrefix = groupId.replace('.', '/') + "/" + model.getArtifactId() + "/" +
                            modelVersion;
                }
                //Do not validate paths that contain property references
                if (pathPrefix != null && !pathPrefix.contains("${")
                        && !StringUtils.startsWithIgnoreCase(relPath, pathPrefix)) {
                    final String msg = "The target deployment path '" + relPath +
                            "' does not match the POM's expected path prefix '" + pathPrefix +
                            "'. Please validate your POM's correctness and make sure the source " +
                            "path is a valid Maven 2 repository root.";
                    if (ArtifactoryConstants.suppressPomConsistencyChecks) {
                        LOGGER.error(
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

    /**
     * Converts a path to maven 1 path. For example org/apache/commons/commons-email/1.1/commons-email-1.1.jar
     * will result in org.apache.commons/jars/commons-email-1.1.jar
     *
     * @param path Path to convert
     * @return A Maven 1 repository path
     */
    public static String toMaven1Path(String path) {
        String[] pathElements = path.split("/");
        String name = pathElements[pathElements.length - 1];
        String fileExt;
        if (isChecksum(path)) {
            int lastPeriodIndex = name.lastIndexOf('.');
            fileExt = name.substring(name.lastIndexOf('.', lastPeriodIndex - 1) + 1,
                    lastPeriodIndex);
        } else {
            fileExt = name.substring(name.lastIndexOf('.') + 1);
        }

        // Get the group path (the path up until the artifact id)
        String groupPath = pathElements[0];
        for (int i = 1; i < pathElements.length - 3; i++) {
            groupPath += "." + pathElements[i];
        }
        String maven1Path = groupPath.replace('/', '.') + "/" + fileExt + "s/" + name;
        return maven1Path;
    }

    public static String getArtifactMetadataContent(ArtifactResource pa) {
        String repositoryKey = pa.getRepoKey();
        InternalRepositoryService repositoryService =
                (InternalRepositoryService) ContextHelper.get().getRepositoryService();
        LocalRepo repo = repositoryService.localOrCachedRepositoryByKey(repositoryKey);
        String pom = repo.getPomContent(pa);
        if (pom == null) {
            pom = "No POM file found for '" + pa.getName() + "'.";
        }
        String artifactMetadata = pa.getActualArtifactXml();
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
