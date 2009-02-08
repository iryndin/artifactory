package org.artifactory.test.http;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.SystemUtils;
import org.artifactory.common.ArtifactoryHome;
import org.artifactory.test.mock.MockServer;
import org.mortbay.jetty.Server;
import org.mortbay.xml.XmlConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * An embedded Artifatory server used for testing.
 *
 * @author Noam
 */
public class ArtifactoryServer {
    private static final Logger log = LoggerFactory.getLogger(ArtifactoryServer.class);

    protected static final String DEFAULT_CONFIG = "localhost-repo";

    private String testName;
    private String configName;
    private MockServer mockServer;
    protected Server jetty;

    public ArtifactoryServer(String testName) {
        this(testName, DEFAULT_CONFIG);
    }

    public ArtifactoryServer(String testName, MockServer mockServerInstance) {
        this(testName, DEFAULT_CONFIG, mockServerInstance);
    }

    public ArtifactoryServer(String testName, String configName) {
        Assert.assertNotNull(testName);
        this.configName = configName;
        this.testName = testName;
    }

    public ArtifactoryServer(String testName, String configName, MockServer mockServerInstance) {
        Assert.assertNotNull(testName);
        this.configName = configName;
        this.testName = testName;
        this.mockServer = mockServerInstance;
    }

    public void start() throws IOException {
        log.info("Test setup started for test");
        setJettyWebappDir();
        //Create the artifactory home in ../target/artifactory/{testName}
        File homeDir = new File("target/test-artifactory", testName);
        log.info("Using artifactory home at: " + homeDir.getAbsolutePath());
        //Delete the home dir
        FileUtils.deleteDirectory(homeDir);

        ArtifactoryHome.setHomeDir(homeDir);
        ArtifactoryHome.create();
        //Set up the logback props
        ArtifactoryHome.getLogbackConfig();

        copyArtifactoryConfig(configName);
        try {
            URL configUrl = ArtifactoryServer.class.getResource("/http/jetty.xml");
            XmlConfiguration xmlConfiguration = new XmlConfiguration(configUrl);
            jetty = new Server();
            xmlConfiguration.configure(jetty);
            jetty.start();
            log.info("Embedded Artifactory Server started");
        } catch (Exception e) {
            log.error("Could not start the Jetty server: " + e);
            stop();
        }
    }

    public void stop() {
        if (jetty != null) {
            try {
                jetty.stop();
            } catch (Exception e1) {
                log.error("Unable to stop the jetty server: " + e1);
            }
        }
    }

    /**
     * Finds where the artifactory webapp dir is and sets a system property for jetty to use. The relative path to the
     * webapp dir might be deferent if the test is executed from maven or from the ide.
     */
    private void setJettyWebappDir() throws IOException {
        File webappDir = getWebappDir();
        String jettyWarDir = webappDir.getCanonicalPath() + "/src/main/webapp";
        System.setProperty("webapp.dir", jettyWarDir);
    }

    private File getWebappDir() throws IOException {
        File userDir = SystemUtils.getUserDir();
        File webappDir = new File(userDir, "webapp");
        if (webappDir.exists()) {
            // running from the root source directory (usually from ide)
            return webappDir;
        } else {
            webappDir = new File(userDir.getParent(), "webapp");
            if (webappDir.exists()) {
                // running from itest (maven)
                return webappDir;
            }
        }
        throw new RuntimeException("webapp directory not found");
    }

    // TODO: reuse from ArtifactoryTestBase
    private void copyArtifactoryConfig(String configName) throws IOException {
        //Copy the artifactory.config.xml to the home
        String configPath = "/org/artifactory/config/" + configName + ".xml";
        BufferedInputStream bis = null;
        BufferedOutputStream bos = null;
        try {
            InputStream is = getClass().getResourceAsStream(configPath);
            if (is == null) {
                throw new IllegalArgumentException("Could not find a configuration resource at: " + configPath);
            }
            if (mockServer != null) {
                String configText = IOUtils.toString(is);
                String modifiedConfig = configText.replaceAll("@mock_host@", mockServer.getSelectedURL());
                InputStream modifiedStream = IOUtils.toInputStream(modifiedConfig);
                bis = new BufferedInputStream(modifiedStream);
            } else {
                bis = new BufferedInputStream(is);
            }
            File targetConfigFile = new File(ArtifactoryHome.getEtcDir(), ArtifactoryHome.ARTIFACTORY_CONFIG_FILE);
            bos = new BufferedOutputStream(new FileOutputStream(targetConfigFile));
            IOUtils.copy(bis, bos);
        } finally {
            IOUtils.closeQuietly(bos);
            IOUtils.closeQuietly(bis);
        }
    }

    public static void main(String[] args) {
        ArtifactoryServer server = new ArtifactoryServer("MainServer");
        try {
            server.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
