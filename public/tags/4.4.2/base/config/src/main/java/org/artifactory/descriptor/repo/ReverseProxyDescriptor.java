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

package org.artifactory.descriptor.repo;

import org.artifactory.descriptor.Descriptor;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlType;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@XmlType(name = "ReverseProxyType",
        propOrder = {"key", "webServerType", "artifactoryAppContext", "publicAppContext", "serverName",
                "serverNameExpression", "sslCertificate", "sslKey", "dockerReverseProxyMethod", "useHttps","useHttp",
                "sslPort", "httpPort","reverseProxyRepoConfigs", "artifactoryServerName","upStreamName", "artifactoryPort"})
public class ReverseProxyDescriptor implements Descriptor {

    @XmlID
    @XmlElement(required = true)
    private String key = "nginx";
    @XmlElement(name = "webServerType", required = true)
    private WebServerType webServerType;
    private String artifactoryAppContext;
    @XmlElement(name = "publicAppContext", required = true)
    private String publicAppContext;
    @XmlElement(name = "serverName", required = true)
    private String serverName;
    @XmlElement(name = "serverNameExpression", nillable = true)
    private String serverNameExpression;
    @XmlElementWrapper(name = "reverseProxyRepositories" ,nillable = true)
    @XmlElement(name = "reverseProxyRepoConfigs", required = false)
    private List<ReverseProxyRepoConfig> reverseProxyRepoConfigs = new ArrayList<>();
    @XmlElement(name = "artifactoryServerName", required = true)
    private String artifactoryServerName;
    @XmlElement(name = "upStreamName", nillable = true)
    private String upStreamName;
    private int artifactoryPort = 8081;
    @XmlElement(name = "sslCertificate", nillable = true)
    private String sslCertificate;
    @XmlElement(name = "sslKey", nillable = true)
    private String sslKey;
    @XmlElement(name = "dockerReverseProxyMethod", required = true)
    private ReverseProxyMethod dockerReverseProxyMethod;
    private boolean useHttps = false;
    private boolean useHttp = true;
    private int sslPort = 443;
    private int httpPort= 80;

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


    public List<ReverseProxyRepoConfig> getReverseProxyRepoConfigs() {
        return reverseProxyRepoConfigs;
    }

    public ReverseProxyRepoConfig getReverseProxyRepoConfigsByRepoKey(String repoKey) {

        ReverseProxyRepoConfig reverseProxyRepo = null;

        for (ReverseProxyRepoConfig reverseProxyRepoConfig : reverseProxyRepoConfigs){
            if (reverseProxyRepoConfig.getRepoRef().getKey().equals(repoKey)){
                reverseProxyRepo = reverseProxyRepoConfig;
            }
        }
        return reverseProxyRepo;
    }

    public void setReverseProxyRepoConfigs(List<ReverseProxyRepoConfig> reverseProxyRepoConfigs) {
        this.reverseProxyRepoConfigs = reverseProxyRepoConfigs;
    }

    public void setArtifactoryAppContext(String artifactoryAppContext) {
        this.artifactoryAppContext = artifactoryAppContext;
    }

    public void deleteReverseProxyConfig(String repoKey) {
        Iterator<ReverseProxyRepoConfig> iterator = reverseProxyRepoConfigs.iterator();
        while (iterator.hasNext()){
            ReverseProxyRepoConfig nextReverseProxyConfig = iterator.next();
            if (nextReverseProxyConfig.getRepoRef().getKey().equals(repoKey)){
                iterator.remove();
            }
        }
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

    public void addReverseProxyRepoConfig(ReverseProxyRepoConfig reverseProxyRepoConfig){
        deleteReverseProxyConfig(reverseProxyRepoConfig.getRepoRef().getKey());
        reverseProxyRepoConfigs.add(reverseProxyRepoConfig);
    }


    public ReverseProxyRepoConfig getReverseProxyRepoConfig(String repoKey ){
        ReverseProxyRepoConfig reverseProxyRepo = null;
        for(ReverseProxyRepoConfig reverseProxyRepoConfig : reverseProxyRepoConfigs ){
            if (reverseProxyRepoConfig.getRepoRef().getKey().equals(repoKey)){
                reverseProxyRepo = reverseProxyRepoConfig;
                break;
            }
        }
        return reverseProxyRepo;
     }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ReverseProxyDescriptor that = (ReverseProxyDescriptor) o;
        return key.equals(that.key);
    }

    @Override
    public int hashCode() {
        return key.hashCode();
    }
}
