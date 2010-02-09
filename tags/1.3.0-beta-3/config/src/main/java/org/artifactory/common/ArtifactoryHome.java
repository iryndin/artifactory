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
package org.artifactory.common;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Properties;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public abstract class ArtifactoryHome {
    public static String SYS_PROP = "artifactory.home";
    public static String ENV_VAR = "ARTIFACTORY_HOME";

    public static final String SYS_PROP_ARTIFACTORY_CONFIG = "artifactory.config.file";

    public static final String ARTIFACTORY_CONFIG_FILE = "artifactory.config.xml";
    private static final String ARTIFACTORY_SYSTEM_PROPERTIES_FILE =
            "artifactory.system.properties";
    private static final String ARTIFACTORY_PROPERTIES_FILE = "artifactory.properties";
    private static final String LOG4J_PROPERTIES_FILE = "log4j.properties";

    public static final File TEMP_FOLDER =
            new File(System.getProperty("java.io.tmpdir"), "artifactory-uploads");

    private static boolean readOnly = false;
    private static File homeDir;
    private static File etcDir;
    private static File dataDir;
    private static File jcrRootDir;
    private static File workingCopyDir;
    private static File logDir;
    private static File configFile;
    private static File tmpDir;
    private static File tmpUploadsDir;

    public static void setHomeDir(File homeDir) {
        ArtifactoryHome.homeDir = homeDir;
        System.setProperty(SYS_PROP, homeDir.getAbsolutePath().replace('\\', '/'));
    }

    private static File getHomeDir() {
        if (homeDir == null) {
            findArtifactoryHome(new ArtifactoryHome.SimpleLog() {
                public void log(String message) {
                    System.out.println(message);
                }
            });
        }
        return homeDir;
    }

    /**
     * Flag used to avoid creation of new folders
     *
     * @return
     */
    public static boolean isReadOnly() {
        return readOnly;
    }

    public static void setReadOnly(boolean readOnly) {
        ArtifactoryHome.readOnly = readOnly;
    }

    public static File getDataDir() {
        return dataDir;
    }

    public static File getJcrRootDir() {
        return jcrRootDir;
    }

    public static File getEtcDir() {
        return etcDir;
    }

    public static File getLogDir() {
        return logDir;
    }

    public static File getWorkingCopyDir() {
        return workingCopyDir;
    }

    public static File getTmpDir() {
        return tmpDir;
    }

    public static File getTmpUploadsDir() {
        return tmpUploadsDir;
    }

    public static void setJcrRootDir(File dir) {
        jcrRootDir = dir;
    }

    public static File getOrCreateSubDir(String subDirName) throws IOException {
        return getOrCreateSubDir(getHomeDir(), subDirName);
    }

    public static File getOrCreateSubDir(File parent, String subDirName) throws IOException {
        File subDir = new File(parent, subDirName);
        if (readOnly) {
            if (!subDir.exists() || !subDir.isDirectory()) {
                throw new RuntimeException("Sub directory " + subDir + " cannot be accessed.");
            }
        }
        FileUtils.forceMkdir(subDir);
        return subDir;
    }

    public static File getConfigFile() {
        if (configFile == null) {
            throw new RuntimeException("Artifactory home folder was not created properly.");
        }
        return configFile;
    }

    public static void create() {
        try {
            // Create or find all the needed subfolders
            etcDir = getOrCreateSubDir("etc");
            dataDir = getOrCreateSubDir("data");
            jcrRootDir = dataDir;
            if (!readOnly) {
                logDir = getOrCreateSubDir("logs");
                getOrCreateSubDir("work");
                workingCopyDir = getOrCreateSubDir(dataDir, "wc");
                tmpDir = new File(System.getProperty("java.io.tmpdir"));
                tmpUploadsDir = getOrCreateSubDir(tmpDir, "artifactory-uploads");
            }
        } catch (IOException e) {
            throw new IllegalArgumentException(
                    "Could not create or access artifactory main directory " + e.getMessage(), e);
        }
        // Manage config file
        File localConfigFile = findLocalConfigFile();
        setConfigFile(localConfigFile);
        if (!readOnly) {
            //Manage the artifactory.system.properties file under etc dir
            initAndLoadSystemPropertyFile();
            //Copy the artifactory.properties file into the data folder
            copyPropertyFile();
        }
    }

    public static void setConfigFile(File newConfigFile) {
        if (!newConfigFile.exists() || !newConfigFile.isFile()) {
            throw new IllegalArgumentException(
                    "File " + newConfigFile.getAbsolutePath() +
                            " does not exists or is not a file.");
        }
        configFile = newConfigFile;
    }

    public static String findArtifactoryHome(SimpleLog logger) {
        logger.log("Determining " + SYS_PROP + "...");
        logger.log("Looking for '-D" + SYS_PROP + "=<path>' vm parameter...");
        String home = System.getProperty(SYS_PROP);
        if (home == null) {
            logger.log("Could not find vm parameter.");
            //Try the environment var
            logger.log("Looking for " + ENV_VAR + " environment variable...");
            home = System.getenv(ENV_VAR);
            if (home == null) {
                logger.log("Could not find environment variable.");
                home = new File(System.getProperty("user.home", "."), ".artifactory")
                        .getAbsolutePath();
                logger.log("Defaulting to '" + home + "'...");
            } else {
                logger.log("Found environment variable value: " + home + ".");
            }
        } else {
            logger.log("Found vm parameter value: " + home + ".");
        }

        home = home.replace('\\', '/');
        logger.log("Using artifactory.home at '" + home + "'.");
        setHomeDir(new File(home));
        return home;
    }

    public static void checkLog4j(String log4jConfigLocation) {
        if ("file:${artifactory.home}/etc/log4j.properties".equals(log4jConfigLocation)) {
            File etcDir = new File(getHomeDir(), "etc");
            File log4jfile = new File(etcDir, LOG4J_PROPERTIES_FILE);
            if (!log4jfile.exists()) {
                try {
                    //Copy from default
                    URL configUrl =
                            ArtifactoryHome.class.getResource(
                                    "/META-INF/default/" + LOG4J_PROPERTIES_FILE);
                    FileUtils.copyURLToFile(configUrl, log4jfile);
                } catch (IOException e) {
                    System.err.println(
                            "Could not create default " + LOG4J_PROPERTIES_FILE + " into " +
                                    log4jfile);
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Missing Closure ;-)
     */
    public interface SimpleLog {
        public void log(String message);
    }

    /**
     * Copy the artifactory.properties file containing internal use information into the data
     * folder
     */
    private static void copyPropertyFile() {
        File artifactoryPropertiesFile = new File(dataDir, ARTIFACTORY_PROPERTIES_FILE);
        try {
            //Copy from default
            URL url = ArtifactoryHome.class.getResource("/META-INF/" + ARTIFACTORY_PROPERTIES_FILE);
            FileUtils.copyURLToFile(url, artifactoryPropertiesFile);
        } catch (IOException e) {
            throw new RuntimeException("Could not copy " +
                    ARTIFACTORY_PROPERTIES_FILE + " to " +
                    artifactoryPropertiesFile.getAbsolutePath(), e);
        }
    }

    /**
     * Copy the system properties file and set its data as system properties
     */
    private static void initAndLoadSystemPropertyFile() {
        //Expose the properties inside artfactory.properties as system properties, availale to
        //ArtifactoryConstants
        File systemPropertiesFile = new File(etcDir, ARTIFACTORY_SYSTEM_PROPERTIES_FILE);
        if (!systemPropertiesFile.exists()) {
            try {
                //Copy from default
                URL url = ArtifactoryHome.class
                        .getResource("/META-INF/default/" + ARTIFACTORY_SYSTEM_PROPERTIES_FILE);
                FileUtils.copyURLToFile(url, systemPropertiesFile);
            } catch (IOException e) {
                throw new RuntimeException("Could not create a default " +
                        ARTIFACTORY_SYSTEM_PROPERTIES_FILE + " at " +
                        systemPropertiesFile.getAbsolutePath(), e);
            }
        }
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(systemPropertiesFile);
            Properties props = new Properties();
            props.load(fis);
            for (Object key : props.keySet()) {
                String value = (String) props.get(key);
                System.setProperty((String) key, value);
            }
        } catch (Exception e) {
            throw new RuntimeException("Could not read default system properties from '" +
                    systemPropertiesFile.getAbsolutePath() + "'.", e);
        } finally {
            IOUtils.closeQuietly(fis);
        }
    }

    private static File findLocalConfigFile() {
        String configFilePath = System.getProperty(SYS_PROP_ARTIFACTORY_CONFIG);
        File localConfigFile;
        if (configFilePath == null) {
            localConfigFile = new File(etcDir, ARTIFACTORY_CONFIG_FILE);
            if (!localConfigFile.exists()) {
                if (readOnly) {
                    throw new RuntimeException("Could not read the default " +
                            ARTIFACTORY_CONFIG_FILE + " at " +
                            localConfigFile.getAbsolutePath());
                } else {
                    try {
                        //Copy from default
                        URL configUrl = ArtifactoryHome.class
                                .getResource("/META-INF/default/" + ARTIFACTORY_CONFIG_FILE);
                        FileUtils.copyURLToFile(configUrl, localConfigFile);
                    } catch (IOException e) {
                        throw new RuntimeException("Could not create a default " +
                                ARTIFACTORY_CONFIG_FILE + " at " +
                                localConfigFile.getAbsolutePath(), e);
                    }
                }
            }
        } else {
            localConfigFile = new File(configFilePath);
        }
        return localConfigFile;
    }
}
