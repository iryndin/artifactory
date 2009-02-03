package org.artifactory.test.internal;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.artifactory.api.common.StatusHolder;
import org.artifactory.api.config.ImportSettings;
import org.artifactory.api.context.ArtifactoryContextThreadBinder;
import org.artifactory.api.security.UserInfo;
import org.artifactory.common.ArtifactoryHome;
import org.artifactory.config.SpringConfResourceLoader;
import org.artifactory.repo.service.InternalRepositoryService;
import org.artifactory.security.SecurityServiceInternal;
import org.artifactory.spring.ArtifactoryApplicationContext;
import org.artifactory.test.mock.MockServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.Authentication;
import org.springframework.security.AuthenticationManager;
import org.springframework.security.context.SecurityContextHolder;
import org.springframework.security.providers.AbstractAuthenticationToken;
import org.springframework.security.providers.UsernamePasswordAuthenticationToken;
import org.testng.ITestContext;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

public abstract class ArtifactoryTestBase {
    private final static Logger log = LoggerFactory.getLogger(ArtifactoryTestBase.class);

    protected ArtifactoryApplicationContext context;
    /**
     * Mock server as remote repo
     */
    protected MockServer mockServer = null;
    public static final String DEFAULT_CONFIG_NAME = "default-test";
    public static String ARTIFACTORY_TEST_HOME = "";

    @BeforeTest
    protected void setUp(ITestContext testNgContext) throws Exception {
        final String testName = testNgContext.getName();
        log.info("Test setup started for test " + testName);
        //Create the artifactory home in ../target/artifactory/{configName}
        String configName = getConfigName();
        File homeDir = new File("test-output/artifactory", testName);
        ARTIFACTORY_TEST_HOME = homeDir.getAbsolutePath();
        log.info("Using artifactory home at: " + ARTIFACTORY_TEST_HOME);
        //Delete the home dir
        FileUtils.deleteDirectory(homeDir);
        ArtifactoryHome.setHomeDir(homeDir);
        ArtifactoryHome.create();
        mockServer = MockServer.start("swamp.jfrog.org", "localhost");
        //Set up the logback props
        //ArtifactoryHome.ensureLogbackConfig("file:${artifactory.home}/etc/logback.xml");
        copyArtifactoryConfig(configName);
        context = new ArtifactoryApplicationContext(SpringConfResourceLoader.getConfigurationPaths());
        ArtifactoryContextThreadBinder.bind(context);
        log.info("Test setup completed for tsts " + testName);
    }

    @AfterTest
    protected void tearDown() throws Exception {
        log.info("Test tearDown started");
        ArtifactoryContextThreadBinder.bind(context);
        context.destroy();
        log.info("Test tearDown completed");
    }

    @BeforeMethod
    protected void bindThreadContext() {
        loginAsAdmin();
        ArtifactoryContextThreadBinder.bind(context);
    }

    @AfterMethod
    protected void unbindThreadContext() {
        ArtifactoryContextThreadBinder.unbind();
        SecurityContextHolder.getContext().setAuthentication(null);
    }

    void authenticate(AbstractAuthenticationToken token) {
        //Create the Anonymous token
        AuthenticationManager authenticationManager = context.beanForType(AuthenticationManager.class);
        Authentication authentication = authenticationManager.authenticate(token);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    void loginAsAdmin() {
        authenticate(new UsernamePasswordAuthenticationToken(
                SecurityServiceInternal.USER_DEFAULT_ADMIN,
                SecurityServiceInternal.DEFAULT_ADMIN_PASSWORD));
    }

    void loginAsAnonymous() {
        authenticate(new UsernamePasswordAuthenticationToken(UserInfo.ANONYMOUS, ""));
    }

    /**
     * Meant to be overriden.
     *
     * @return Artifactory configuration file name for this test.
     */
    String getConfigName() {
        return DEFAULT_CONFIG_NAME;
    }

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
            String configText = IOUtils.toString(is);
            String modifiedConfig = configText.replaceAll("@mock_host@", mockServer.getSelectedURL());
            InputStream modifiedStream = IOUtils.toInputStream(modifiedConfig);
            bis = new BufferedInputStream(modifiedStream);
            File targetConfigFile = new File(ArtifactoryHome.getEtcDir(), ArtifactoryHome.ARTIFACTORY_CONFIG_FILE);
            bos = new BufferedOutputStream(new FileOutputStream(targetConfigFile));
            IOUtils.copy(bis, bos);
        } finally {
            IOUtils.closeQuietly(bos);
            IOUtils.closeQuietly(bis);
        }
    }

    protected void importToRepoFromExportPath(String exportPath, String targetRepo, boolean asynchronousImport)
            throws IOException, InterruptedException, URISyntaxException {
        InternalRepositoryService repoService = context.beanForType(InternalRepositoryService.class);
        StatusHolder statusHolder = new StatusHolder();
        URL exportUrl = getClass().getResource(exportPath);
        URI exportUri = exportUrl.toURI();
        if (exportUri.toString().contains(".jar")) {
            // TODO: Need to extract info from jar
            throw new IOException("Cannot run WcCommiter test if export in jar!");
        }
        loginAsAdmin();
        ImportSettings importSettings = new ImportSettings(new File(exportUri));
        importSettings.setCopyToWorkingFolder(asynchronousImport);
        repoService.importRepo(
                targetRepo,
                importSettings,
                statusHolder);
        if (statusHolder.isError()) {
            Throwable throwable = statusHolder.getException();
            if (throwable != null) {
                throw new RuntimeException(
                        "Import of test failed with msg: " + statusHolder.getStatusMsg(),
                        throwable);
            } else {
                throw new RuntimeException(
                        "Import of test failed with msg: " + statusHolder.getStatusMsg());
            }
        }
    }

    public String getHomePath() {
        return ARTIFACTORY_TEST_HOME;
    }
}