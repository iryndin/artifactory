/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2010 JFrog Ltd.
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

package org.artifactory.jcr;

import org.apache.commons.io.IOUtils;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.common.ArtifactoryHome;
import org.artifactory.common.ConstantValues;
import org.artifactory.common.ResourceStreamHandle;
import org.artifactory.io.StringResourceStreamHandle;
import org.artifactory.log.LoggerFactory;
import org.artifactory.util.ResourceUtils;
import org.slf4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import static org.artifactory.common.ArtifactoryHome.ARTIFACTORY_JCR_CONFIG_DIR_DEFAULT;
import static org.artifactory.common.ArtifactoryHome.ARTIFACTORY_JCR_FILE;
import static org.artifactory.util.PathUtils.hasText;

/**
 * This class acts as a resource loader for the jcr configuration files. It receives a request for a resource and
 * returns it from a specified path. It will first check if a path is specified in the ConstantsValue class, and if it
 * cannot find it there, (not specified, bad path, non-existent file, etc') it will try to find it in the resources
 * folder (META-INF/jcr).
 *
 * @author Noam Tenne
 */
public class JcrConfResourceLoader implements ResourceStreamHandle {

    private static final Logger log = LoggerFactory.getLogger(JcrConfResourceLoader.class);

    private static final String PROP_TRANSIENT_REPO_XML =
            ConstantValues.SYS_PROP_PREFIX + "alternate-" + ARTIFACTORY_JCR_FILE;

    private final String defaultResName;
    private File configFile;
    private InputStream is;
    private static final String FORWARD_SLASH = "/";
    private static final String FILE_PREFIX = "file:";

    public JcrConfResourceLoader() {
        this(ARTIFACTORY_JCR_FILE);
    }

    public JcrConfResourceLoader(String defaultResourceName) {
        this.defaultResName = defaultResourceName;
    }

    public InputStream getInputStream() {
        String transientRepoXml = getTransientRepoXml();
        if (transientRepoXml != null) {
            try {
                is = new StringResourceStreamHandle(transientRepoXml).getInputStream();
            } catch (IOException e) {
                log.error("Cannot convert string to stream", e);
            }
        }
        if (is == null) {
            File configFile = getConfigFile();
            is = getConfigInputStream(configFile);
            if (is == null) {
                throw new IllegalStateException(
                        "Repository config could not be loaded - could not get the config input stream.");
            }
        }
        return is;
    }

    public static String getTransientRepoXml() {
        return ArtifactoryHome.get().getArtifactoryProperties().getProperty(PROP_TRANSIENT_REPO_XML, null);
    }

    public static void setTransientRepoXml(String transientRepoXml) {
        ArtifactoryHome.get().getArtifactoryProperties().setProperty(PROP_TRANSIENT_REPO_XML, transientRepoXml);
    }

    public static String removeTransientRepoXml() {
        return ArtifactoryHome.get().getArtifactoryProperties().removeProperty(PROP_TRANSIENT_REPO_XML);
    }

    /**
     * Return the user jcr config dir or, if not set, the default one
     *
     * @return
     */
    public File getConfigFile() {
        if (configFile != null) {
            return configFile;
        }
        String jcrConfigDir = getUserJcrConfigDir();
        if (jcrConfigDir == null) {
            jcrConfigDir = getRelativeConfigDir(ARTIFACTORY_JCR_CONFIG_DIR_DEFAULT);
        }
        configFile = new File(jcrConfigDir, ARTIFACTORY_JCR_FILE);
        if (!configFile.exists()) {
            if (getUserJcrConfigDir() != null) {
                //Do not use the default if the config dir was set by the user and no file - fail
                throw new RuntimeException(ConstantValues.jcrConfigDir.getPropertyName() +
                        " was specified (" + ConstantValues.jcrConfigDir.getString() +
                        "), but repository config file was not found at: " + configFile.getAbsolutePath() + ".");
            } else {
                copyDefaultJcrConfig(configFile);
            }
        }
        return configFile;
    }

    public static String getUserJcrConfigDir() {
        String jcrConfigDir = ConstantValues.jcrConfigDir.getString();

        if (!hasText(jcrConfigDir)) {
            return null;
        }
        jcrConfigDir = jcrConfigDir.trim();
        //If it is a relative url, relate it to $ARTIFACTORY_HOME/etc
        boolean startWithSlash = jcrConfigDir.startsWith(FORWARD_SLASH);
        boolean startWithFile = jcrConfigDir.startsWith(FILE_PREFIX);
        File file = new File(jcrConfigDir);
        if (file.exists()) {
            jcrConfigDir = file.getAbsolutePath();
        } else if ((!startWithSlash) && (!startWithFile)) {
            jcrConfigDir = getRelativeConfigDir(jcrConfigDir);
        } else if (startWithFile) {
            try {
                jcrConfigDir = new URL(jcrConfigDir).getFile();
            } catch (MalformedURLException e) {
                throw new RuntimeException("Error while resolving configuration file: " + e.getMessage());
            }
        }
        return jcrConfigDir;
    }

    private static String getRelativeConfigDir(String jcrConfigDir) {
        ArtifactoryHome artifactoryHome = ContextHelper.get().getArtifactoryHome();
        jcrConfigDir = artifactoryHome.getEtcDir().getAbsoluteFile() + FORWARD_SLASH + jcrConfigDir;
        return jcrConfigDir;
    }

    public long getSize() {
        return -1;
    }

    private void copyDefaultJcrConfig(File outputFile) {
        File parentDir = outputFile.getParentFile();
        boolean created = parentDir.mkdirs();
        if (!created && !parentDir.exists()) {
            throw new RuntimeException(
                    "Could not create the repository config output directory: " + parentDir.getAbsolutePath() +
                            ". Please verify that the user running Artifactory has sufficient privileges to create " +
                            "this directory.");
        }
        log.info("Copying default config to: {}.", outputFile.getAbsolutePath());
        String defaultResPath = "/META-INF/jcr/" + defaultResName;
        try {
            ResourceUtils.copyResource(defaultResPath, outputFile, getClass());
        } catch (IOException e) {
            throw new RuntimeException(
                    "Could copy the default repository config (" + defaultResName + ") from classpath.", e);
        }
    }

    private InputStream getConfigInputStream(File configFile) {
        FileOutputStream fileOutputStream = null;
        try {
            return new FileInputStream(configFile);
        } catch (Exception e) {
            throw new IllegalArgumentException(e.getMessage());
        } finally {
            IOUtils.closeQuietly(fileOutputStream);
        }
    }

    public void close() {
        try {
            if (is != null) {
                is.close();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            is = null;
        }
    }
}