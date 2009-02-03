package org.artifactory.repo.spring;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.artifactory.ArtifactoryHome;
import org.artifactory.repo.CentralConfig;
import org.artifactory.repo.jaxb.JaxbHelper;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public class CentralConfigFactoryBean implements FactoryBean, InitializingBean, DisposableBean {
    @SuppressWarnings({"UNUSED_SYMBOL", "UnusedDeclaration"})
    private final static Logger LOGGER = Logger.getLogger(CentralConfigFactoryBean.class);

    private CentralConfig cc;

    public CentralConfig getObject() throws Exception {
        return cc;
    }

    public Class getObjectType() {
        return CentralConfig.class;
    }

    public boolean isSingleton() {
        return true;
    }

    public void afterPropertiesSet() throws Exception {
        String home = ArtifactoryHome.path();
        String configLocation = home + "/etc/artifactory.config.xml";
        LOGGER.info("Loading configuration (using '" + configLocation + "')...");
        InputStream in = null;
        //Support loading the configuration from a url stream or server resource
        LOGGER.info("Trying to load configuration from url...");
        try {
            URL url = new URL(configLocation);
            URLConnection con = url.openConnection();
            in = con.getInputStream();
        } catch (Exception e) {
            LOGGER.info("Could not load configuration from url '" + configLocation +
                    "'. (" + e.getMessage() + ").");
        }
        //Try to get it from the path
        if (in == null) {
            LOGGER.info("Trying to load configuration from regular path file reosurce....");
            try {
                in = new FileInputStream(configLocation);
            } catch (FileNotFoundException e) {
                LOGGER.error("Could not load configuration from path location '"
                        + configLocation + "'. Giving up!");
                throw new RuntimeException("Artifactory configuration load failure!");
            }
        }
        try {
            cc = new JaxbHelper<CentralConfig>().read(in, CentralConfig.class);
            LOGGER.info("Loaded configuration from '" + configLocation + "'.");
            cc.afterPropertiesSet();
            //TODO: [by yl] REMOVE ME
            /*
            MavenWrapper wrapper = cc.getMavenWrapper();
            Artifact artifact = wrapper.createArtifact("groovy", "groovy", "1.0-jsr-06",
                    Artifact.SCOPE_COMPILE, "jar");
            wrapper.resolve(artifact, cc.getLocalRepositories().get(0), cc.getRemoteRepositories());
            */
        } catch (Exception e) {
            LOGGER.error("Failed to load configuration from '" + configLocation + "'.", e);
            throw new RuntimeException(e);
        } finally {
            IOUtils.closeQuietly(in);
        }
    }

    public void destroy() throws Exception {
        if (cc != null) {
            cc.destroy();
        }
    }
}
