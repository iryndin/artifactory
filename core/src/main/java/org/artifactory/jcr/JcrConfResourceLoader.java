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
import org.slf4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import static org.artifactory.util.PathUtils.hasText;

/**
 * This class acts as a resource loader for the jcr configuration files. It receives a request for a resource and
 * returns it from a specified path. It will first check if a path is specified in the ConstantsValue class, and if it
 * cannot find it there, (not specified, bad path, non-existant file, etc') it will try to find it in the resources
 * folder (META-INF/jcr).
 *
 * @author Noam Tenne
 */
public class JcrConfResourceLoader implements ResourceStreamHandle {

    private static final Logger log = LoggerFactory.getLogger(JcrConfResourceLoader.class);

    private final String resourceName;
    private InputStream is;
    private static final String FORWARD_SLASH = "/";
    private static final String FILE_PREFIX = "file:";
    public static final String ARTIFACTORY_ALTERNATE_REPO_XML = "artifactory.alternate-repo.xml";

    public JcrConfResourceLoader(String resourceName) {
        this.resourceName = resourceName;
    }

    public String getResourceName() {
        return resourceName;
    }

    public InputStream getInputStream() {
        String alternateRepoXml =
                ArtifactoryHome.get().getArtifactoryProperties().getProperty(ARTIFACTORY_ALTERNATE_REPO_XML, null);
        if (hasText(alternateRepoXml)) {
            try {
                is = new StringResourceStreamHandle(alternateRepoXml).getInputStream();
            } catch (IOException e) {
                log.error("Cannot convert string to stream", e);
            }
        }
        if (is == null) {
            String jcrConfigDir = ConstantValues.jcrConfigDir.getString();

            if (hasText(jcrConfigDir)) {
                jcrConfigDir = jcrConfigDir.trim();

                //If it is a relative url, relate it to $ARTIFACTORY_HOME/etc
                boolean startWithSlash = jcrConfigDir.startsWith(FORWARD_SLASH);
                boolean startWithFile = jcrConfigDir.startsWith(FILE_PREFIX);
                File file = new File(jcrConfigDir);
                if (file.exists()) {
                    jcrConfigDir = file.getAbsolutePath();
                } else if ((!startWithSlash) && (!startWithFile)) {
                    ArtifactoryHome artifactoryHome = ContextHelper.get().getArtifactoryHome();
                    jcrConfigDir = artifactoryHome.getEtcDir().getAbsoluteFile() + FORWARD_SLASH + jcrConfigDir;
                } else if (startWithFile) {
                    try {
                        jcrConfigDir = new URL(jcrConfigDir).getFile();
                    } catch (MalformedURLException e) {
                        throw new RuntimeException("Error while resolving configuration file: " + e.getMessage());
                    }
                }
                File requestedResource = new File(jcrConfigDir, resourceName);
                if (requestedResource.exists()) {
                    FileOutputStream fileOutputStream = null;
                    try {
                        is = new FileInputStream(requestedResource);
                    } catch (Exception e) {
                        is = null;
                        throw new IllegalArgumentException(e.getMessage());
                    } finally {
                        IOUtils.closeQuietly(fileOutputStream);
                    }
                } else {
                    log.warn("Jcr config file not found: {}. Using fallback config.",
                            requestedResource.getAbsolutePath());
                }
            }
        }
        if (is == null) {
            is = getFallbackInputStream();
        }
        return is;
    }

    public long getSize() {
        return -1;
    }

    protected InputStream getFallbackInputStream() {
        String resName = "/META-INF/jcr/" + resourceName;
        InputStream result = getClass().getResourceAsStream(resName);
        if (result == null) {
            throw new RuntimeException("Did not find resource '" + resName + "' in the classpath");
        }
        return result;
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