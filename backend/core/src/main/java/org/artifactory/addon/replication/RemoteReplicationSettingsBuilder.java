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

package org.artifactory.addon.replication;

import org.artifactory.addon.ReplicationAddon;
import org.artifactory.repo.RepoPath;

import java.io.Writer;

/**
 * @author Noam Y. Tenne
 */
public class RemoteReplicationSettingsBuilder {

    private final RepoPath repoPath;
    private boolean progress = false;
    private int socketTimeoutMillis = 15000;
    private int mark = 0;
    private boolean deleteExisting = false;
    private boolean includeProperties = false;
    private ReplicationAddon.Overwrite overwrite = ReplicationAddon.Overwrite.force;
    private final Writer responseWriter;

    public RemoteReplicationSettingsBuilder(RepoPath repoPath, Writer responseWriter) {
        this.repoPath = repoPath;
        this.responseWriter = responseWriter;
    }

    public RemoteReplicationSettingsBuilder progress(boolean progress) {
        this.progress = progress;
        return this;
    }

    public RemoteReplicationSettingsBuilder mark(int mark) {
        this.mark = mark;
        return this;
    }

    public RemoteReplicationSettingsBuilder deleteExisting(boolean deleteExisting) {
        this.deleteExisting = deleteExisting;
        return this;
    }

    public RemoteReplicationSettingsBuilder includeProperties(boolean includeProperties) {
        this.includeProperties = includeProperties;
        return this;
    }

    public RemoteReplicationSettingsBuilder overwrite(ReplicationAddon.Overwrite overwrite) {
        this.overwrite = overwrite;
        return this;
    }

    public RemoteReplicationSettingsBuilder timeout(int socketTimeoutMillis) {
        this.socketTimeoutMillis = socketTimeoutMillis;
        return this;
    }

    public RemoteReplicationSettings build() {
        if (repoPath == null) {
            throw new IllegalArgumentException("Repo path cannot be null.");
        }
        if (responseWriter == null) {
            throw new IllegalArgumentException("Response writer cannot be null.");
        }

        return new RemoteReplicationSettings(repoPath, progress, mark, deleteExisting, includeProperties, overwrite,
                responseWriter, socketTimeoutMillis);
    }
}
