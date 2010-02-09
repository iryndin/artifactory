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
package org.artifactory.descriptor.config;

import org.apache.commons.collections15.OrderedMap;
import org.apache.commons.collections15.map.ListOrderedMap;
import org.artifactory.descriptor.Descriptor;
import org.artifactory.descriptor.backup.BackupDescriptor;
import org.artifactory.descriptor.index.IndexerDescriptor;
import org.artifactory.descriptor.repo.LocalRepoDescriptor;
import org.artifactory.descriptor.repo.ProxyDescriptor;
import org.artifactory.descriptor.repo.RemoteRepoDescriptor;
import org.artifactory.descriptor.repo.VirtualRepoDescriptor;
import org.artifactory.descriptor.repo.jaxb.LocalRepositoriesMapAdapter;
import org.artifactory.descriptor.repo.jaxb.RemoteRepositoriesMapAdapter;
import org.artifactory.descriptor.repo.jaxb.VirtualRepositoriesMapAdapter;
import org.artifactory.descriptor.security.Security;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

@XmlRootElement(name = "config")
@XmlType(name = "CentralConfigType",
        propOrder = {"serverName", "security", "fileUploadMaxSizeMb",
                "dateFormat", "backups", "indexer", "localRepositoriesMap", "remoteRepositoriesMap",
                "virtualRepositoriesMap", "proxies"})
@XmlAccessorType(XmlAccessType.FIELD)
public class CentralConfigDescriptor implements Descriptor {

    public static final String DEFAULT_DATE_FORMAT = "dd-MM-yy HH:mm:ssZ";

    public static final TimeZone UTC_TIME_ZONE = TimeZone.getTimeZone("UTC");

    @XmlElement(name = "localRepositories", required = true)
    @XmlJavaTypeAdapter(LocalRepositoriesMapAdapter.class)
    private OrderedMap<String, LocalRepoDescriptor> localRepositoriesMap =
            new ListOrderedMap<String, LocalRepoDescriptor>();

    @XmlElement(name = "remoteRepositories", required = false)
    @XmlJavaTypeAdapter(RemoteRepositoriesMapAdapter.class)
    private OrderedMap<String, RemoteRepoDescriptor> remoteRepositoriesMap =
            new ListOrderedMap<String, RemoteRepoDescriptor>();

    @XmlElement(name = "virtualRepositories", required = false)
    @XmlJavaTypeAdapter(VirtualRepositoriesMapAdapter.class)
    private OrderedMap<String, VirtualRepoDescriptor> virtualRepositoriesMap =
            new ListOrderedMap<String, VirtualRepoDescriptor>();

    @XmlElementWrapper(name = "proxies")
    @XmlElement(name = "proxy", required = false)
    private List<ProxyDescriptor> proxies = new ArrayList<ProxyDescriptor>();

    @XmlElement(defaultValue = DEFAULT_DATE_FORMAT)
    private String dateFormat = DEFAULT_DATE_FORMAT;

    @XmlElement(defaultValue = "100", required = false)
    private int fileUploadMaxSizeMb = 100;

    @XmlElementWrapper(name = "backups")
    @XmlElement(name = "backup", required = false)
    private List<BackupDescriptor> backups = new ArrayList<BackupDescriptor>();

    private IndexerDescriptor indexer;

    /**
     * A name uniquely identifying this artifactory server instance
     */
    @XmlElement
    private String serverName;

    @XmlElement
    private Security security;

    public OrderedMap<String, LocalRepoDescriptor> getLocalRepositoriesMap() {
        return localRepositoriesMap;
    }

    public void setLocalRepositoriesMap(
            OrderedMap<String, LocalRepoDescriptor> localRepositoriesMap) {
        this.localRepositoriesMap = localRepositoriesMap;
    }

    public OrderedMap<String, RemoteRepoDescriptor> getRemoteRepositoriesMap() {
        return remoteRepositoriesMap;
    }

    public void setRemoteRepositoriesMap(
            OrderedMap<String, RemoteRepoDescriptor> remoteRepositoriesMap) {
        this.remoteRepositoriesMap = remoteRepositoriesMap;
    }

    public OrderedMap<String, VirtualRepoDescriptor> getVirtualRepositoriesMap() {
        return virtualRepositoriesMap;
    }

    public void setVirtualRepositoriesMap(
            OrderedMap<String, VirtualRepoDescriptor> virtualRepositoriesMap) {
        this.virtualRepositoriesMap = virtualRepositoriesMap;
    }

    public List<ProxyDescriptor> getProxies() {
        return proxies;
    }

    public void setProxies(List<ProxyDescriptor> proxies) {
        this.proxies = proxies;
    }

    public String getDateFormat() {
        return dateFormat;
    }

    public void setDateFormat(String dateFormat) {
        this.dateFormat = dateFormat;
    }

    public int getFileUploadMaxSizeMb() {
        return fileUploadMaxSizeMb;
    }

    public void setFileUploadMaxSizeMb(int fileUploadMaxSizeMb) {
        this.fileUploadMaxSizeMb = fileUploadMaxSizeMb;
    }

    public List<BackupDescriptor> getBackups() {
        return backups;
    }

    public void setBackups(List<BackupDescriptor> backups) {
        this.backups = backups;
    }

    public IndexerDescriptor getIndexer() {
        return indexer;
    }

    public void setIndexer(IndexerDescriptor descriptor) {
        this.indexer = descriptor;
    }

    public String getServerName() {
        return serverName;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    public Security getSecurity() {
        return security;
    }

    public void setSecurity(Security security) {
        this.security = security;
    }
}