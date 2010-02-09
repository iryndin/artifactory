package org.artifactory.descriptor.config;

import org.apache.commons.collections15.OrderedMap;
import org.artifactory.descriptor.Descriptor;
import org.artifactory.descriptor.backup.BackupDescriptor;
import org.artifactory.descriptor.index.IndexerDescriptor;
import org.artifactory.descriptor.repo.LocalRepoDescriptor;
import org.artifactory.descriptor.repo.ProxyDescriptor;
import org.artifactory.descriptor.repo.RemoteRepoDescriptor;
import org.artifactory.descriptor.repo.VirtualRepoDescriptor;
import org.artifactory.descriptor.security.SecurityDescriptor;

import java.util.List;
import java.util.TimeZone;

/**
 * Imutable interface for the central config,
 *
 * @author Yossi Shaul
 */
public interface CentralConfigDescriptor extends Descriptor {
    TimeZone UTC_TIME_ZONE = TimeZone.getTimeZone("UTC");

    OrderedMap<String, LocalRepoDescriptor> getLocalRepositoriesMap();

    OrderedMap<String, RemoteRepoDescriptor> getRemoteRepositoriesMap();

    OrderedMap<String, VirtualRepoDescriptor> getVirtualRepositoriesMap();

    List<ProxyDescriptor> getProxies();

    String getDateFormat();

    int getFileUploadMaxSizeMb();

    List<BackupDescriptor> getBackups();

    IndexerDescriptor getIndexer();

    String getServerName();

    SecurityDescriptor getSecurity();

    /**
     * @return true if the global offline mode is set.
     */
    boolean isOfflineMode();
}
