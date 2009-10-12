package org.artifactory.update.test;

import java.io.File;
import java.io.InputStream;
import java.net.URL;

/**
 * Helper methods for testing.
 *
 * @author Yossi Shaul
 */
public class TestUtils {

    public static InputStream getResource(String path) {
        InputStream is = TestUtils.class.getResourceAsStream(path);
        if (is == null) {
            throw new RuntimeException("Cannot find resource " + path);
        }
        return is;
    }

    public static File getResourceAsFile(String path) {
        URL resource = TestUtils.class.getResource(path);
        if (resource == null) {
            throw new RuntimeException("Resource not found: " + path);
        }
        return new File(resource.getFile());
    }
}
