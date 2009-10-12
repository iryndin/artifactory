package org.artifactory.version;

import org.apache.commons.io.IOUtils;
import org.artifactory.common.ConstantsValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Returns ArtifactoryVersion object from a properties stream/file.
 *
 * @author Yossi Shaul
 */
public class ArtifactoryVersionReader {
    private final static Logger log = LoggerFactory.getLogger(ArtifactoryVersionReader.class);

    public static CompoundVersionDetails read(InputStream inputStream) {
        if (inputStream == null) {
            throw new IllegalArgumentException("Artifactory properties input stream cannot be null");
        }
        Properties props = new Properties();
        try {
            props.load(inputStream);
        } catch (IOException e) {
            throw new RuntimeException("Cannot read input property stream", e);
        } finally {
            IOUtils.closeQuietly(inputStream);
        }
        String versionString = props.getProperty(ConstantsValue.artifactoryVersion.getPropertyName());
        String revisionString = props.getProperty(ConstantsValue.artifactoryRevision.getPropertyName());

        ArtifactoryVersion matchedVersion = null;

        // If current version or development version ${project.version}
        if (ArtifactoryVersion.getCurrent().getValue().equals(versionString) ||
                "${project.version}".equals(versionString) ||
                "${buildNumber}".equals(revisionString)) {
            // Just return the current version
            matchedVersion = ArtifactoryVersion.getCurrent();
        }

        if (matchedVersion == null) {
            matchedVersion = findByVersionString(versionString, revisionString);
        }
        if (matchedVersion == null) {
            matchedVersion = findClosestMatch(versionString, revisionString);
        }

        if (matchedVersion == null) {
            throw new IllegalStateException("No version declared is higher than " + revisionString);
        }

        return new CompoundVersionDetails(matchedVersion, versionString, revisionString);
    }

    private static ArtifactoryVersion findByVersionString(String versionString, String revisionString) {
        int artifactoryRevision = Integer.parseInt(revisionString);
        for (ArtifactoryVersion version : ArtifactoryVersion.values()) {
            if (version.getValue().equals(versionString)) {
                if (artifactoryRevision != version.getRevision()) {
                    log.warn("Version found is " + version + " but the revision " +
                            artifactoryRevision + " is not the one supported!\n" +
                            "Reading the folder may work with this version.\n" +
                            "For Information: Using the Command Line Tool is preferable in this case.");
                }
                return version;
            }
        }
        return null;
    }

    private static ArtifactoryVersion findClosestMatch(String versionString, String revisionString) {
        int artifactoryRevision = Integer.parseInt(revisionString);
        log.warn("Version " + versionString + " is not an official realease version. " +
                "The closest revision to " + artifactoryRevision + " will be used to determine the current version.\n" +
                "Warning: This version is unsupported! Reading backup data may not work!\n" +
                "Specifying an explicit version in artadmin commands is recommended.");
        for (ArtifactoryVersion version : ArtifactoryVersion.values()) {
            if (version.getRevision() >= artifactoryRevision) {
                return version;
            }
        }
        return null;
    }

    public static CompoundVersionDetails read(File propertiesFile) {
        if (propertiesFile == null) {
            throw new IllegalArgumentException("Null properties file is not allowed");
        }
        try {
            return read(new FileInputStream(propertiesFile));
        } catch (FileNotFoundException e) {
            throw new RuntimeException("Properties file " + propertiesFile.getName() + " doesn't exist");
        }
    }
}
