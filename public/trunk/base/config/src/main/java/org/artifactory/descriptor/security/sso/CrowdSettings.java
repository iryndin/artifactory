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
import org.artifactory.descriptor.Descriptor;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import java.util.Properties;

/**
 * Crowd connection settings descriptor
 *
 * @author Noam Y. Tenne
 */
@XmlType(name = "CrowdSettingsType", propOrder = {"enableIntegration", "serverUrl", "applicationName", "password",
        "sessionValidationInterval", "useDefaultProxy", "noAutoUserCreation", "customCookieTokenKey"},
        namespace = Descriptor.NS)
public class CrowdSettings implements Descriptor {

    @XmlElement(defaultValue = "false")
    private boolean enableIntegration = false;

    private String serverUrl;

    private String applicationName;

    private String password;

    private long sessionValidationInterval;

    @XmlElement(defaultValue = "false")
    private boolean useDefaultProxy = false;

    @XmlElement(defaultValue = "true")
    private boolean noAutoUserCreation = true;

    private String customCookieTokenKey;

    public boolean isEnableIntegration() {
        return enableIntegration;
    }

    public void setEnableIntegration(boolean enableIntegration) {
        this.enableIntegration = enableIntegration;
    }

    public String getServerUrl() {
        return serverUrl;
    }

    public void setServerUrl(String serverUrl) {
        this.serverUrl = serverUrl;
    }

    public String getApplicationName() {
        return applicationName;
    }

    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public long getSessionValidationInterval() {
        return sessionValidationInterval;
    }

    public void setSessionValidationInterval(long sessionValidationInterval) {
        this.sessionValidationInterval = sessionValidationInterval;
    }

    public boolean isUseDefaultProxy() {
        return useDefaultProxy;
    }

    public void setUseDefaultProxy(boolean useDefaultProxy) {
        this.useDefaultProxy = useDefaultProxy;
    }

    public boolean isNoAutoUserCreation() {
        return noAutoUserCreation;
    }

    public void setNoAutoUserCreation(boolean noAutoUserCreation) {
        this.noAutoUserCreation = noAutoUserCreation;
    }

    public String getCustomCookieTokenKey() {
        return customCookieTokenKey;
    }

    public void setCustomCookieTokenKey(String customCookieTokenKey) {
        this.customCookieTokenKey = customCookieTokenKey;
    }

    public Properties getConfigurationProperties() {
        Properties properties = new Properties();
        if (isEnableIntegration()) {
            properties.setProperty("crowd.server.url", getServerUrl());
            if (StringUtils.isNotBlank(applicationName)) {
                properties.setProperty("application.name", applicationName);
            }
            if (StringUtils.isNotBlank(password)) {
                properties.setProperty("application.password", password);
            }
            if (sessionValidationInterval > 0) {
                properties.setProperty("session.validationinterval", Long.toString(sessionValidationInterval));
            }
            properties.setProperty("session.isauthenticated", "artifactory.crowd.session.isAuthenticated");
            properties.setProperty("session.tokenkey", "artifactory.crowd.session.tokenKey");
            properties.setProperty("session.lastvalidation", "artifactory.crowd.session.lastValidation");
            if (StringUtils.isNotBlank(customCookieTokenKey)) {
                properties.setProperty("cookie.tokenkey", customCookieTokenKey);
            }
        }
        return properties;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CrowdSettings)) {
            return false;
        }

        CrowdSettings that = (CrowdSettings) o;

        if (enableIntegration != that.enableIntegration) {
            return false;
        }
        if (noAutoUserCreation != that.noAutoUserCreation) {
            return false;
        }
        if (sessionValidationInterval != that.sessionValidationInterval) {
            return false;
        }
        if (useDefaultProxy != that.useDefaultProxy) {
            return false;
        }
        if (applicationName != null ? !applicationName.equals(that.applicationName) : that.applicationName != null) {
            return false;
        }
        if (password != null ? !password.equals(that.password) : that.password != null) {
            return false;
        }
        if (serverUrl != null ? !serverUrl.equals(that.serverUrl) : that.serverUrl != null) {
            return false;
        }
        if (customCookieTokenKey != null ? !customCookieTokenKey.equals(that.customCookieTokenKey) :
                that.customCookieTokenKey != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = (enableIntegration ? 1 : 0);
        result = 31 * result + (serverUrl != null ? serverUrl.hashCode() : 0);
        result = 31 * result + (applicationName != null ? applicationName.hashCode() : 0);
        result = 31 * result + (password != null ? password.hashCode() : 0);
        result = 31 * result + (int) (sessionValidationInterval ^ (sessionValidationInterval >>> 32));
        result = 31 * result + (useDefaultProxy ? 1 : 0);
        result = 31 * result + (noAutoUserCreation ? 1 : 0);
        result = 31 * result + (customCookieTokenKey != null ? customCookieTokenKey.hashCode() : 0);
        return result;
    }
}
