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

package org.artifactory.addon.replication;

import org.artifactory.descriptor.repo.ProxyDescriptor;
import org.artifactory.repo.RepoPath;

/**
 * @author Noam Y. Tenne
 */
public class LocalReplicationSettings extends ReplicationBaseSettings {

    private final String url;
    private final ProxyDescriptor proxyDescriptor;
    private final String username;
    private final String password;

    /**
     * <B>NOTE<B>: Try to refrain from using this constructor directly and use the builder instead
     */
    LocalReplicationSettings(RepoPath repoPath, String url, ProxyDescriptor proxyDescriptor,
            int socketTimeoutMillis, String username, String password, boolean deleteExisting,
            boolean includeProperties) {
        super(repoPath, deleteExisting, includeProperties, socketTimeoutMillis);
        this.url = url;
        this.proxyDescriptor = proxyDescriptor;
        this.username = username;
        this.password = password;
    }

    public String getUrl() {
        return url;
    }

    public ProxyDescriptor getProxyDescriptor() {
        return proxyDescriptor;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }
}
