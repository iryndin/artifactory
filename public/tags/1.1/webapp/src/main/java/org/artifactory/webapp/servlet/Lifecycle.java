/*
 * Copyright (c) 2004-2005, by OpenXource, LLC. All rights reserved.
 * 
 * THIS IS UNPUBLISHED PROPRIETARY SOURCE CODE OF OPENXOURCE
 *  
 * The copyright notice above does not evidence any          
 * actual or intended publication of such source code. 
 */
package org.artifactory.webapp.servlet;

import org.artifactory.repo.CentralConfig;
import org.artifactory.repo.jaxb.JaxbHelper;
import org.artifactory.utils.IoUtil;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

public class Lifecycle implements ServletContextListener {

    public static final String PARAM_CONFIG_LOCATION = "artifactory.config.location";

    private static final org.apache.log4j.Logger LOGGER =
            org.apache.log4j.Logger.getLogger(Lifecycle.class);

    public void contextInitialized(ServletContextEvent event) {
        LOGGER.info("Starting Artifactory...");
        LOGGER.info("Loading configuration...");
        LOGGER.info("Looking for \"-D" + PARAM_CONFIG_LOCATION + "=<location>\" vm parameter.");
        String location = System.getProperty(PARAM_CONFIG_LOCATION);
        if (location == null) {
            LOGGER.info("Could not find vm parameter. Trying servlet context init parameter...");
            location = event.getServletContext().getInitParameter(PARAM_CONFIG_LOCATION);
            if (location == null) {
                throw new RuntimeException("Artifactory configuration location not set!");
            }
        }
        LOGGER.info("Loading configuration (using '" + location + "')...");
        InputStream in = null;
        CentralConfig cc;
        //Support loading the configuration from a url stream or server resource
        LOGGER.info("Trying to load configuration from url...");
        try {
            URL url = new URL(location);
            URLConnection con = url.openConnection();
            in = con.getInputStream();
        } catch (Exception e) {
            LOGGER.info("Could not load configuration from url '" + location +
                    "'. (" + e.getMessage() + ").");
        }
        //Try to get it from the context
        LOGGER.info("Trying to load configuration from regular context reosurce...");
        ServletContext servletContext = event.getServletContext();
        if (in == null) {
            in = servletContext.getResourceAsStream(location);
        }
        //Try to get it from the path
        if (in == null) {
            LOGGER.info("Could not load configuration from context location '" + location + "'.");
            LOGGER.info("Trying to load configuration from regular path file reosurce....");
            try {
                in = new FileInputStream(location);
            } catch (FileNotFoundException e) {
                LOGGER.info("Could not load configuration from path location '"
                        + location + "'. Giving up!");
                throw new RuntimeException("Artifactory configuration load failure!");
            }
        }
        try {
            cc = new JaxbHelper<CentralConfig>().read(in, CentralConfig.class);
            cc.start();
            //TODO: [by yl] REMOVE ME
            /*
            MavenWrapper wrapper = cc.getMavenWrapper();
            Artifact artifact = wrapper.createArtifact("groovy", "groovy", "1.0-jsr-06",
                    Artifact.SCOPE_COMPILE, "jar");
            wrapper.resolve(artifact, cc.getLocalRepositories().get(0), cc.getRemoteRepositories());
            */
        } catch (Exception e) {
            LOGGER.error("Failed to load configuration from '" + location + "'.", e);
            throw new RuntimeException(e);
        } finally {
            IoUtil.close(in);
        }
        event.getServletContext().setAttribute(CentralConfig.ATTR_CONFIG, cc);
        LOGGER.info("Loaded configuration from '" + location + "'.");
    }

    public void contextDestroyed(ServletContextEvent event) {
        CentralConfig cc =
                (CentralConfig) event.getServletContext().getAttribute(CentralConfig.ATTR_CONFIG);
        if (cc != null) {
            cc.stop();
        }
        event.getServletContext().removeAttribute(CentralConfig.ATTR_CONFIG);
    }
}
