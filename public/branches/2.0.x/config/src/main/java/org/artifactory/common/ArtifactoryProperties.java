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

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public class ArtifactoryProperties {

    private static final ArtifactoryProperties instance = new ArtifactoryProperties();

    /**
     * The combine properties of System, artifactory.system.properties file and artifactory.properties file. All System
     * properties starting with 'artifactory.' will be included.
     */
    private Properties artifactoryProperties = new Properties();

    /**
     * A map of substitue repo keys (oldKey:newKey) for supporting old repository keys (that are invalid xml ids).
     */
    private Map<String, String> substituteRepoKeys = new HashMap<String, String>();

    public static ArtifactoryProperties get() {
        return instance;
    }

    private ArtifactoryProperties() {
    }

    String getProperty(String key, String defaultValue) {
        return artifactoryProperties.getProperty(key, defaultValue);
    }

    public Map<String, String> getSubstituteRepoKeys() {
        return substituteRepoKeys;
    }

    public void loadArtifactorySystemProperties(File systemPropertiesFile, File artifactoryPropertiesFile) {
        Properties combinedProperties = new Properties();
        if (systemPropertiesFile != null && systemPropertiesFile.exists()) {
            FileInputStream fis = null;
            try {
                fis = new FileInputStream(systemPropertiesFile);
                combinedProperties.load(fis);
            } catch (Exception e) {
                throw new RuntimeException("Could not read default system properties from '" +
                        systemPropertiesFile.getAbsolutePath() + "'.", e);
            } finally {
                IOUtils.closeQuietly(fis);
            }
        }

        // load artifactory.properties (version and revision properties)
        if (artifactoryPropertiesFile != null && artifactoryPropertiesFile.exists()) {
            FileInputStream fis = null;
            try {
                // Load from file than override from the system props
                fis = new FileInputStream(artifactoryPropertiesFile);
                combinedProperties.load(fis);
            } catch (Exception e) {
                throw new RuntimeException("Could not read artifactory.properties from '" +
                        artifactoryPropertiesFile.getAbsolutePath() + "'.", e);
            } finally {
                IOUtils.closeQuietly(fis);
            }
        }

        // Override with System properties
        loadSystemProperties(combinedProperties);

        //Cleanup all non-artifactory system properties and set them as system properties
        for (Object key : combinedProperties.keySet()) {
            String propName = (String) key;
            String propValue = combinedProperties.getProperty(propName);
            if (!propName.startsWith(ConstantsValue.SYS_PROP_PREFIX)) {
                System.setProperty(propName, propValue);
            }
        }
        artifactoryProperties = combinedProperties;

        fillRepoKeySubstitute();
        // Clean constants values caches
        ConstantsValue.clearCache();
    }

    private static void loadSystemProperties(Properties result) {
        Properties properties = System.getProperties();
        for (Object key : properties.keySet()) {
            String sKey = (String) key;
            if (sKey.startsWith(ConstantsValue.SYS_PROP_PREFIX)) {
                result.put(sKey, properties.getProperty(sKey));
            }
        }
    }

    private void fillRepoKeySubstitute() {
        Map<String, String> result = new HashMap<String, String>();
        String prefix = ConstantsValue.substituteRepoKeys.getPropertyName();
        for (Object o : artifactoryProperties.keySet()) {
            String key = (String) o;
            if (key.startsWith(prefix)) {
                String oldRepoKey = key.substring(prefix.length());
                String newRepoKey = (String) artifactoryProperties.get(key);
                result.put(oldRepoKey, newRepoKey);
            }
        }
        substituteRepoKeys = result;
    }

    /**
     * Add the substitute to the substitute map and properties list.
     *
     * @param oldRepoKey
     * @param newRepoKey
     */
    public void addSubstitute(String oldRepoKey, String newRepoKey) {
        substituteRepoKeys.put(oldRepoKey, newRepoKey);
        artifactoryProperties
                .put(ConstantsValue.substituteRepoKeys.getPropertyName() + oldRepoKey, newRepoKey);
    }

    /**
     * Returns the propertiesCopy object
     *
     * @return Properties - The copy of the artifactoryProperties object
     */
    public Properties getPropertiesCopy() {
        Properties propertiesCopy = new Properties(artifactoryProperties);
        propertiesCopy.putAll(artifactoryProperties);
        return propertiesCopy;
    }

    /**
     * Set the value of the given key
     *
     * @param propertyName Property key
     * @param value        Property value
     */
    public void setProperty(String propertyName, String value) {
        artifactoryProperties.setProperty(propertyName, value);
    }

    public void store(OutputStream out) {
        try {
            artifactoryProperties.store(out, "");
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        }
    }
}