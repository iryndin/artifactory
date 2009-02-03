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
import org.apache.maven.embedder.MavenEmbedder;
import org.apache.maven.embedder.MavenEmbedderConsoleLogger;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.artifactory.utils.MimeTypes;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.springframework.util.StringUtils;

import javax.jcr.RepositoryException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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
    private static final Pattern ARTIFACT_FILENAME_PATTERN =
            Pattern.compile("(.+?)-(\\d.+?(-SNAPSHOT)?)(-([^-\\d]+))?\\.(\\w{3,}?)");
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

    public static boolean isSnapshot(String pathInfo) {
        boolean found = pathInfo.indexOf("-SNAPSHOT.") > 0;
        if (!found) {
            int versionIdx = pathInfo.indexOf("SNAPSHOT/");
            found = versionIdx > 0 && pathInfo.lastIndexOf('/') == versionIdx + 8;
        }
        return found;
    }

    public static boolean isChecksum(String pathInfo) {
        String fileName = getFileName(pathInfo);
        return StringUtils.endsWithIgnoreCase(fileName, ".sha1") ||
                StringUtils.endsWithIgnoreCase(fileName, ".md5");
    }

    public static boolean isMetaData(String pathInfo) {
        String fileName = getFileName(pathInfo);
        return fileName.startsWith(METADATA_PREFIX) &&
                StringUtils.endsWithIgnoreCase(fileName, ".xml");
    }

    public static boolean isJarVariant(String pathInfo) {
        String ext = MimeTypes.getExtension(pathInfo);
        return StringUtils.endsWithIgnoreCase(ext, "ar") || ext.equalsIgnoreCase("jam");
    }

    public static boolean isPom(String pathInfo) {
        int idx = pathInfo.indexOf(".pom");
        return idx > 0
                && (pathInfo.length() - 4 == idx
                || pathInfo.charAt(idx + 4) == '.');
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
                    modelVersion = model.getParent().getVersion();
                }
                String pathPrefix = groupId.replace('.', '/') + "/" +
                        model.getArtifactId() + "/" + modelVersion;
                if (!relPath.startsWith(pathPrefix)) {
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

    private static String getFileName(String pathInfo) {
        File dummy = new File(pathInfo);
        return dummy.getName();
    }
}
