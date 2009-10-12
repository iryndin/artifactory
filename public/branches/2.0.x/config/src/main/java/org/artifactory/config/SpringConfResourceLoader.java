package org.artifactory.config;

import org.artifactory.common.ArtifactoryHome;
import org.artifactory.common.ConstantsValue;
import org.artifactory.util.PathUtils;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

/**
 * This class acts as a resource locator for the spring configuration files. It recieves a request for the resources
 * paths and returns it (if found). It will first check if a path is specified in the ConstantsValue class, and if it
 * cannot find it there, (not specified, bad path, non-existant file, etc') it will try to find it in the resources
 * folder (META-INF/spring).
 *
 * @author Noam Tenne
 */
public class SpringConfResourceLoader {
    private static final String FORWARD_SLASH = "/";
    private static final String FILE_PREFIX = "file:";

    /**
     * Returns a String[] of the spring configuration files urls.
     *
     * @return String[] - Spring configuration files urls
     */
    public static String[] getConfigurationPaths() {
        String[] files = {"applicationContext.xml", "jcr.xml", "scheduling.xml", "security.xml"};
        ArrayList<String> paths = new ArrayList<String>();

        for (String file : files) {
            String path = getPath(file);
            if (path == null) {
                throw new RuntimeException(
                        "Cannot find the resource: " + file + " at any specified location");
            }
            paths.add(path);
        }

        if (paths.size() == 0) {
            throw new RuntimeException(
                    "Did not find any of the required resources");
        }
        String[] pathsToReturn = paths.toArray(new String[paths.size()]);
        return pathsToReturn;
    }

    /**
     * Returns a resources URL via name. Will look in path specified in ConstantsValue (if specified) Or in
     * META-INF/spring
     *
     * @param resourceName The name of the wanted resource
     * @return String - The url of the resource
     */
    private static String getPath(String resourceName) {
        String springConfPath = ConstantsValue.springConfigPath.getString();
        if (PathUtils.hasText(springConfPath)) {
            springConfPath = springConfPath.trim();

            if ((!springConfPath.startsWith(FORWARD_SLASH)) &&
                    (!springConfPath.startsWith(FILE_PREFIX))) {
                springConfPath = ArtifactoryHome.getEtcDir().getAbsoluteFile() + FORWARD_SLASH +
                        springConfPath;
            }

            File requestedResource = new File(springConfPath, resourceName);
            if (requestedResource.exists()) {
                URL url;
                try {
                    url = requestedResource.toURL();
                } catch (MalformedURLException e) {
                    throw new RuntimeException(
                            "Given url at: " + requestedResource.getAbsolutePath() +
                                    " is malformed");
                }
                if (url != null) {
                    return url.toExternalForm();
                }
            }
        }

        return "/META-INF/spring/" + resourceName;
    }
}
