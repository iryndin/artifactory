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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import java.io.File;
import static java.lang.Integer.parseInt;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * @author freds
 * @date Oct 12, 2008
 */
public class ArtifactoryPropertiesTest {
    private final static Logger log = LoggerFactory.getLogger(ArtifactoryPropertiesTest.class);

    @AfterMethod
    public void clearArtifactoryProperties() {
        // clear any property that might have been set in a test
        for (ConstantsValue constantsValue : ConstantsValue.values()) {
            System.clearProperty(constantsValue.getPropertyName());
        }
    }

    @Test
    public void printArtifactorySystemFile() {
        ConstantsValue[] constantsValues = ConstantsValue.values();
        StringBuilder builder = new StringBuilder("Default Properties:\n");
        for (ConstantsValue value : constantsValues) {
            builder.append("#").append(value.getPropertyName()).append("=")
                    .append(value.getDefValue()).append("\n");
        }
        String propFile = builder.toString();
        log.info(propFile);
    }

    @Test
    public void testLoadProps() throws URISyntaxException {
        URL sysPropsUrl = getClass().getResource("/config/system/artifactory.system.1.properties");
        File file = new File(sysPropsUrl.toURI());
        ArtifactoryProperties.get().loadArtifactorySystemProperties(file, null);
        Assert.assertEquals(ConstantsValue.ajaxRefreshMilis.getInt(), 1000);
        Assert.assertEquals(ConstantsValue.lockTimeoutSecs.getInt(),
                parseInt(ConstantsValue.lockTimeoutSecs.getDefValue()));
        Assert.assertEquals(ConstantsValue.authenticationCacheIdleTimeSecs.getInt(), 50);
        Assert.assertEquals(ConstantsValue.jcrFixConsistency.getBoolean(), true);
        Assert.assertEquals(ConstantsValue.searchMaxResults.getInt(),
                parseInt(ConstantsValue.searchMaxResults.getDefValue()));
        Assert.assertEquals(ConstantsValue.suppressPomConsistencyChecks.getBoolean(), false);
    }

    @Test
    public void testSystemProps() throws URISyntaxException {
        URL sysPropsUrl = getClass().getResource("/config/system/artifactory.system.1.properties");
        File file = new File(sysPropsUrl.toURI());
        System.setProperty(ConstantsValue.authenticationCacheIdleTimeSecs.getPropertyName(), "800");

        ArtifactoryProperties.get().loadArtifactorySystemProperties(file, null);
        Assert.assertEquals(ConstantsValue.ajaxRefreshMilis.getInt(), 1000);
        Assert.assertEquals(ConstantsValue.lockTimeoutSecs.getInt(), 120);
        Assert.assertEquals(ConstantsValue.authenticationCacheIdleTimeSecs.getInt(), 800);
        Assert.assertEquals(ConstantsValue.jcrFixConsistency.getBoolean(), true);
        Assert.assertEquals(ConstantsValue.searchMaxResults.getInt(),
                parseInt(ConstantsValue.searchMaxResults.getDefValue()));
        Assert.assertEquals(ConstantsValue.suppressPomConsistencyChecks.getBoolean(), false);
    }

    @Test
    public void defaultArtifactoryVersion() throws URISyntaxException {
        ArtifactoryProperties.get().loadArtifactorySystemProperties(null, null);
        Assert.assertNull(ConstantsValue.artifactoryVersion.getString(), "Expected null but was " +
                ConstantsValue.artifactoryVersion.getString());
        Assert.assertNull(ConstantsValue.artifactoryRevision.getString(), "Expected null but was " +
                ConstantsValue.artifactoryRevision.getString());
    }

    @Test
    public void artifactoryVersion() throws URISyntaxException {
        URL artPropsUrl = getClass().getResource("/config/system/artifactory.properties");
        File file = new File(artPropsUrl.toURI());
        ArtifactoryProperties.get().loadArtifactorySystemProperties(null, file);
        Assert.assertEquals(ConstantsValue.artifactoryVersion.getString(), "10.3");
        Assert.assertEquals(ConstantsValue.artifactoryRevision.getInt(), 12345);
    }

    @Test
    public void systemPropsOverrideArtifactoryProperties() throws URISyntaxException {
        System.setProperty(ConstantsValue.artifactoryVersion.getPropertyName(), "3.0");
        System.setProperty(ConstantsValue.artifactoryRevision.getPropertyName(), "5555");

        URL artPropsUrl = getClass().getResource("/config/system/artifactory.properties");
        File file = new File(artPropsUrl.toURI());
        ArtifactoryProperties.get().loadArtifactorySystemProperties(null, file);
        Assert.assertEquals(ConstantsValue.artifactoryVersion.getString(), "3.0");
        Assert.assertEquals(ConstantsValue.artifactoryRevision.getInt(), 5555);
    }
}
