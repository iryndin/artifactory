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
import org.apache.maven.embedder.MavenEmbedder;
import org.apache.maven.embedder.MavenEmbedderConsoleLogger;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.artifactory.utils.MimeTypes;
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
    public static final Pattern ARTIFACT_FILENAME_PATTERN =
            Pattern.compile("(.+?)-(\\d.+?(-SNAPSHOT)?)(-([^-\\d]+))?\\.(\\w{3,}?)");

    public static final Pattern UNIQUE_SNAPSHOT_NAME_PATTERN =
            Pattern.compile("^(.*)-(([0-9]{8}.[0-9]{6})-([0-9]+))[\\.-].+$");

    public static final String METADATA_PREFIX = "maven-metadata";

    public static MavenEmbedder createMavenEmbedder() {
        /*ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        MavenEmbedder maven;
        try {
            maven = new MavenEmbedder(classLoader);
        } catch (MavenEmbedderException e) {
            throw new RuntimeException("Failed to create the MavenEmbedder.", e);
        }*/
        MavenEmbedder maven = new MavenEmbedder();
        maven.setLogger(new MavenEmbedderConsoleLogger());
        return maven;
    }

    /**
     * Returns a matcher
     *
     * @param fileName The file name (with no preceding path) to match against
     * @return A RE matcher. If the matcher matches, then: group(1)=artifactId; group(2)=version;
     *         group(5)=classifier; group(6)=packaging.
     */
    @SuppressWarnings({"UnnecessaryLocalVariable"})
    public static Matcher artifactMatcher(String fileName) {
        Matcher matcher = ARTIFACT_FILENAME_PATTERN.matcher(fileName);
        return matcher;
    }

    public static boolean isVersionSnapshot(String version) {
        return version.endsWith(Artifact.SNAPSHOT_VERSION);
    }

    public static boolean isNonUniqueSnapshot(String pathInfo) {
        int idx = pathInfo.indexOf("-SNAPSHOT.");
        return idx > 0 && idx > pathInfo.lastIndexOf('/');
    }

    public static boolean isSnapshot(String pathInfo) {
        boolean result = isNonUniqueSnapshot(pathInfo);
        if (!result) {
            int versionIdx = pathInfo.indexOf("SNAPSHOT/");
            result = versionIdx > 0 && pathInfo.lastIndexOf('/') == versionIdx + 8;
        }
        return result;
    }

    public static boolean isMetadataChecksum(String pathInfo) {
        String fileName = getFileName(pathInfo);
        return isChecksum(fileName) && fileName.startsWith(METADATA_PREFIX);
    }

    public static boolean isChecksum(String pathInfo) {
        String fileName = getFileName(pathInfo);
        return StringUtils.endsWithIgnoreCase(fileName, ".sha1") ||
                StringUtils.endsWithIgnoreCase(fileName, ".md5");
    }

    public static boolean isMetadata(String pathInfo) {
        String fileName = getFileName(pathInfo);
        return fileName.startsWith(METADATA_PREFIX) &&
                StringUtils.endsWithIgnoreCase(fileName, ".xml");
    }

    public static boolean isSnapshotMetadata(String pathInfo) {
        //*-SNAPSHOT/*maven-metadata.xml
        String parent = new File(pathInfo).getParent();
        return parent != null && parent.endsWith("-SNAPSHOT") && isMetadata(pathInfo);
    }

    public static boolean isJarVariant(String pathInfo) {
        String ext = MimeTypes.getExtension(pathInfo);
        return ext != null &&
                (StringUtils.endsWithIgnoreCase(ext, ".ar") || ext.equalsIgnoreCase(".jam") ||
                        ext.equalsIgnoreCase(".zip"));
    }

    public static boolean isPom(String pathInfo) {
        return StringUtils.endsWithIgnoreCase(pathInfo, ".pom");
    }

    public static boolean isXml(String pathInfo) {
        return isPom(pathInfo) || StringUtils.endsWithIgnoreCase(pathInfo, ".xml");
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
                        && !relPath.startsWith(pathPrefix)) {
                    throw new RepositoryException(
                            "The target deployment path '" + relPath +
                                    "' does not match the " +
                                    "POM's expected path prefix '" + pathPrefix +
                                    "' (make sure your import path is a m2 " +
                                    "repository root). Some files might have been " +
                                    "incorrectly imported - Please remove them " +
                                    "manually.");
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

    private static String getFileName(String pathInfo) {
        File dummy = new File(pathInfo);
        return dummy.getName();
    }
}
