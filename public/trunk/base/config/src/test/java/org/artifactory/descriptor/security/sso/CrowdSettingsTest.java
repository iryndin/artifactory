/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2012 JFrog Ltd.
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

package org.artifactory.descriptor.security.sso;

import org.apache.commons.lang.StringUtils;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Properties;

import static org.testng.Assert.*;

/**
 * Tests the behavior of the Crowd settings descriptor
 *
 * @author Noam Y. Tenne
 */
@Test
public class CrowdSettingsTest {

    /**
     * Tests the validity of the default values
     */
    public void testDefaultConstructor() {
        CrowdSettings crowdSettings = new CrowdSettings();
        assertNull(crowdSettings.getServerUrl(), "Default server URL should be null.");
        assertNull(crowdSettings.getApplicationName(), "Default application name should be null.");
        assertNull(crowdSettings.getPassword(), "Default password should be null.");
        assertEquals(crowdSettings.getSessionValidationInterval(), 0L,
                "Default session validation interval should be 0.");
        assertFalse(crowdSettings.isEnableIntegration(), "Integration should not be enabled by default.");
        assertTrue(crowdSettings.isNoAutoUserCreation(), "Auto user creation should not be enabled by default.");
        assertFalse(crowdSettings.isUseDefaultProxy(),
                "Use of default proxy configuration should not be enabled by default.");
    }

    /**
     * Tests the behavior of the client properties generator method
     */
    public void testClientPropertiesProduction() {
        String serverUrl = "http://my.url";
        String applicationName = "artifactory";
        String password = "secret";
        long sessionValidationInterval = 1337;

        CrowdSettings crowdSettings = new CrowdSettings();
        crowdSettings.setServerUrl(serverUrl);
        crowdSettings.setApplicationName(applicationName);
        crowdSettings.setPassword(password);
        crowdSettings.setSessionValidationInterval(sessionValidationInterval);

        //First test property generation when org.artifactory.descriptor.security.sso.CrowdSettings.enableIntegration is false
        Properties configurationProperties = crowdSettings.getConfigurationProperties();
        Assert.assertTrue(configurationProperties.isEmpty(),
                "Generated configuration properties should be empty since the integration is not enabled.");

        //Now "enable" the integration and test the properties
        crowdSettings.setEnableIntegration(true);
        configurationProperties = crowdSettings.getConfigurationProperties();
        assertEquals(configurationProperties.getProperty("crowd.server.url"), serverUrl
                , "Unexpected server URL property value");
        assertEquals(configurationProperties.getProperty("application.name"), applicationName,
                "Unexpected application name property value");
        assertEquals(configurationProperties.getProperty("application.password"), password,
                "Unexpected password property value");
        assertEquals(Long.parseLong(configurationProperties.getProperty("session.validationinterval")),
                sessionValidationInterval, "Unexpected session validation interval property value");
        assertTrue(StringUtils.isNotBlank(configurationProperties.getProperty("session.isauthenticated")),
                "session.isauthenticated session attribute key must have a value.");
        assertTrue(StringUtils.isNotBlank(configurationProperties.getProperty("session.tokenkey")),
                "session.tokenkey session attribute key must have a value.");
        assertTrue(StringUtils.isNotBlank(configurationProperties.getProperty("session.lastvalidation")),
                "session.lastvalidation session attribute key must have a value.");
    }
}
