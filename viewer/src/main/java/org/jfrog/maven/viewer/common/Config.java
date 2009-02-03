/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jfrog.maven.viewer.common;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.log4j.Logger;
import org.apache.maven.settings.MavenSettingsBuilder;
import java.io.File;
import java.net.URL;

//TODO Create a real configuration file support

/**
 * User: Dror Bereznitsky
 * Date: 10/11/2006
 * Time: 18:09:46
 */
public class Config {
    private static final Logger logger = Logger.getLogger(Config.class);

    // Configuration property names
    private static final String MAVEN_HOME = "maven.home";
    private static final String MAVEN_SETTINGS = MavenSettingsBuilder.ALT_USER_SETTINGS_XML_LOCATION;
    private static final String LOCAL_REPO = MavenSettingsBuilder.ALT_LOCAL_REPOSITORY_LOCATION;
    private static final String CENTRAL_REPO = "maven.remote.repo.central";
    private static final String LAST_OPEN_FOLDER = "folder.last.open";
    private static final String LAST_SAVE_FOLDER = "folder.last.save";
    private static final String TRANSITIVE_DEPTH = "transitive.depth";
    private static final String OFFLINE = "offline";

    // Default config file name
    private static final String CONFIG_FILE_NAME = "viewer.properties";

    // System property for setting alternative config file path
    private static final String CONFIG_FILE_PATH = "config.file.path";

    private static PropertiesConfiguration config;

    public static void init() {
        try {
            File configFile = null;
            // First try the config.file.path system property
            if (System.getProperty(CONFIG_FILE_PATH) != null) {
                configFile = new File(System.getProperty(CONFIG_FILE_PATH));
                if (!configFile.exists() || !configFile.isFile()) {
                    logger.warn("Could not find config file or not a file: " +
                            configFile.getPath() +
                            ". Using default config file path.");
                    configFile = null;
                }
            }
            // Try to find the config file somewhere in the classpath using classloaders
            if (configFile == null) {
                URL configUrl = getResource(CONFIG_FILE_NAME);
                configFile = new File(configUrl.getFile());
                if (!configFile.exists()) {
                    logger.warn("Could not find config file at default path. Using default values.");
                    configFile = null;
                }
            }
            if (configFile != null) {
                logger.info("Using config file found at: " + configFile.getAbsolutePath());
                config = new PropertiesConfiguration(configFile);
            } else {
                config = new PropertiesConfiguration();
            }
        } catch (ConfigurationException e) {
            logger.warn(e);
            config = new PropertiesConfiguration();
        }
        setDefaultValues();
    }

    private static URL getResource(String resource) {
        ClassLoader classLoader;
        URL url;

        // Try using the context classloader
        classLoader = Thread.currentThread().getContextClassLoader();
        if(classLoader != null) {
            url = classLoader.getResource(resource);
            if(url != null) {
              return url;
            }
        }

        // Try using the classloader that loaded this class
        classLoader = Config.class.getClassLoader();
        if(classLoader != null) {
            url = classLoader.getResource(resource);
            if(url != null) {
              return url;
            }
        }

        // If nothing else works try this
        return ClassLoader.getSystemResource(resource);
    }

    private static void setDefaultValues() {
        checkAndSetDefaultValue(LOCAL_REPO,
                null,
                true);
        checkAndSetDefaultValue(CENTRAL_REPO,
                null,
                false);
        checkAndSetDefaultValue(MAVEN_HOME,
                null,
                true);
        checkAndSetDefaultValue(MAVEN_SETTINGS,
                null,
                true);
        checkAndSetDefaultValue(TRANSITIVE_DEPTH,
                3,
                false);
        checkAndSetDefaultValue(OFFLINE,
                false,
                false);

        if (config.getString(LAST_OPEN_FOLDER) == null) {
            config.setProperty(LAST_OPEN_FOLDER, config.getString(LOCAL_REPO));
        }

        if (config.getString(LAST_SAVE_FOLDER) == null) {
            config.setProperty(LAST_SAVE_FOLDER, System.getProperty("user.dir"));
        }
    }

    private static Object checkAndSetDefaultValue(String propertyKey, Object defaultValue, boolean setSystemProperty) {
        // First check from file
        Object value = config.getString(propertyKey);
        if (value == null) {
            // Then from system property (-DXXX=value)
            value = System.getProperty(propertyKey);
            if (value == null) {
                // No value in file and system props use default
                value = defaultValue;
            } else {
                // Check type validity
                if (defaultValue != null && !(defaultValue instanceof String)) {
                    if (defaultValue instanceof Integer) {
                        value = new Integer(value.toString());
                    }
                }
            }
            if (value != null) {
                config.setProperty(propertyKey, value);
            }
        }
        if (value != null && setSystemProperty) {
            System.setProperty(propertyKey, value.toString());
        }
        return value;
    }

    public static String getCentralRemoteRepository() {
        return config.getString(CENTRAL_REPO);
    }

    public static String getLocalRepository() {
        return config.getString(LOCAL_REPO);
    }

    public static int getTransitiveDepth() {
        return config.getInt(TRANSITIVE_DEPTH);
    }

    public static String getLastOpenFolder() {
        return config.getString(LAST_OPEN_FOLDER);
    }

    public static String getLastSaveFolder() {
        return config.getString(LAST_SAVE_FOLDER);
    }

    public static String getMavenHome() {
        return config.getString(MAVEN_HOME);
    }

    public static void setLastOpenFolder(String folder) {
        config.setProperty(LAST_OPEN_FOLDER, folder);
    }

    public static void setLastSaveFolder(String folder) {
        config.setProperty(LAST_SAVE_FOLDER, folder);
    }

    public static Boolean isOffline() {
        return config.getBoolean(OFFLINE);
    }

    public static void setOffline(Boolean value) {
        config.setProperty(OFFLINE, value);    
    }

    public static void save() throws ConfigurationException {
        config.save();
    }

    public static void setLocalRepository(String localRepositoryPath) {
        config.setProperty(LOCAL_REPO, localRepositoryPath);
        config.setProperty(LAST_OPEN_FOLDER, localRepositoryPath);
    }
}
