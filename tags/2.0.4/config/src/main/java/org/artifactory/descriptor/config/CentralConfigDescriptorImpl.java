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
import org.artifactory.descriptor.backup.BackupDescriptor;
import org.artifactory.descriptor.index.IndexerDescriptor;
import org.artifactory.descriptor.repo.HttpRepoDescriptor;
import org.artifactory.descriptor.repo.LocalRepoDescriptor;
import org.artifactory.descriptor.repo.ProxyDescriptor;
import org.artifactory.descriptor.repo.RealRepoDescriptor;
import org.artifactory.descriptor.repo.RemoteRepoDescriptor;
import org.artifactory.descriptor.repo.RepoDescriptor;
import org.artifactory.descriptor.repo.VirtualRepoDescriptor;
import org.artifactory.descriptor.repo.jaxb.LocalRepositoriesMapAdapter;
import org.artifactory.descriptor.repo.jaxb.RemoteRepositoriesMapAdapter;
import org.artifactory.descriptor.repo.jaxb.VirtualRepositoriesMapAdapter;
import org.artifactory.descriptor.security.SecurityDescriptor;
import org.artifactory.util.AlreadyExistsException;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.util.ArrayList;
import java.util.List;

@XmlRootElement(name = "config")
@XmlType(name = "CentralConfigType",
        propOrder = {"serverName", "offlineMode", "fileUploadMaxSizeMb", "dateFormat", "security",
                "backups", "indexer", "localRepositoriesMap", "remoteRepositoriesMap",
                "virtualRepositoriesMap", "proxies"})
@XmlAccessorType(XmlAccessType.FIELD)
public class CentralConfigDescriptorImpl implements MutableCentralConfigDescriptor {

    public static final String DEFAULT_DATE_FORMAT = "dd-MM-yy HH:mm:ss z";

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

    @XmlTransient
    private ProxyDescriptor defaultProxy;

    /**
     * A name uniquely identifying this artifactory server instance
     */
    @XmlElement
    private String serverName;

    /**
     * if this flag is set all the remote repos will work in offline mode
     */
    @XmlElement(defaultValue = "false", required = false)
    private boolean offlineMode;

    /**
     * security might not be present in the xml but we always want to create it
     */
    @XmlElement
    private SecurityDescriptor security = new SecurityDescriptor();

    public OrderedMap<String, LocalRepoDescriptor> getLocalRepositoriesMap() {
        return localRepositoriesMap;
    }

    public void setLocalRepositoriesMap(OrderedMap<String, LocalRepoDescriptor> localRepositoriesMap) {
        this.localRepositoriesMap = localRepositoriesMap;
    }

    public OrderedMap<String, RemoteRepoDescriptor> getRemoteRepositoriesMap() {
        return remoteRepositoriesMap;
    }

    public void setRemoteRepositoriesMap(OrderedMap<String, RemoteRepoDescriptor> remoteRepositoriesMap) {
        this.remoteRepositoriesMap = remoteRepositoriesMap;
        updateDefaultProxy();
    }

    public OrderedMap<String, VirtualRepoDescriptor> getVirtualRepositoriesMap() {
        return virtualRepositoriesMap;
    }

    public void setVirtualRepositoriesMap(OrderedMap<String, VirtualRepoDescriptor> virtualRepositoriesMap) {
        this.virtualRepositoriesMap = virtualRepositoriesMap;
    }

    public List<ProxyDescriptor> getProxies() {
        return proxies;
    }

    public void setProxies(List<ProxyDescriptor> proxies) {
        this.proxies = proxies;
    }

    public ProxyDescriptor getDefaultProxy() {
        return defaultProxy;
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

    public SecurityDescriptor getSecurity() {
        return security;
    }

    public void setSecurity(SecurityDescriptor security) {
        this.security = security;
    }

    public RepoDescriptor removeRepository(String repoKey) {
        // first remove the repository itself
        RepoDescriptor removedRepo = localRepositoriesMap.remove(repoKey);
        if (removedRepo == null) {
            removedRepo = remoteRepositoriesMap.remove(repoKey);
            updateDefaultProxy();
        }
        if (removedRepo == null) {
            removedRepo = virtualRepositoriesMap.remove(repoKey);
        }
        if (removedRepo == null) {
            // not found - finish
            return null;
        }

        // remove from any virtual repository
        for (VirtualRepoDescriptor virtualRepoDescriptor : virtualRepositoriesMap.values()) {
            virtualRepoDescriptor.removeRepository(removedRepo);
        }

        if (removedRepo instanceof RealRepoDescriptor) {
            // remove the repository from any backup exclude list
            for (BackupDescriptor backup : getBackups()) {
                backup.removeExcludedRepository((RealRepoDescriptor) removedRepo);
            }

            // remove from the indexer exclude list
            IndexerDescriptor indexer = getIndexer();
            if (indexer != null) {
                indexer.removeExcludedRepository((RealRepoDescriptor) removedRepo);
            }
        }

        return removedRepo;
    }

    public boolean isKeyAvailable(String key) {
        return !(VirtualRepoDescriptor.GLOBAL_VIRTUAL_REPO_KEY.equals(key) ||
                isRepositoryExists(key) ||
                isProxyExists(key) ||
                isBackupExists(key) ||
                isLdapExists(key));
    }

    public boolean isRepositoryExists(String repoKey) {
        return localRepositoriesMap.containsKey(repoKey)
                || remoteRepositoriesMap.containsKey(repoKey)
                || virtualRepositoriesMap.containsKey(repoKey);
    }

    public void addLocalRepository(LocalRepoDescriptor localRepoDescriptor)
            throws AlreadyExistsException {
        String repoKey = localRepoDescriptor.getKey();
        failIfRepoKeyAlreadyExists(repoKey);
        localRepositoriesMap.put(repoKey, localRepoDescriptor);
    }

    public void addRemoteRepository(RemoteRepoDescriptor remoteRepoDescriptor) {
        String repoKey = remoteRepoDescriptor.getKey();
        failIfRepoKeyAlreadyExists(repoKey);
        remoteRepositoriesMap.put(repoKey, remoteRepoDescriptor);
        updateDefaultProxy();
    }

    public void addVirtualRepository(VirtualRepoDescriptor virtualRepoDescriptor) {
        String repoKey = virtualRepoDescriptor.getKey();
        failIfRepoKeyAlreadyExists(repoKey);
        virtualRepositoriesMap.put(repoKey, virtualRepoDescriptor);
    }

    public boolean isProxyExists(String proxyKey) {
        return getProxy(proxyKey) != null;
    }

    public void addProxy(ProxyDescriptor proxyDescriptor) {
        String proxyKey = proxyDescriptor.getKey();
        if (isProxyExists(proxyKey)) {
            throw new AlreadyExistsException("Proxy " + proxyKey + " already exists");
        }
        proxies.add(proxyDescriptor);
    }

    public ProxyDescriptor removeProxy(String proxyKey) {
        ProxyDescriptor proxyDescriptor = getProxy(proxyKey);
        if (proxyDescriptor == null) {
            return null;
        }

        // remove the proxy from the proxies list
        proxies.remove(proxyDescriptor);

        // remove references from all remote repositories
        for (RemoteRepoDescriptor remoteRepo : remoteRepositoriesMap.values()) {
            if (remoteRepo instanceof HttpRepoDescriptor) {
                ((HttpRepoDescriptor) remoteRepo).setProxy(null);
            }
        }
        updateDefaultProxy();

        return proxyDescriptor;
    }

    public boolean isBackupExists(String backupKey) {
        return getBackup(backupKey) != null;
    }

    public void addBackup(BackupDescriptor backupDescriptor) {
        String backupKey = backupDescriptor.getKey();
        if (isBackupExists(backupKey)) {
            throw new AlreadyExistsException("Backup " + backupKey + " already exists");
        }
        backups.add(backupDescriptor);
    }

    public BackupDescriptor removeBackup(String backupKey) {
        BackupDescriptor backupDescriptor = getBackup(backupKey);
        if (backupDescriptor == null) {
            return null;
        }

        // remove the backup from the backups list
        backups.remove(backupDescriptor);

        return backupDescriptor;
    }

    public void setOfflineMode(boolean offlineMode) {
        this.offlineMode = offlineMode;
    }

    public boolean isOfflineMode() {
        return offlineMode;
    }

    private ProxyDescriptor getProxy(String proxyKey) {
        for (ProxyDescriptor proxy : proxies) {
            if (proxy.getKey().equals(proxyKey)) {
                return proxy;
            }
        }
        return null;
    }

    private BackupDescriptor getBackup(String backupKey) {
        for (BackupDescriptor backup : backups) {
            if (backup.getKey().equals(backupKey)) {
                return backup;
            }
        }
        return null;
    }

    private boolean isLdapExists(String key) {
        return security != null && security.isLdapExists(key);
    }

    private void failIfRepoKeyAlreadyExists(String repoKey) {
        if (isRepositoryExists(repoKey)) {
            throw new AlreadyExistsException("Repository " + repoKey + " already exists");
        }
    }

    /**
     * Sets the default proxy to be the first proxy used by a remote repo
     */
    private void updateDefaultProxy() {
        for (RemoteRepoDescriptor remoteRepoDescriptor : remoteRepositoriesMap.values()) {
            if (remoteRepoDescriptor instanceof HttpRepoDescriptor) {
                ProxyDescriptor proxyDescriptor = ((HttpRepoDescriptor) remoteRepoDescriptor).getProxy();
                if (proxyDescriptor != null) {
                    defaultProxy = proxyDescriptor;
                    return;
                }
            }
        }
        defaultProxy = null;
    }
}