package org.artifactory.io;

import org.artifactory.common.ResourceStreamHandle;

import java.io.IOException;
import java.io.InputStream;

/**
 * User: freds Date: Jun 1, 2008 Time: 8:39:25 PM
 */
public class ClasspathResourceLoader implements ResourceStreamHandle {
    private final String resourceName;
    private InputStream is;

    public ClasspathResourceLoader(String resourceName) {
        this.resourceName = resourceName;
    }

    public InputStream getInputStream() {
        if (is == null) {
            is = getClass().getResourceAsStream(resourceName);
            if (is == null) {
                throw new RuntimeException(
                        "Did not find resource " + resourceName + " in the classpath");
            }
        }
        return is;
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
