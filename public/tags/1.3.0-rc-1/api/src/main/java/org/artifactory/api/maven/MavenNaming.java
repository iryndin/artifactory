package org.artifactory.api.maven;

import org.artifactory.api.mime.ContentType;
import org.artifactory.api.mime.NamingUtils;
import org.artifactory.utils.PathUtils;

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
    public static final Pattern ARTIFACT_NAME_PATTERN =
            Pattern.compile("(.+?)-(\\d.+?(-SNAPSHOT)?)(-([^-\\d]+))?\\.(\\w{3,}?)");

    public static final Pattern UNIQUE_SNAPSHOT_NAME_PATTERN =
            Pattern.compile("^(.*)-(([0-9]{8}.[0-9]{6})-([0-9]+))[\\.-].+$");

    public static final String METADATA_PREFIX = "maven-metadata";
    public static final String SNAPSHOT_VERSION = "SNAPSHOT";
    public static final String NEXUS_INDEX_DIR = ".index";
    public static final String NEXUS_INDEX_PREFIX = "nexus-maven-repository-index";
    public static final String NEXUS_INDEX_ZIP = NEXUS_INDEX_PREFIX + ".zip";
    public static final String NEXUS_INDEX_PROPERTIES = NEXUS_INDEX_PREFIX + ".properties";
    public static final String MAVEN_METADATA_NAME = "maven-metadata.xml";

    /**
     * Returns a matcher
     *
     * @param name The file name (with no preceding path) to match against
     * @return A RE matcher. If the matcher matches, then: group(1)=artifactId; group(2)=version; group(5)=classifier;
     *         group(6)=packaging.
     */
    @SuppressWarnings({"UnnecessaryLocalVariable"})
    public static Matcher artifactMatcher(String name) {
        Matcher matcher = ARTIFACT_NAME_PATTERN.matcher(name);
        return matcher;
    }

    public static boolean isVersionSnapshot(String version) {
        return version.endsWith(SNAPSHOT_VERSION);
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
        return NamingUtils.isChecksum(path);
    }

    public static boolean isChecksum(File path) {
        return isChecksum(path.getName());
    }

    public static boolean isHidden(String path) {
        return path.startsWith(".");
    }

    public static boolean isIndex(String path) {
        String name = PathUtils.getName(path);
        return name.startsWith(NEXUS_INDEX_PREFIX);
    }

    public static boolean isMetadata(String path) {
        return NamingUtils.isMetadata(path);
    }

    public static boolean isSnapshotMetadata(String path) {
        //*-SNAPSHOT/*maven-metadata.xml
        String parent = new File(path).getParent();
        return parent != null && parent.endsWith("-SNAPSHOT") && isMetadata(path);
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
        return fileName.equals(MAVEN_METADATA_NAME);
    }

    public static boolean isPom(String path) {
        ContentType ct = NamingUtils.getContentType(path);
        return ct.isPom();
    }

}
