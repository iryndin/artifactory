/*
 * This file is part of Artifactory.
 *
 * Artifactory is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Artifactory is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Artifactory.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.artifactory.api.maven;

import org.apache.commons.io.FilenameUtils;
import org.artifactory.api.mime.ContentType;
import org.artifactory.api.mime.NamingUtils;
import org.artifactory.api.util.Pair;
import org.artifactory.util.PathUtils;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public class MavenNaming {
    //Uses lazy evaluation of the version (+?)
    //see: http://www.regular-expressions.info/reference.html
    //For testing, see: http://www.cis.upenn.edu/~matuszek/General/RegexTester/regex-tester.html
    //Should be used with standard artifact names:
    //artifactId-version.ext
    //artifactId-version-classifier.ext
    //groups: 1-artifactId; 2-version; 5-classifier; 6-packaging.
    private static final Pattern ARTIFACT_NAME_PATTERN =
            Pattern.compile("(.+?)-(\\d.+?(-SNAPSHOT)?)(-(.+?))?\\.(\\w{3,}?)");

    // Matcher for unique snapshot names of the form artifactId-version-date.time-buildNumber.type
    // for example: artifactory-1.0-20081214.090217-4.pom
    // groups: 1: artifactId-version, 2: date.time-buildNumber 3: date.time 4: buildNumber
    private static final Pattern UNIQUE_SNAPSHOT_NAME_PATTERN =
            Pattern.compile("^(.+-.+)-(([0-9]{8}.[0-9]{6})-([0-9]+))[\\.-].+$");

    public static final String METADATA_PREFIX = "maven-metadata";
    public static final String MAVEN_METADATA_NAME = "maven-metadata.xml";
    public static final String SNAPSHOT_VERSION = "SNAPSHOT";
    public static final String NEXUS_INDEX_DIR = ".index";
    public static final String NEXUS_INDEX_PREFIX = "nexus-maven-repository-index";
    public static final String NEXUS_INDEX_ZIP = NEXUS_INDEX_PREFIX + ".zip";
    public static final String NEXUS_INDEX_PROPERTIES = NEXUS_INDEX_PREFIX + ".properties";
    public static final String NEXUS_INDEX_ZIP_PATH = NEXUS_INDEX_DIR + "/" + NEXUS_INDEX_ZIP;
    public static final String NEXUS_INDEX_PROPERTIES_PATH = NEXUS_INDEX_DIR + "/" + NEXUS_INDEX_PROPERTIES;

    /**
     * Returns a MavenArtifactInfo based on info that was managed to gather from the file name matcher
     *
     * @param fileName The file name (with no preceding path) to match against
     * @return MavenArtifactInfo object with gathered info
     */
    public static MavenArtifactInfo getInfoByMatching(String fileName) {
        MavenArtifactInfo mavenArtifactInfo = new MavenArtifactInfo();
        Matcher matcher = ARTIFACT_NAME_PATTERN.matcher(fileName);
        if (matcher.matches()) {
            mavenArtifactInfo.setArtifactId(matcher.group(1));
            mavenArtifactInfo.setVersion(matcher.group(2));
            mavenArtifactInfo.setClassifier(matcher.group(5));
        }

        return mavenArtifactInfo;
    }

    /**
     * @param version String representing the maven version
     * @return True if the version is a non-unique snapshot version (ie, ends with SNAPSHOT)
     */
    public static boolean isVersionSnapshot(String version) {
        return version.endsWith(SNAPSHOT_VERSION);
    }

    /**
     * @param path Path to a file
     * @return True if the path is of a non-unique snapshot version file
     */
    public static boolean isNonUniqueSnapshot(String path) {
        int idx = path.indexOf("-SNAPSHOT.");
        if (idx < 0) {
            idx = path.indexOf("-SNAPSHOT-");
        }
        return idx > 0 && idx > path.lastIndexOf('/');
    }

    public static boolean isUniqueSnapshot(String path) {
        int versionIdx = path.indexOf("SNAPSHOT/");
        if (versionIdx > 0) {
            String fileName = PathUtils.getName(path);
            return isUniqueSnapshotFileName(fileName);
        } else {
            return false;
        }
    }

    /**
     * @param path A path to file or directory
     * @return True if the path is for a snapshot file or folder
     */
    public static boolean isSnapshot(String path) {
        boolean result = isNonUniqueSnapshot(path);
        if (!result) {
            result = isUniqueSnapshot(path);
        }
        //A path ending with just the version dir
        if (!result) {
            int versionIdx = path.indexOf("SNAPSHOT/");
            result = versionIdx > 0 && path.lastIndexOf('/') == versionIdx + 8;
        }
        if (!result) {
            result = path.endsWith("SNAPSHOT");
        }
        return result;
    }

    public static boolean isRelease(String path) {
        return !isSnapshot(path) && !NamingUtils.isMetadata(path) && !isChecksum(path) && !isIndex(path);
    }

    public static boolean isMetadataChecksum(String path) {
        String name = PathUtils.getName(path);
        return isChecksum(name) && name.startsWith(METADATA_PREFIX);
    }

    public static boolean isChecksum(String path) {
        return NamingUtils.isChecksum(path);
    }

    public static boolean isChecksum(File path) {
        return isChecksum(path.getName());
    }

    public static boolean isHidden(String path) {
        return path.startsWith(".");
    }

    /**
     * @param path Path to test
     * @return True if this path points to nexus index file.
     */
    public static boolean isIndex(String path) {
        String name = PathUtils.getName(path);
        return name.startsWith(NEXUS_INDEX_PREFIX);
    }

    public static boolean isMavenMetadata(String path) {
        String fileName = PathUtils.getName(path);
        return isMavenMetadataFileName(fileName);
    }

    public static boolean isSnapshotMavenMetadata(String path) {
        final Pair<String, String> nameAndParent = NamingUtils.getMetadtaNameAndParent(path);
        String name = nameAndParent.getFirst();
        String parent = nameAndParent.getSecond();
        return parent != null && parent.endsWith("-SNAPSHOT") && isMavenMetadataFileName(name);
    }

    /**
     * Converts a path to maven 1 path. For example org/apache/commons/commons-email/1.1/commons-email-1.1.jar will
     * result in org.apache.commons/jars/commons-email-1.1.jar
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

    public static boolean isMavenMetadataFileName(String fileName) {
        return MAVEN_METADATA_NAME.equals(fileName) ||
                fileName.endsWith(NamingUtils.METADATA_PREFIX + MAVEN_METADATA_NAME);
    }

    public static boolean isPom(String path) {
        ContentType ct = NamingUtils.getContentType(path);
        return ct.isPom();
    }

    public static boolean isClientOrServerPom(String path) {
        return isPom(path) || isClientPom(path);
    }

    public static boolean isClientPom(String path) {
        String name = FilenameUtils.getName(path);
        return "pom.xml".equalsIgnoreCase(name);
    }

    /**
     * @param fileName The file name to test if is a unique snapshot
     * @return True if the file name is of the form artifactId-version-date.time-buildNumber.type
     *         <p/>
     *         For example: artifactory-1.0-20081214.090217-4.pom
     */
    public static boolean isUniqueSnapshotFileName(String fileName) {
        Matcher matcher = UNIQUE_SNAPSHOT_NAME_PATTERN.matcher(fileName);
        return matcher.matches();
    }

    /**
     * @param uniqueVersion A file name representing a valid unique snapshot version.
     * @return The timestamp of the unique snapshot version
     */
    public static String getUniqueSnapshotVersionTimestamp(String uniqueVersion) {
        Matcher matcher = UNIQUE_SNAPSHOT_NAME_PATTERN.matcher(uniqueVersion);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Not a valid maven unique snapshot version: " + uniqueVersion);
        }
        return matcher.group(3);
    }

    /**
     * @param uniqueVersion A file name representing a valid unique snapshot version.
     * @return The timestamp-buildNumber of the unique snapshot version
     */
    public static String getUniqueSnapshotVersionTimestampAndBuildNumber(String uniqueVersion) {
        Matcher matcher = UNIQUE_SNAPSHOT_NAME_PATTERN.matcher(uniqueVersion);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Not a valid maven unique snapshot version: " + uniqueVersion);
        }
        return matcher.group(2);
    }

    /**
     * @param uniqueVersion A file name representing a valid unique snapshot version.
     * @return The buildNumber of the unique snapshot version
     */
    public static int getUniqueSnapshotVersionBuildNumber(String uniqueVersion) {
        Matcher matcher = UNIQUE_SNAPSHOT_NAME_PATTERN.matcher(uniqueVersion);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Not a valid maven unique snapshot version: " + uniqueVersion);
        }
        return Integer.parseInt(matcher.group(4));
    }
}
