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

import java.io.File;
import java.io.IOException;
import java.net.URL;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public abstract class ArtifactoryHome {
    public static String SYS_PROP = "artifactory.home";
    public static String ENV_VAR = "ARTIFACTORY_HOME";

    public static final String ARTIFACTORY_CONFIG_FILE = "artifactory.config.xml";
    public static final String ARTIFACTORY_SYSTEM_PROPERTIES_FILE =
            "artifactory.system.properties";
    public static final String ARTIFACTORY_PROPERTIES_FILE = "artifactory.properties";
    private static final String LOGBACK_CONFIG_FILE_NAME = "logback.xml";

    public static final File TEMP_FOLDER =
            new File(System.getProperty("java.io.tmpdir"), "artifactory-uploads");

    private static boolean readOnly = false;
    private static IllegalArgumentException initFailCause = null;
    private static File homeDir;
    private static File etcDir;
    private static File dataDir;
    private static File jcrRootDir;
    private static File workingCopyDir;
    private static File logDir;
    private static File backupDir;
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

    public static File getBackupDir() {
        return backupDir;
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
            setDataAndJcrDir();
            logDir = getOrCreateSubDir("logs");
            backupDir = getOrCreateSubDir("backup");
            File jettyWorkDir = getOrCreateSubDir("work");
            workingCopyDir = getOrCreateSubDir(dataDir, "wc");
            tmpDir = getOrCreateSubDir(dataDir, "tmp");
            tmpUploadsDir = getOrCreateSubDir(tmpDir, "artifactory-uploads");

            //Manage the artifactory.system.properties file under etc dir
            initAndLoadSystemPropertyFile();
            // Manage config file
            File localConfigFile = findLocalConfigFile();
            setConfigFile(localConfigFile);

            //Check the write access to all directories that need it
            checkWritableDirectory(dataDir);
            checkWritableDirectory(jcrRootDir);
            if (!readOnly) {
                checkWritableDirectory(logDir);
                checkWritableDirectory(backupDir);
                checkWritableDirectory(jettyWorkDir);
                checkWritableDirectory(workingCopyDir);
                checkWritableDirectory(tmpDir);
                checkWritableDirectory(tmpUploadsDir);
            }
        } catch (Exception e) {
            initFailCause = new IllegalArgumentException(
                    "Could not initialize artifactory main directory due to " + e.getMessage(), e);
            throw initFailCause;
        }
    }

    public static void setDataAndJcrDir() throws IOException {
        dataDir = getOrCreateSubDir("data");
        jcrRootDir = dataDir;
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

    /**
     * Chacks the existence of the logback configuration file under the etc directory. If the file doesn't exist this
     * method will extract a default one from the war.
     *
     * @param logbackConfigLocation The location to check
     */
    public static void ensureLogbackConfig(String logbackConfigLocation) {
        if ("file:${artifactory.home}/etc/logback.xml".equals(logbackConfigLocation)) {
            File etcDir = new File(getHomeDir(), "etc");
            File logbackFile = new File(etcDir, LOGBACK_CONFIG_FILE_NAME);
            if (!logbackFile.exists()) {
                try {
                    //Copy from default
                    URL configUrl = ArtifactoryHome.class.getResource(
                            "/META-INF/default/" + LOGBACK_CONFIG_FILE_NAME);
                    FileUtils.copyURLToFile(configUrl, logbackFile);
                } catch (IOException e) {
                    // we don't have the logger configuration - use System.err
                    System.err.printf("Could not create default %s into %s",
                            LOGBACK_CONFIG_FILE_NAME, logbackFile);
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Assert to stop init process if home dir is in invalid state
     */
    public static void assertInitialized() {
        if (initFailCause != null) {
            throw initFailCause;
        }
    }

    /**
     * Missing Closure ;-)
     */
    public interface SimpleLog {
        public void log(String message);
    }

    public static File getArtifactoryPropertiesFile() {
        return new File(dataDir, ARTIFACTORY_PROPERTIES_FILE);
    }

    public static URL getDefaultArtifactoryPropertiesUrl() {
        return ArtifactoryHome.class.getResource("/META-INF/" + ARTIFACTORY_PROPERTIES_FILE);
    }

    /**
     * Copy the system properties file and set its data as system properties
     */
    private static void initAndLoadSystemPropertyFile() {
        // Expose the properties inside artfactory.properties and artfactory.system.properties
        // as system properties, availale to ArtifactoryConstants
        File systemPropertiesFile = new File(etcDir, ARTIFACTORY_SYSTEM_PROPERTIES_FILE);
        if (!readOnly && !systemPropertiesFile.exists()) {
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

        File artifactoryPropertiesFile = getArtifactoryPropertiesFile();
        if (!readOnly && !artifactoryPropertiesFile.exists()) {
            //Copy the artifactory.properties file into the data folder
            try {
                //Copy from default
                URL url = getDefaultArtifactoryPropertiesUrl();
                FileUtils.copyURLToFile(url, artifactoryPropertiesFile);
            } catch (IOException e) {
                throw new RuntimeException("Could not copy " +
                        ARTIFACTORY_PROPERTIES_FILE + " to " +
                        artifactoryPropertiesFile.getAbsolutePath(), e);
            }
        }
        ArtifactoryProperties.get().loadArtifactorySystemProperties(systemPropertiesFile, artifactoryPropertiesFile);
    }

    private static File findLocalConfigFile() {
        String configFilePath = ConstantsValue.config.getString();
        File localConfigFile;
        if (configFilePath == null) {
            localConfigFile = new File(etcDir, ARTIFACTORY_CONFIG_FILE);
            if (!localConfigFile.exists()) {
                if (readOnly) {
                    throw new RuntimeException("Could not read the default " + ARTIFACTORY_CONFIG_FILE + " at " +
                            localConfigFile.getAbsolutePath());
                } else {
                    try {
                        //Copy from default
                        URL configUrl =
                                ArtifactoryHome.class.getResource("/META-INF/default/" + ARTIFACTORY_CONFIG_FILE);
                        FileUtils.copyURLToFile(configUrl, localConfigFile);
                    } catch (IOException e) {
                        throw new RuntimeException("Could not create a default " +
                                ARTIFACTORY_CONFIG_FILE + " at " +
                                localConfigFile.getAbsolutePath(), e);
                    }
                }
            }
            ArtifactoryProperties.get().setProperty(ConstantsValue.config.getPropertyName(), localConfigFile.getPath());
        } else {
            localConfigFile = new File(configFilePath);
        }
        return localConfigFile;
    }

    private static void checkWritableDirectory(File dir) {
        if (!dir.exists() || !dir.isDirectory() || !dir.canWrite()) {
            throw new IllegalArgumentException("Failed to create writable directory: " +
                    dir.getAbsolutePath());
        }
    }
}
