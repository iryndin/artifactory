package org.artifactory.converters.helpers;

import org.apache.commons.io.FileUtils;
import org.artifactory.api.context.ArtifactoryContext;
import org.artifactory.api.context.ArtifactoryContextThreadBinder;
import org.artifactory.common.ArtifactoryHome;
import org.artifactory.common.ha.HaNodeProperties;
import org.artifactory.converters.ConvertersManagerImpl;
import org.artifactory.mime.version.MimeTypesVersion;
import org.artifactory.util.ResourceUtils;
import org.artifactory.version.ArtifactoryVersion;
import org.testng.Assert;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

import static org.artifactory.common.ArtifactoryHome.*;

/**
 * Author: gidis
 * Helper static methods for the ConverterManagerTest
 */
public class ConvertersManagerTestHelper {
    public static final String home = "target/converterManagerTest/";

    /**
     * Creates Complete environment files and database simulator
     */
    public static ArtifactoryContext createEnvironment(ArtifactoryVersion homeVersion, ArtifactoryVersion dbVersion,
            ArtifactoryVersion clusterVersion)
            throws IOException {
        createHomeEnvironment(homeVersion, clusterVersion);
        ArtifactoryHome artifactoryHome = new MockArtifactoryHome();
        ConvertersManagerImpl convertersManager = new ConvertersManagerImpl(artifactoryHome);
        MockArtifactoryContext artifactoryContext = new MockArtifactoryContext(dbVersion, 1,
                convertersManager);
        ArtifactoryContextThreadBinder.bind(artifactoryContext);
        ArtifactoryHome.bind(artifactoryHome);
        return artifactoryContext;
    }

    public static void assertArtifactoryPropertiesHasBeenUpdate(ArtifactoryContext context) throws IOException {
        Properties properties = new Properties();
        try (FileInputStream inStream = new FileInputStream(home + ".artifactory/data/artifactory.properties")) {
            properties.load(inStream);
            // Assert that the Artifactory.properties file has been updated
            Assert.assertEquals(properties.get("artifactory.version"),
                    ArtifactoryHome.get().readRunningArtifactoryVersion().getVersion().getValue());
            // Assert that the Artifactory.properties has been loaded into artifactorySystemProperties
            Assert.assertEquals(properties.get("artifactory.version"),
                    ArtifactoryHome.get().getArtifactoryProperties().getProperty("artifactory.version", ""));
            Assert.assertEquals(properties.get("artifactory.version"),
                    context.getConverterManager().getRunningVersionDetails().getVersion().getValue());

            // Assert Mimetypes conversion
            String mimetypes = FileUtils.readFileToString(new File(home + ".artifactory-ha/ha-etc/mimetypes.xml"));
            String mimetypesVersion = "<mimetypes version=\"" + MimeTypesVersion.values().length + "\">";
            Assert.assertTrue(mimetypes.contains(mimetypesVersion));
        }
    }

    public static void assertValidMimeTypes() throws IOException {
        // Assert Mimetypes conversion
        String mimetypes = FileUtils.readFileToString(new File(home + ".artifactory-ha/ha-etc/mimetypes.xml"));
        String mimetypesVersion = "<mimetypes version=\"" + MimeTypesVersion.values().length + "\">";
        Assert.assertTrue(mimetypes.contains(mimetypesVersion));
    }

    public static void assertConverterHasNotBeenExecuted(ArtifactoryContext artifactoryContext) {
        boolean updateDbPropertiesHasBeenCalled = ((MockArtifactoryContext) artifactoryContext).getMockDbPropertiesService().isUpdateDbPropertiesHasBeenCalled();
        Assert.assertFalse(updateDbPropertiesHasBeenCalled);
        boolean conversionFinished = ((MockArtifactoryContext) artifactoryContext).getMockArtifactoryStateManager().isConversionFinished();
        Assert.assertFalse(conversionFinished);
    }


    public static void assertConversionHasBeenExecuted(ArtifactoryContext artifactoryContext) {
        boolean conversionFinished = ((MockArtifactoryContext) artifactoryContext).getMockArtifactoryStateManager().isConversionFinished();
        Assert.assertTrue(conversionFinished);
    }

    public static void assertConversionHasNotBeenExecuted(ArtifactoryContext artifactoryContext) {
        boolean conversionFinished = ((MockArtifactoryContext) artifactoryContext).getMockArtifactoryStateManager().isConversionFinished();
        Assert.assertFalse(conversionFinished);
    }

    public static void assertDBPropertiesHasBeenUpdated(ArtifactoryContext artifactoryContext) {
        boolean updateDbPropertiesHasBeenCalled = ((MockArtifactoryContext) artifactoryContext).getMockDbPropertiesService().isUpdateDbPropertiesHasBeenCalled();
        Assert.assertTrue(updateDbPropertiesHasBeenCalled);
    }

    public static void assertDBPropertiesHasNotBeenUpdated(ArtifactoryContext artifactoryContext) {
        boolean updateDbPropertiesHasBeenCalled = ((MockArtifactoryContext) artifactoryContext).getMockDbPropertiesService().isUpdateDbPropertiesHasBeenCalled();
        Assert.assertFalse(updateDbPropertiesHasBeenCalled);
    }

    public static void createHomeEnvironment(ArtifactoryVersion homeVersion, ArtifactoryVersion clusterVersion)
            throws IOException {
        FileUtils.deleteDirectory(new File(home + ".artifactory"));
        new File(home + ".artifactory").mkdir();
        new File(home + ".artifactory-ha").mkdir();
        if (homeVersion != null) {
            String basePath = "/converters/templates/home/" + homeVersion.getValue();
            String dataValue = ResourceUtils.getResourceAsString(basePath + "/" + ARTIFACTORY_PROPERTIES_FILE);
            String logbackValue = ResourceUtils.getResourceAsString(basePath + "/" + LOGBACK_CONFIG_FILE_NAME);
            String mimetypesValue = ResourceUtils.getResourceAsString(basePath + "/" + MIME_TYPES_FILE_NAME);
            Properties properties = createHaNodeProperties();

            new File(home + ".artifactory/data").mkdir();
            new File(home + ".artifactory/etc").mkdir();
            // HOME
            FileUtils.write(new File(home + ".artifactory/data/" + ARTIFACTORY_PROPERTIES_FILE), dataValue);
            FileUtils.write(new File(home + ".artifactory/etc/" + LOGBACK_CONFIG_FILE_NAME), logbackValue);
            FileUtils.write(new File(home + ".artifactory/etc/" + MIME_TYPES_FILE_NAME), mimetypesValue);
            try (FileOutputStream out = new FileOutputStream(
                    home + ".artifactory/etc/" + ARTIFACTORY_HA_NODE_PROPERTIES_FILE)) {
                properties.store(out, "");
            }
        }
        if (clusterVersion != null) {
            String basePath = "/converters/templates/home/" + clusterVersion.getValue();
            String dataValue = ResourceUtils.getResourceAsString(basePath + "/" + ARTIFACTORY_PROPERTIES_FILE);
            String logbackValue = ResourceUtils.getResourceAsString(basePath + "/" + LOGBACK_CONFIG_FILE_NAME);
            String mimetypesValue = ResourceUtils.getResourceAsString(basePath + "/" + MIME_TYPES_FILE_NAME);
            String clusterPropertiesValue = "security.token=76b07383dcda344979681e01efa5ac50";

            new File(home + ".artifactory-ha/ha-data").mkdir();
            new File(home + ".artifactory-ha/ha-etc").mkdir();
            // HA
            FileUtils.write(new File(home + ".artifactory-ha/ha-data/" + ARTIFACTORY_PROPERTIES_FILE), dataValue);
            FileUtils.write(new File(home + ".artifactory-ha/ha-etc/" + LOGBACK_CONFIG_FILE_NAME), logbackValue);
            FileUtils.write(new File(home + ".artifactory-ha/ha-etc/" + MIME_TYPES_FILE_NAME), mimetypesValue);
            FileUtils.write(new File(home + ".artifactory-ha/ha-etc/" + CLUSTER_PROPS_FILE), clusterPropertiesValue);
        }
    }

    private static Properties createHaNodeProperties() {
        Properties properties = new Properties();
        properties.put(HaNodeProperties.PROP_NODE_ID, "pom");
        properties.put(HaNodeProperties.PROP_CLUSTER_HOME,
                System.getProperties().get("user.dir") + "/" + home + ".artifactory-ha");
        properties.put(HaNodeProperties.PROP_CONTEXT_URL, "localhost");
        properties.put(HaNodeProperties.PROP_PRIMARY, "false");
        return properties;
    }
}
