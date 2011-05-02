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

package org.artifactory.webapp.wicket.page.config.repos;

import org.apache.wicket.injection.web.InjectorHolder;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.artifactory.api.config.CentralConfigService;
import org.artifactory.descriptor.config.CentralConfigDescriptor;
import org.artifactory.descriptor.config.MutableCentralConfigDescriptor;
import org.artifactory.descriptor.replication.ReplicationDescriptor;
import org.artifactory.descriptor.repo.LocalRepoDescriptor;
import org.artifactory.descriptor.repo.RemoteRepoDescriptor;
import org.artifactory.descriptor.repo.RepoDescriptor;
import org.artifactory.descriptor.repo.VirtualRepoDescriptor;

import java.io.Serializable;

/**
 * Helper class to RepositoryConfigPage, help syncing between mutableDescriptor model and mutableDescriptor cache
 *
 * @author Eli Givoni
 */
public class CachingDescriptorHelper implements Serializable {
    /**
     * All the saved changes are applied to this config object and this is sent to the central config to be persisted.
     */
    //private MutableCentralConfigDescriptor cachingMutableDescriptor;
    /**
     * Used as the model for the repositories config page.
     */
    private MutableCentralConfigDescriptor modelMutableDescriptor;

    @SpringBean
    private CentralConfigService centralConfigService;

    {
        InjectorHolder.getInjector().inject(this);
    }

    CachingDescriptorHelper(MutableCentralConfigDescriptor mutableDescriptor) {
        this.modelMutableDescriptor = mutableDescriptor;
    }

    /**
     * @return MutableCentralConfigDescriptor from the db
     */
    public MutableCentralConfigDescriptor getSavedMutableDescriptor() {
        return getDescriptor();
    }

    /**
     * @return RepositoryConfigPage mutable descriptor model
     */
    public MutableCentralConfigDescriptor getModelMutableDescriptor() {
        return modelMutableDescriptor;
    }

    public void syncAndSaveLocalRepositories() {
        MutableCentralConfigDescriptor configDescriptor = getDescriptor();
        configDescriptor.setLocalRepositoriesMap(modelMutableDescriptor.getLocalRepositoriesMap());
        saveDescriptor(configDescriptor);
    }

    public void syncAndSaveRemoteRepositories() {
        MutableCentralConfigDescriptor configDescriptor = getDescriptor();
        configDescriptor.setRemoteRepositoriesMap(modelMutableDescriptor.getRemoteRepositoriesMap());
        configDescriptor.setReplications(modelMutableDescriptor.getReplications());
        saveDescriptor(configDescriptor);
    }

    public void syncAndSaveVirtualRepositories() {
        MutableCentralConfigDescriptor configDescriptor = getDescriptor();
        configDescriptor.setVirtualRepositoriesMap(modelMutableDescriptor.getVirtualRepositoriesMap());
        saveDescriptor(configDescriptor);
    }

    protected void removeRepositoryAndSave(String repoKey) {
        modelMutableDescriptor.removeRepository(repoKey);
        MutableCentralConfigDescriptor savedDescriptor = getDescriptor();
        savedDescriptor.removeRepository(repoKey);
        saveDescriptor(savedDescriptor);
    }

    private void saveDescriptor(MutableCentralConfigDescriptor descriptor) {
        centralConfigService.saveEditedDescriptorAndReload(descriptor);
    }

    private MutableCentralConfigDescriptor getDescriptor() {
        return centralConfigService.getMutableDescriptor();
    }

    /**
     * reload this repo descriptor in case user canceled (will work also for save)
     */
    public void reloadRepository(String repoKey) {
        CentralConfigDescriptor cc = centralConfigService.getMutableDescriptor();

        RepoDescriptor repoToReload = cc.getLocalRepositoriesMap().get(repoKey);
        if (repoToReload != null) {
            modelMutableDescriptor.getLocalRepositoriesMap().put(repoKey, (LocalRepoDescriptor) repoToReload);
            return;
        }
        repoToReload = cc.getRemoteRepositoriesMap().get(repoKey);
        if (repoToReload != null) {
            modelMutableDescriptor.getRemoteRepositoriesMap().put(repoKey, (RemoteRepoDescriptor) repoToReload);
            ReplicationDescriptor existingReplication = cc.getReplication(repoKey);
            if (existingReplication != null) {
                modelMutableDescriptor.updateReplication(existingReplication);
            }
            return;
        }
        repoToReload = cc.getVirtualRepositoriesMap().get(repoKey);
        if (repoToReload != null) {
            modelMutableDescriptor.getVirtualRepositoriesMap().put(repoKey, (VirtualRepoDescriptor) repoToReload);
        }
    }
}
