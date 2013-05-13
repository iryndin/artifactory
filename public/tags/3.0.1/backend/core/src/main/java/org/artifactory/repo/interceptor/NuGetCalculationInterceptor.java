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

import org.apache.commons.lang.StringUtils;
import org.artifactory.addon.AddonsManager;
import org.artifactory.addon.NuGetAddon;
import org.artifactory.addon.PropertiesAddon;
import org.artifactory.api.common.MultiStatusHolder;
import org.artifactory.api.repo.RepositoryService;
import org.artifactory.common.MutableStatusHolder;
import org.artifactory.descriptor.repo.RepoDescriptor;
import org.artifactory.md.Properties;
import org.artifactory.repo.RepoPath;
import org.artifactory.repo.interceptor.storage.StorageInterceptorAdapter;
import org.artifactory.sapi.fs.VfsFile;
import org.artifactory.sapi.fs.VfsItem;
import org.artifactory.sapi.interceptor.ImportInterceptor;

import javax.inject.Inject;

import static org.artifactory.addon.nuget.NuGetProperties.Id;
import static org.artifactory.addon.nuget.NuGetProperties.IsLatestVersion;

/**
 * Triggers NuGet package related storage events
 *
 * @author Noam Y. Tenne
 */
public class NuGetCalculationInterceptor extends StorageInterceptorAdapter implements ImportInterceptor {

    @Inject
    AddonsManager addonsManager;

    @Inject
    RepositoryService repositoryService;

    @Override
    public void beforeDelete(VfsItem fsItem, MutableStatusHolder statusHolder) {
        /**
         * Although the latest recalculation is called on before delete, the final method is annotated with delay until
         * after commit
         */
        updateRepositoryLatestNuPkgVersionIfNeeded(fsItem);
    }

    @Override
    public void afterCreate(VfsItem fsItem, MutableStatusHolder statusHolder) {
        extractNuPkgInfoIfNeeded(fsItem, new MultiStatusHolder());
    }

    @Override
    public void afterMove(VfsItem sourceItem, VfsItem targetItem, MutableStatusHolder statusHolder,
            Properties properties) {
        handleOnAfterMoveOrCopy(sourceItem, targetItem, statusHolder);
    }

    @Override
    public void afterCopy(VfsItem sourceItem, VfsItem targetItem, MutableStatusHolder statusHolder,
            Properties properties) {
        handleOnAfterMoveOrCopy(null, targetItem, statusHolder);
    }

    @Override
    public void afterImport(VfsItem fsItem, MutableStatusHolder statusHolder) {
        extractNuPkgInfoIfNeeded(fsItem, statusHolder);
    }

    /**
     * Extracts the info of the given item if it's a nupkg and is stored within a nupkg enabled repo
     */
    private void extractNuPkgInfoIfNeeded(VfsItem createdItem, MutableStatusHolder statusHolder) {
        if (shouldTakeAction(createdItem)) {
            addonsManager.addonByType(NuGetAddon.class).extractNuPkgInfo(((VfsFile) createdItem), statusHolder);
        }
    }

    /**
     * If it's a nupkg recalculate the IDs latest version state for the repository
     */
    private void handleOnAfterMoveOrCopy(VfsItem sourceItem, VfsItem targetItem, MutableStatusHolder statusHolder) {
        if ((sourceItem != null) && shouldTakeAction(sourceItem)) {
            updateRepositoryLatestNuPkgVersionIfNeeded(sourceItem);
        }

        if (shouldTakeAction(targetItem)) {
            Properties nuPkgProperties = addonsManager.addonByType(PropertiesAddon.class).getProperties(
                    targetItem.getRepoPath());
            String nuPkgId = Id.extract(nuPkgProperties);
            if (StringUtils.isBlank(nuPkgId)) {
                /**
                 * Package might have come from a repo with no NuGet support, so extract the info and the request
                 * calculation
                 */
                addonsManager.addonByType(NuGetAddon.class).extractNuPkgInfo((VfsFile) targetItem, statusHolder);
            } else {
                addonsManager.addonByType(NuGetAddon.class).requestAsyncLatestNuPkgVersionUpdate(
                        targetItem.getRepoKey(), nuPkgId);
            }
        }
    }

    /**
     * If it's a nupkg annotated as latest version, recalculate the latest version state for this ID on the repository
     */
    private void updateRepositoryLatestNuPkgVersionIfNeeded(VfsItem affectedItem) {
        if (shouldTakeAction(affectedItem)) {
            Properties nuPkgProperties = addonsManager.addonByType(PropertiesAddon.class).getProperties(
                    affectedItem.getRepoPath());
            String isLatestVersion = IsLatestVersion.extract(nuPkgProperties);
            if (Boolean.TRUE.toString().equals(isLatestVersion)) {
                String nuPkgId = Id.extract(nuPkgProperties);
                if (StringUtils.isNotBlank(nuPkgId)) {
                    addonsManager.addonByType(NuGetAddon.class).requestAsyncLatestNuPkgVersionUpdate(
                            affectedItem.getRepoKey(), nuPkgId);
                }
            }
        }
    }

    private boolean shouldTakeAction(VfsItem item) {
        if (item.isFile()) {
            RepoPath repoPath = item.getRepoPath();
            if (repoPath.getPath().endsWith(".nupkg")) {
                String repoKey = repoPath.getRepoKey();
                RepoDescriptor repoDescriptor = repositoryService.repoDescriptorByKey(repoKey);
                return ((repoDescriptor != null) && repoDescriptor.isEnableNuGetSupport());
            }
        }

        return false;
    }
}
