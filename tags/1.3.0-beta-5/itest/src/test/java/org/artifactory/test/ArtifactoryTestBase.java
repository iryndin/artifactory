package org.artifactory.test;

import net.sf.ehcache.CacheManager;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.artifactory.api.context.ArtifactoryContextThreadBinder;
import org.artifactory.api.security.UserInfo;
import org.artifactory.common.ArtifactoryHome;
import org.artifactory.security.SecurityServiceInternal;
import org.artifactory.spring.ArtifactoryApplicationContext;
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

public abstract class ArtifactoryTestBase {
    private final static Logger log = Logger.getLogger(ArtifactoryTestBase.class);

    protected ArtifactoryApplicationContext context;

    public static final String DEFAULT_CONFIG_NAME = "default-test";

    @BeforeTest
    protected void setUp(ITestContext testNgContext) throws Exception {
        log.info("Test setup started for test " + testNgContext.getName());
        //Create the artifactory home in ../target/artifactory/{configName}
        String configName = getConfigName();
        File homeDir = new File("test-output/artifactory", configName);
        log.info("Using artifactory home at: " + homeDir.getAbsolutePath());
        //Delete the home dir
        FileUtils.deleteDirectory(homeDir);

        // TODO: try to avoid this step
        // clear caches of previously executed tests
        CacheManager.getInstance().removalAll();

        ArtifactoryHome.setHomeDir(homeDir);
        ArtifactoryHome.create();
        //Set up the log4j props
        ArtifactoryHome.checkLog4j("file:${artifactory.home}/etc/log4j.properties");
        copyArtifactoryConfig(configName);
        context = new ArtifactoryApplicationContext("/META-INF/spring/applicationContext.xml");
        ArtifactoryContextThreadBinder.bind(context);
        log.info("Test setup completed for tsts " + testNgContext.getName());
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
        AuthenticationManager authenticationManager =
                context.beanForType(AuthenticationManager.class);
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
                throw new IllegalArgumentException(
                        "Could not find a configuration resource at: " + configPath);
            }
            bis = new BufferedInputStream(is);
            File targetConfigFile =
                    new File(ArtifactoryHome.getEtcDir(), ArtifactoryHome.ARTIFACTORY_CONFIG_FILE);
            bos = new BufferedOutputStream(new FileOutputStream(targetConfigFile));
            IOUtils.copy(bis, bos);
        } finally {
            IOUtils.closeQuietly(bos);
            IOUtils.closeQuietly(bis);
        }
    }
}