package org.artifactory.descriptor.config;

import org.apache.commons.collections15.OrderedMap;
import org.artifactory.descriptor.backup.BackupDescriptor;
import org.artifactory.descriptor.index.IndexerDescriptor;
import org.artifactory.descriptor.repo.LocalRepoDescriptor;
import org.artifactory.descriptor.repo.ProxyDescriptor;
import org.artifactory.descriptor.repo.RemoteRepoDescriptor;
import org.artifactory.descriptor.repo.RepoDescriptor;
import org.artifactory.descriptor.repo.VirtualRepoDescriptor;
import org.artifactory.descriptor.security.SecurityDescriptor;

import java.util.List;

/**
 * Mutable interface for the central config.
 *
 * @author Yossi Shaul
 */
public interface MutableCentralConfigDescriptor extends CentralConfigDescriptor {
    void setLocalRepositoriesMap(OrderedMap<String, LocalRepoDescriptor> localRepositoriesMap);

    void setRemoteRepositoriesMap(OrderedMap<String, RemoteRepoDescriptor> remoteRepositoriesMap);

    void setVirtualRepositoriesMap(
            OrderedMap<String, VirtualRepoDescriptor> virtualRepositoriesMap);

    void setProxies(List<ProxyDescriptor> proxies);

    void setDateFormat(String dateFormat);

    void setFileUploadMaxSizeMb(int fileUploadMaxSizeMb);

    void setBackups(List<BackupDescriptor> backups);

    void setIndexer(IndexerDescriptor descriptor);

    void setServerName(String serverName);

    void setSecurity(SecurityDescriptor security);

    void setOfflineMode(boolean offlineMode);

    /**
     * Removes the repository with the specified key from the repositories list. Will also remove
     * any references to this repositories from virtual repos, the backup and the indexer. The
     * repository might be of any type (local, remote or virtual).
     *
     * @param repoKey The key of the repository to remove.
     * @return The removed repository descripto or null if not found.
     */
    RepoDescriptor removeRepository(String repoKey);

    /**
     * @param repoKey The repository key to check.
     * @return True if a repository with the input key exists.
     */
    boolean isRepositoryExists(String repoKey);

    /**
     * Adds the local repository to local repos map.
     *
     * @param localRepoDescriptor The local repo to add.
     * @throws AlreadyExistsException If any repo with that key already exists.
     */
    void addLocalRepository(LocalRepoDescriptor localRepoDescriptor);

    /**
     * Adds the remote repository to remote repos map.
     *
     * @param remoteRepoDescriptor The remote repo to add.
     * @throws AlreadyExistsException If any repo with that key already exists.
     */
    void addRemoteRepository(RemoteRepoDescriptor remoteRepoDescriptor);

    /**
     * Adds the virtual repository to virtual repos map.
     *
     * @param virtualRepoDescriptor The virtual repo to add.
     * @throws AlreadyExistsException If any repo with that key already exists.
     */
    void addVirtualRepository(VirtualRepoDescriptor virtualRepoDescriptor);

    /**
     * This methods checks if the key is used by any descriptor.
     * This check is importans since all the descriptors keys are defined as XmlIds and must be
     * inique in the xml file.
     *
     * @param key The key to check.
     * @return True if the key is not used by any other descriptor.
     */
    boolean isKeyAvailable(String key);

    /**
     * @param proxyKey The proxy key to check.
     * @return True if a proxy with the input key exists.
     */
    boolean isProxyExists(String proxyKey);

    /**
     * Adds the proxy to the proxies list.
     *
     * @param proxyDescriptor The new proxy to add.
     * @throws AlreadyExistsException If any proxy with the same key already exists.
     */
    void addProxy(ProxyDescriptor proxyDescriptor);

    /**
     * Removes the proxy with the specified key from the proxies list. Will also
     * remove any references to this proxy from remote repos
     *
     * @param proxyKey The proxy key to check.
     * @return The removed proxy descriptor or null if not found.
     */
    ProxyDescriptor removeProxy(String proxyKey);

    /**
     * @param backupKey The backup key to check.
     * @return True if a backup with the input key exists.
     */
    boolean isBackupExists(String backupKey);

    /**
     * Adds the backup to the backups list.
     *
     * @param backupDescriptor The new backup to add.
     * @throws AlreadyExistsException If any backup with the same key already exists.
     */
    void addBackup(BackupDescriptor backupDescriptor);

    /**
     * Removes the backup with the specified key from the backups list. Will also
     * remove any references to this backup from remote repos
     *
     * @param backupKey The backup key to check.
     * @return The removed backup descriptor or null if not found.
     */
    BackupDescriptor removeBackup(String backupKey);
}
