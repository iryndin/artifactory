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

package org.artifactory.repo.interceptor;

import org.artifactory.addon.AddonsManager;
import org.artifactory.addon.replication.ReplicationAddon;
import org.artifactory.common.MutableStatusHolder;
import org.artifactory.md.Properties;
import org.artifactory.repo.interceptor.storage.StorageInterceptorAdapter;
import org.artifactory.sapi.fs.VfsItem;

import javax.inject.Inject;

/**
 * @author Noam Y. Tenne
 */
public class ReplicationEventInterceptor extends StorageInterceptorAdapter {

    @Inject
    private AddonsManager addonsManager;

    @Override
    public void afterCreate(VfsItem fsItem, MutableStatusHolder statusHolder) {
        queueCreateEvent(fsItem);
    }

    @Override
    public void afterDelete(VfsItem fsItem, MutableStatusHolder statusHolder) {
        queueDeleteEvent(fsItem);
    }

    @Override
    public void afterMove(VfsItem sourceItem, VfsItem targetItem, MutableStatusHolder statusHolder,
            Properties properties) {
        queueDeleteEvent(sourceItem);
        queueCreateEvent(targetItem);
    }

    @Override
    public void afterCopy(VfsItem sourceItem, VfsItem targetItem, MutableStatusHolder statusHolder,
            Properties properties) {
        queueCreateEvent(targetItem);
    }

    @Override
    public void afterPropertyCreate(VfsItem fsItem, MutableStatusHolder statusHolder, String name, String... values) {
        getReplicationAddon().offerLocalReplicationPropertiesChangeEvent(fsItem.getRepoPath());
    }

    @Override
    public void afterPropertyDelete(VfsItem fsItem, MutableStatusHolder statusHolder, String name) {
        getReplicationAddon().offerLocalReplicationPropertiesChangeEvent(fsItem.getRepoPath());
    }

    private void queueCreateEvent(VfsItem fsItem) {
        if (fsItem.isFile()) {
            getReplicationAddon().offerLocalReplicationDeploymentEvent(fsItem.getRepoPath());
        } else {
            getReplicationAddon().offerLocalReplicationMkDirEvent(fsItem.getRepoPath());
        }
        if (!fsItem.getProperties().isEmpty()) {
            getReplicationAddon().offerLocalReplicationPropertiesChangeEvent(fsItem.getRepoPath());
        }
    }

    private void queueDeleteEvent(VfsItem fsItem) {
        getReplicationAddon().offerLocalReplicationDeleteEvent(fsItem.getRepoPath());
    }

    private ReplicationAddon getReplicationAddon() {
        return addonsManager.addonByType(ReplicationAddon.class);
    }
}
