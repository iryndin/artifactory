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

package org.artifactory.rest.common.model.reverseproxy;

import org.artifactory.descriptor.repo.ReverseProxyMethod;
import org.artifactory.descriptor.repo.WebServerType;

public class ReverseProxyDescriptorModel {
    
    private String key = "nginx";
    private WebServerType webServerType;
    private String artifactoryAppContext;
    private String publicAppContext;
    private String serverName;
    private String serverNameExpression;
    private String artifactoryServerName;
    private String upStreamName;
    private int artifactoryPort = 8081;
    private String sslCertificate;
    private String sslKey;
    private ReverseProxyMethod dockerReverseProxyMethod;
    private boolean useHttps;
    private boolean useHttp;
    private int sslPort = 443;
    private int httpPort= 80;

    private ReverseProxyRepositories reverseProxyRepositories;

    public ReverseProxyRepositories getReverseProxyRepositories() {
        return reverseProxyRepositories;
    }

    public void setReverseProxyRepositories(ReverseProxyRepositories reverseProxyRepositories) {
        this.reverseProxyRepositories = reverseProxyRepositories;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public WebServerType getWebServerType() {
        return webServerType;
    }

    public void setWebServerType(WebServerType webServerType) {
        this.webServerType = webServerType;
    }

    public String getArtifactoryAppContext() {
        return artifactoryAppContext;
    }


    public void setArtifactoryAppContext(String artifactoryAppContext) {
        this.artifactoryAppContext = artifactoryAppContext;
    }



    public String getPublicAppContext() {
        return publicAppContext;
    }

    public void setPublicAppContext(String publicAppContext) {
        this.publicAppContext = publicAppContext;
    }

    public String getServerName() {
        return serverName;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    public String getServerNameExpression() {
        return serverNameExpression;
    }

    public void setServerNameExpression(String serverNameExpression) {
        this.serverNameExpression = serverNameExpression;
    }

    public String getSslCertificate() {
        return sslCertificate;
    }

    public void setSslCertificate(String sslCertificate) {
        this.sslCertificate = sslCertificate;
    }

    public String getSslKey() {
        return sslKey;
    }

    public void setSslKey(String sslKey) {
        this.sslKey = sslKey;
    }

    public ReverseProxyMethod getDockerReverseProxyMethod() {
        return dockerReverseProxyMethod;
    }

    public void setDockerReverseProxyMethod(ReverseProxyMethod dockerReverseProxyMethod) {
        this.dockerReverseProxyMethod = dockerReverseProxyMethod;
    }

    public boolean isUseHttps() {
        return useHttps;
    }

    public void setUseHttps(boolean useHttps) {
        this.useHttps = useHttps;
    }

    public int getSslPort() {
        return sslPort;
    }

    public void setSslPort(int sslPort) {
        this.sslPort = sslPort;
    }

    public int getHttpPort() {
        return httpPort;
    }

    public void setHttpPort(int httpPort) {
        this.httpPort = httpPort;
    }

    public String getUpStreamName() {
        return upStreamName;
    }

    public void setUpStreamName(String upStreamName) {
        this.upStreamName = upStreamName;
    }

    public String getArtifactoryServerName() {
        return artifactoryServerName;
    }

    public void setArtifactoryServerName(String artifactoryServerName) {
        this.artifactoryServerName = artifactoryServerName;
    }

    public int getArtifactoryPort() {
        return artifactoryPort;
    }

    public void setArtifactoryPort(int artifactoryPort) {
        this.artifactoryPort = artifactoryPort;
    }

    public boolean isUseHttp() {
        return useHttp;
    }

    public void setUseHttp(boolean useHttp) {
        this.useHttp = useHttp;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ReverseProxyDescriptorModel that = (ReverseProxyDescriptorModel) o;
        return key.equals(that.key);
    }

    @Override
    public int hashCode() {
        return key.hashCode();
    }
}
