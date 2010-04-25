/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2010 JFrog Ltd.
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

import org.apache.commons.collections15.OrderedMap;
import org.artifactory.descriptor.Descriptor;
import org.artifactory.descriptor.addon.AddonSettings;
import org.artifactory.descriptor.backup.BackupDescriptor;
import org.artifactory.descriptor.index.IndexerDescriptor;
import org.artifactory.descriptor.mail.MailServerDescriptor;
import org.artifactory.descriptor.property.PropertySet;
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

    ProxyDescriptor getDefaultProxy();

    MailServerDescriptor getMailServer();

    List<PropertySet> getPropertySets();

    String getUrlBase();

    AddonSettings getAddons();

    String getLogo();

    String getFooter();
}
