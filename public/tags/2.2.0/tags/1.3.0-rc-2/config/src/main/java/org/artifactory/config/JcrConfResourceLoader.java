package org.artifactory.config;

import org.artifactory.common.ArtifactoryHome;
import org.artifactory.common.ConstantsValue;
import org.artifactory.common.ResourceStreamHandle;
import org.artifactory.util.PathUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

/**
 * This class acts as a resource loader for the jcr configuration files. It recieves a request for a resource and
 * returns it from a specified path. It will first check if a path is specified in the ConstantsValue class, and if it
 * cannot find it there, (not specified, bad path, non-existant file, etc') it will try to find it in the resources
 * folder (META-INF/jcr).
 *
 * @author Noam Tenne
 */
public class JcrConfResourceLoader implements ResourceStreamHandle {
    private final String resourceName;
    private InputStream is;
    private final String FORWARD_SLASH = "/";
    private final String FILE_PREFIX = "file:";

    public JcrConfResourceLoader(String resourceName) {
        this.resourceName = resourceName;
    }

    public String getResourceName() {
        return resourceName;
    }

    public InputStream getInputStream() {
        if (is == null) {
            String jcrConfigPath = ConstantsValue.jcrConfigPath.getString();

            if (PathUtils.hasText(jcrConfigPath)) {
                jcrConfigPath = jcrConfigPath.trim();

                if ((!jcrConfigPath.startsWith(FORWARD_SLASH)) &&
                        (!jcrConfigPath.startsWith(FILE_PREFIX))) {
                    jcrConfigPath = ArtifactoryHome.getEtcDir().getAbsoluteFile() + FORWARD_SLASH + jcrConfigPath;
                }

                File requestedResource = new File(jcrConfigPath, resourceName);

                if (requestedResource.exists()) {
                    try {
                        is = new FileInputStream(requestedResource);
                    } catch (FileNotFoundException e) {
                        is = null;
                        throw new IllegalArgumentException(e.getMessage());
                    }

                    if (is != null) {
                        return is;
                    }
                }
            }
            is = getFallbackInputStream();
        }
        return is;
    }

    protected InputStream getFallbackInputStream() {
        String resName = "/META-INF/jcr/" + resourceName;
        InputStream result = getClass().getResourceAsStream(resName);
        if (result == null) {
            throw new RuntimeException(
                    "Did not find resource " + resName + " in the classpath");
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
