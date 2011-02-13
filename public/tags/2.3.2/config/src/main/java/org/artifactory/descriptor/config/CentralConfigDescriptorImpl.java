/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2011 JFrog Ltd.
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

package org.artifactory.descriptor.config;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.artifactory.descriptor.Descriptor;
import org.artifactory.descriptor.addon.AddonSettings;
import org.artifactory.descriptor.backup.BackupDescriptor;
import org.artifactory.descriptor.index.IndexerDescriptor;
import org.artifactory.descriptor.mail.MailServerDescriptor;
import org.artifactory.descriptor.property.PropertySet;
import org.artifactory.descriptor.repo.HttpRepoDescriptor;
import org.artifactory.descriptor.repo.LocalRepoDescriptor;
import org.artifactory.descriptor.repo.ProxyDescriptor;
import org.artifactory.descriptor.repo.RealRepoDescriptor;
import org.artifactory.descriptor.repo.RemoteRepoDescriptor;
import org.artifactory.descriptor.repo.RepoBaseDescriptor;
import org.artifactory.descriptor.repo.RepoDescriptor;
import org.artifactory.descriptor.repo.RepoLayout;
import org.artifactory.descriptor.repo.VirtualRepoDescriptor;
import org.artifactory.descriptor.repo.jaxb.LocalRepositoriesMapAdapter;
import org.artifactory.descriptor.repo.jaxb.RemoteRepositoriesMapAdapter;
import org.artifactory.descriptor.repo.jaxb.VirtualRepositoriesMapAdapter;
import org.artifactory.descriptor.security.SecurityDescriptor;
import org.artifactory.util.AlreadyExistsException;
import org.artifactory.util.DoesNotExistException;
import org.artifactory.util.RepoLayoutUtils;

import javax.annotation.Nullable;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@XmlRootElement(name = "config")
@XmlType(name = "CentralConfigType",
        propOrder = {"serverName", "offlineMode", "fileUploadMaxSizeMb", "dateFormat", "addons", "mailServer",
                "security", "backups", "indexer", "localRepositoriesMap", "remoteRepositoriesMap",
                "virtualRepositoriesMap", "proxies", "propertySets", "urlBase", "logo", "footer", "repoLayouts"},
        namespace = Descriptor.NS)
@XmlAccessorType(XmlAccessType.FIELD)
public class CentralConfigDescriptorImpl implements MutableCentralConfigDescriptor {

    public static final String DEFAULT_DATE_FORMAT = "dd-MM-yy HH:mm:ss z";

    @XmlElement(name = "localRepositories", required = true)
    @XmlJavaTypeAdapter(LocalRepositoriesMapAdapter.class)
    private Map<String, LocalRepoDescriptor> localRepositoriesMap = Maps.newLinkedHashMap();

    @XmlElement(name = "remoteRepositories", required = false)
    @XmlJavaTypeAdapter(RemoteRepositoriesMapAdapter.class)
    private Map<String, RemoteRepoDescriptor> remoteRepositoriesMap = Maps.newLinkedHashMap();

    @XmlElement(name = "virtualRepositories", required = false)
    @XmlJavaTypeAdapter(VirtualRepositoriesMapAdapter.class)
    private Map<String, VirtualRepoDescriptor> virtualRepositoriesMap = Maps.newLinkedHashMap();

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


    /**
     * if this flag is set all the remote repos will work in offline mode
     */
    @XmlElement(defaultValue = "false", required = false)
    private boolean offlineMode;

    private AddonSettings addons = new AddonSettings();

    private MailServerDescriptor mailServer;

    /**
     * security might not be present in the xml but we always want to create it
     */
    @XmlElement
    private SecurityDescriptor security = new SecurityDescriptor();

    @XmlElementWrapper(name = "propertySets")
    @XmlElement(name = "propertySet", required = false)
    private List<PropertySet> propertySets = new ArrayList<PropertySet>();

    @XmlElement
    private String urlBase;

    @XmlElement
    private String logo;

    @XmlElement
    private String footer;

    @XmlElementWrapper(name = "repoLayouts")
    @XmlElement(name = "repoLayout", required = false)
    private List<RepoLayout> repoLayouts = Lists.newArrayList();

    public Map<String, LocalRepoDescriptor> getLocalRepositoriesMap() {
        return localRepositoriesMap;
    }

    public void setLocalRepositoriesMap(Map<String, LocalRepoDescriptor> localRepositoriesMap) {
        this.localRepositoriesMap = localRepositoriesMap;
    }

    public Map<String, RemoteRepoDescriptor> getRemoteRepositoriesMap() {
        return remoteRepositoriesMap;
    }

    public void setRemoteRepositoriesMap(Map<String, RemoteRepoDescriptor> remoteRepositoriesMap) {
        this.remoteRepositoriesMap = remoteRepositoriesMap;
    }

    public Map<String, VirtualRepoDescriptor> getVirtualRepositoriesMap() {
        return virtualRepositoriesMap;
    }

    public void setVirtualRepositoriesMap(Map<String, VirtualRepoDescriptor> virtualRepositoriesMap) {
        this.virtualRepositoriesMap = virtualRepositoriesMap;
    }

    public List<ProxyDescriptor> getProxies() {
        return proxies;
    }

    public void setProxies(List<ProxyDescriptor> proxies) {
        this.proxies = proxies;
    }

    public ProxyDescriptor getDefaultProxy() {
        for (ProxyDescriptor proxy : proxies) {
            if (proxy.isDefaultProxy()) {
                return proxy;
            }
        }
        return null;
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

    public AddonSettings getAddons() {
        return addons;
    }

    public void setAddons(AddonSettings addons) {
        this.addons = addons;
    }

    public void addDefaultProxyToRemoteRepositories(ProxyDescriptor proxyDescriptor) {
        Map<String, RemoteRepoDescriptor> descriptorOrderedMap = getRemoteRepositoriesMap();
        for (RemoteRepoDescriptor descriptor : descriptorOrderedMap.values()) {
            if (descriptor instanceof HttpRepoDescriptor) {
                HttpRepoDescriptor httpRepoDescriptor = (HttpRepoDescriptor) descriptor;
                httpRepoDescriptor.setProxy(proxyDescriptor);
            }
        }
    }

    public MailServerDescriptor getMailServer() {
        return mailServer;
    }

    public void setSecurity(SecurityDescriptor security) {
        if (security == null) {
            security = new SecurityDescriptor();
        }
        this.security = security;
    }

    public void setMailServer(MailServerDescriptor mailServer) {
        this.mailServer = mailServer;
    }

    public List<PropertySet> getPropertySets() {
        return propertySets;
    }

    public void setPropertySets(List<PropertySet> propertySets) {
        this.propertySets = propertySets;
    }

    public String getUrlBase() {
        return urlBase;
    }

    public void setUrlBase(String urlBase) {
        this.urlBase = urlBase;
    }

    public RepoDescriptor removeRepository(String repoKey) {
        // first remove the repository itself
        RepoDescriptor removedRepo = localRepositoriesMap.remove(repoKey);
        if (removedRepo == null) {
            removedRepo = remoteRepositoriesMap.remove(repoKey);
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
        }

        if (removedRepo instanceof RepoBaseDescriptor) {
            // remove from the indexer exclude list
            IndexerDescriptor indexer = getIndexer();
            if (indexer != null) {
                indexer.removeExcludedRepository((RepoBaseDescriptor) removedRepo);
            }
        }

        return removedRepo;
    }


    public String getFooter() {
        return footer;
    }

    public void setFooter(String footer) {
        this.footer = footer;
    }

    public boolean isKeyAvailable(String key) {
        return !(VirtualRepoDescriptor.GLOBAL_VIRTUAL_REPO_KEY.equals(key) ||
                isRepositoryExists(key) ||
                isProxyExists(key) ||
                isBackupExists(key) ||
                isLdapExists(key) ||
                isPropertySetExists(key) ||
                isRepoLayoutExists(key));
    }

    public boolean isRepositoryExists(String repoKey) {
        return localRepositoriesMap.containsKey(repoKey)
                || remoteRepositoriesMap.containsKey(repoKey)
                || virtualRepositoriesMap.containsKey(repoKey);
    }

    public void addLocalRepository(LocalRepoDescriptor localRepoDescriptor)
            throws AlreadyExistsException {
        String repoKey = localRepoDescriptor.getKey();
        repoKeyExists(repoKey, false);
        localRepositoriesMap.put(repoKey, localRepoDescriptor);
    }

    public void addRemoteRepository(RemoteRepoDescriptor remoteRepoDescriptor) {
        String repoKey = remoteRepoDescriptor.getKey();
        repoKeyExists(repoKey, false);
        remoteRepositoriesMap.put(repoKey, remoteRepoDescriptor);
        ProxyDescriptor defaultProxyDescriptor = defaultProxyDefined();
        if (defaultProxyDescriptor != null) {
            if (remoteRepoDescriptor instanceof HttpRepoDescriptor) {
                ((HttpRepoDescriptor) remoteRepoDescriptor).setProxy(defaultProxyDescriptor);
            }
        }
    }

    public void addVirtualRepository(VirtualRepoDescriptor virtualRepoDescriptor) {
        String repoKey = virtualRepoDescriptor.getKey();
        repoKeyExists(repoKey, false);
        virtualRepositoriesMap.put(repoKey, virtualRepoDescriptor);
    }

    public boolean isProxyExists(String proxyKey) {
        return getProxy(proxyKey) != null;
    }

    public void addProxy(ProxyDescriptor proxyDescriptor, boolean defaultForAllRemoteRepo) {
        String proxyKey = proxyDescriptor.getKey();
        if (isProxyExists(proxyKey)) {
            throw new AlreadyExistsException("Proxy " + proxyKey + " already exists");
        }
        if (proxyDescriptor.isDefaultProxy()) {
            proxyChanged(proxyDescriptor, defaultForAllRemoteRepo);
            // remove default flag from other existing proxy if exist
            for (ProxyDescriptor proxy : proxies) {
                proxy.setDefaultProxy(false);
            }
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
        return proxyDescriptor;
    }

    public void proxyChanged(ProxyDescriptor proxy, boolean updateExistingRepos) {
        if (proxy.isDefaultProxy()) {
            if (updateExistingRepos) {
                updateExistingRepos(proxy);
            }
            //Unset the previous default if any
            for (ProxyDescriptor proxyDescriptor : proxies) {
                if (!proxy.equals(proxyDescriptor)) {
                    proxyDescriptor.setDefaultProxy(false);
                }
            }
        }
    }

    private void updateExistingRepos(ProxyDescriptor proxy) {
        ProxyDescriptor previousDefaultProxy = findPreviousProxyDescriptor(proxy);
        for (RemoteRepoDescriptor remoteRepoDescriptor : remoteRepositoriesMap.values()) {
            if (remoteRepoDescriptor instanceof HttpRepoDescriptor) {
                HttpRepoDescriptor httpRepoDescriptor = (HttpRepoDescriptor) remoteRepoDescriptor;
                ProxyDescriptor existingRepoProxy = httpRepoDescriptor.getProxy();
                // if the repo doesn't have a proxy, or it is the previous default proxy configured then override it.
                if (existingRepoProxy == null || existingRepoProxy.equals(previousDefaultProxy)) {
                    httpRepoDescriptor.setProxy(proxy);
                }
            }
        }
    }

    private ProxyDescriptor findPreviousProxyDescriptor(final ProxyDescriptor proxyDescriptor) {
        return Iterables.find(proxies, new Predicate<ProxyDescriptor>() {
            public boolean apply(@Nullable ProxyDescriptor input) {
                return (input != null) && input.isDefaultProxy() && !input.getKey().equals(proxyDescriptor.getKey());
            }
        }, null);
    }

    public boolean isBackupExists(String backupKey) {
        return getBackup(backupKey) != null;
    }


    public String getLogo() {
        return logo;
    }

    public void setLogo(String logo) {
        this.logo = logo;
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

    public boolean isPropertySetExists(String propertySetName) {
        return getPropertySet(propertySetName) != null;
    }

    public void addPropertySet(PropertySet propertySet) {
        String propertySetName = propertySet.getName();
        if (isPropertySetExists(propertySetName)) {
            throw new AlreadyExistsException("Property set " + propertySetName + " already exists");
        }
        propertySets.add(propertySet);
    }

    public PropertySet removePropertySet(String propertySetName) {
        PropertySet propertySet = getPropertySet(propertySetName);
        if (propertySet == null) {
            return null;
        }

        //Remove the property set from the property sets list
        propertySets.remove(propertySet);

        //Remove the property set from any local repo which is associated with it
        Collection<LocalRepoDescriptor> localRepoDescriptorCollection = localRepositoriesMap.values();
        for (LocalRepoDescriptor localRepoDescriptor : localRepoDescriptorCollection) {
            localRepoDescriptor.removePropertySet(propertySetName);
        }

        //Remove the property set from any remote repo which is associated with it
        Collection<RemoteRepoDescriptor> remoteRepoDescriptors = remoteRepositoriesMap.values();
        for (RemoteRepoDescriptor remoteRepoDescriptor : remoteRepoDescriptors) {
            remoteRepoDescriptor.removePropertySet(propertySetName);
        }

        return propertySet;
    }

    public void setOfflineMode(boolean offlineMode) {
        this.offlineMode = offlineMode;
    }

    public boolean isOfflineMode() {
        return offlineMode;
    }

    public ProxyDescriptor defaultProxyDefined() {
        for (ProxyDescriptor proxyDescriptor : proxies) {
            if (proxyDescriptor.isDefaultProxy()) {
                return proxyDescriptor;
            }
        }
        return null;
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

    private PropertySet getPropertySet(String propertySetName) {
        for (PropertySet propertySet : propertySets) {
            if (propertySet.getName().equals(propertySetName)) {
                return propertySet;
            }
        }

        return null;
    }

    private boolean isLdapExists(String key) {
        return security != null && security.isLdapExists(key);
    }

    private void repoKeyExists(String repoKey, boolean shouldExist) {
        boolean exists = isRepositoryExists(repoKey);
        if (exists && !shouldExist) {
            throw new AlreadyExistsException("Repository " + repoKey + " already exists");
        }

        if (!exists && shouldExist) {
            throw new DoesNotExistException("Repository " + repoKey + " does not exist");
        }
    }

    public List<RepoLayout> getRepoLayouts() {
        return repoLayouts;
    }

    public boolean isRepoLayoutExists(String repoLayoutName) {
        for (RepoLayout repoLayout : repoLayouts) {
            if (repoLayout.getName().equals(repoLayoutName)) {
                return true;
            }
        }

        return false;
    }

    public void addRepoLayout(RepoLayout repoLayout) {
        String repoLayoutName = repoLayout.getName();
        if (isRepoLayoutExists(repoLayoutName)) {
            throw new AlreadyExistsException("Repo Layout " + repoLayoutName + " already exists");
        }
        repoLayouts.add(repoLayout);
    }

    public RepoLayout removeRepoLayout(String repoLayoutName) {
        RepoLayout repoLayout = getRepoLayout(repoLayoutName);
        if (repoLayout == null) {
            return null;
        }

        repoLayouts.remove(repoLayout);


        Collection<LocalRepoDescriptor> localRepoDescriptorCollection = localRepositoriesMap.values();
        for (LocalRepoDescriptor localRepoDescriptor : localRepoDescriptorCollection) {
            if (repoLayout.equals(localRepoDescriptor.getRepoLayout())) {
                localRepoDescriptor.setRepoLayout(RepoLayoutUtils.MAVEN_2_DEFAULT);
            }
        }

        Collection<RemoteRepoDescriptor> remoteRepoDescriptors = remoteRepositoriesMap.values();
        for (RemoteRepoDescriptor remoteRepoDescriptor : remoteRepoDescriptors) {
            if (repoLayout.equals(remoteRepoDescriptor.getRepoLayout())) {
                remoteRepoDescriptor.setRepoLayout(RepoLayoutUtils.MAVEN_2_DEFAULT);
            }
            if (repoLayout.equals(remoteRepoDescriptor.getRemoteRepoLayout())) {
                remoteRepoDescriptor.setRemoteRepoLayout(null);
            }
        }

        Collection<VirtualRepoDescriptor> virtualRepoDescriptors = virtualRepositoriesMap.values();
        for (VirtualRepoDescriptor virtualRepoDescriptor : virtualRepoDescriptors) {
            if (repoLayout.equals(virtualRepoDescriptor.getRepoLayout())) {
                virtualRepoDescriptor.setRepoLayout(null);
            }
        }

        return repoLayout;
    }

    public void setRepoLayouts(List<RepoLayout> repoLayouts) {
        this.repoLayouts = repoLayouts;
    }

    public RepoLayout getRepoLayout(String repoLayoutName) {
        for (RepoLayout repoLayout : repoLayouts) {
            if (repoLayout.getName().equals(repoLayoutName)) {
                return repoLayout;
            }
        }

        return null;
    }
}