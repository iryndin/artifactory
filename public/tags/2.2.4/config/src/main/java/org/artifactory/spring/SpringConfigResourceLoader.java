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

package org.artifactory.spring;

import org.artifactory.addon.AddonInfo;
import org.artifactory.addon.AddonState;
import org.artifactory.common.ArtifactoryHome;
import org.artifactory.common.ConstantValues;
import org.artifactory.common.property.ArtifactorySystemProperties;
import org.artifactory.log.LoggerFactory;
import org.artifactory.util.PathUtils;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

/**
 * This class acts as a resource locator for the spring configuration files. It recieves a request for the resources
 * paths and returns it (if found). It will first check if a path is specified in the ConstantsValue class, and if it
 * cannot find it there, (not specified, bad path, non-existant file, etc') it will try to find it in the resources
 * folder (META-INF/spring).
 *
 * @author Noam Tenne
 */
public class SpringConfigResourceLoader {
    private final static Logger log = LoggerFactory.getLogger(SpringConfigResourceLoader.class);

    private static final String FORWARD_SLASH = "/";
    private static final String FILE_PREFIX = "file:";

    /**
     * Returns a String[] of the spring configuration files urls.
     *
     * @param artifactoryHome Artifactory home holding the config directory
     * @return String[] - Spring configuration files urls
     */
    public static SpringConfigPaths getConfigurationPaths(ArtifactoryHome artifactoryHome) {
        //Load the default config locations
        String[] springConfigs = {"applicationContext.xml", "jcr.xml", "scheduling.xml", "security.xml",
                "addons.xml", "interceptors.xml"};
        List<String> paths = new ArrayList<String>();

        for (String springConfig : springConfigs) {
            String path = getPath(springConfig, artifactoryHome);
            if (path == null) {
                throw new RuntimeException("Cannot find the resource: " + springConfig + " at any specified location");
            }
            paths.add(path);
        }
        Collections.sort(paths);

        if (paths.size() == 0) {
            throw new RuntimeException("Did not find any of the required resources");
        }

        Map<String, AddonInfo> addonContextPaths = loadEnabledAddons(artifactoryHome.getArtifactoryProperties());

        //TODO: [by yl] Load custom interceptors

        SpringConfigPaths configPaths = new SpringConfigPaths(paths, addonContextPaths);
        return configPaths;
    }

    private static Map<String, AddonInfo> loadEnabledAddons(ArtifactorySystemProperties props) {
        String[] disabledAddonsArr = props.getProperty(ConstantValues.disabledAddons).split(",");
        List<String> disabledAddons = new ArrayList<String>(disabledAddonsArr.length);
        for (String disabledAddon : disabledAddonsArr) {
            disabledAddons.add(disabledAddon.trim());
        }
        log.info("{}={}", ConstantValues.disabledAddons.getPropertyName(), disabledAddons);
        try {
            Enumeration<URL> addonsPropsUrls =
                    Thread.currentThread().getContextClassLoader().getResources("META-INF/addon.properties");
            Map<String, AddonInfo> addonsContextPathsByAddonName = new TreeMap<String, AddonInfo>();
            while (addonsPropsUrls.hasMoreElements()) {
                URL addonPropsUrl = addonsPropsUrls.nextElement();
                log.debug("Inspecting addon properties: {}", addonPropsUrl.toExternalForm());
                Properties addonProperties = new Properties();
                addonProperties.load(addonPropsUrl.openStream());
                String addonName = addonProperties.getProperty("name");
                String addonDisplayName = addonProperties.getProperty("display.name");

                if (addonName != null) {
                    String enabledAddon = PathUtils.getParent(addonPropsUrl.toExternalForm()) + "/addon.xml";
                    AddonState state;
                    if (!disabledAddons.contains(addonName)) {
                        // addon is enabled, add it's addon.xml to the spring context
                        log.info("Adding enabled addon: {}", addonName);
                        state = AddonState.INACTIVATED; //  if license is installed it will be activated by the manager 
                    } else {
                        log.info("Adding disabled addon: {}", addonName);
                        state = AddonState.DISABLED;
                    }
                    addonsContextPathsByAddonName.put(addonName,
                            new AddonInfo(addonName, addonDisplayName, enabledAddon, state, addonProperties));
                }
            }
            return addonsContextPathsByAddonName;
        } catch (IOException e) {
            throw new RuntimeException("Failed to load addons", e);
        }
    }

    /**
     * Returns a resources URL via name. Will look in path specified in ConstantsValue (if specified) Or in
     * META-INF/spring
     *
     * @param resourceName    The name of the wanted resource
     * @param artifactoryHome Artifactory home instance
     * @return String - The url of the resource
     */
    private static String getPath(String resourceName, ArtifactoryHome artifactoryHome) {
        String springConfPath = artifactoryHome.getArtifactoryProperties().getProperty(ConstantValues.springConfigDir);
        if (PathUtils.hasText(springConfPath)) {
            springConfPath = springConfPath.trim();

            if ((!springConfPath.startsWith(FORWARD_SLASH)) &&
                    (!springConfPath.startsWith(FILE_PREFIX))) {
                springConfPath = artifactoryHome.getEtcDir().getAbsoluteFile() + FORWARD_SLASH + springConfPath;
            }

            File requestedResource = new File(springConfPath, resourceName);
            if (requestedResource.exists()) {
                URL url;
                try {
                    url = requestedResource.toURI().toURL();
                } catch (MalformedURLException e) {
                    throw new RuntimeException(
                            "Given url at: " + requestedResource.getAbsolutePath() + " is malformed");
                }
                if (url != null) {
                    return url.toExternalForm();
                }
            }
        }

        return "/META-INF/spring/" + resourceName;
    }
}