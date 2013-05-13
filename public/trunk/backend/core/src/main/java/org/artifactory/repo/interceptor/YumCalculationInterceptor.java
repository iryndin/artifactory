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

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import org.apache.commons.lang.StringUtils;
import org.artifactory.addon.AddonsManager;
import org.artifactory.addon.YumAddon;
import org.artifactory.common.MutableStatusHolder;
import org.artifactory.descriptor.repo.LocalRepoDescriptor;
import org.artifactory.md.Properties;
import org.artifactory.model.common.RepoPathImpl;
import org.artifactory.repo.RepoPath;
import org.artifactory.repo.interceptor.storage.StorageInterceptorAdapter;
import org.artifactory.repo.service.InternalRepositoryService;
import org.artifactory.sapi.fs.VfsItem;
import org.artifactory.util.PathUtils;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.List;

/**
 * @author Noam Y. Tenne
 */
public class YumCalculationInterceptor extends StorageInterceptorAdapter {

    @Inject
    AddonsManager addonsManager;

    @Inject
    InternalRepositoryService repositoryService;

    @Override
    public void afterCreate(VfsItem fsItem, MutableStatusHolder statusHolder) {
        invokeCalculationIfNeed(fsItem);
    }

    @Override
    public void afterDelete(VfsItem fsItem, MutableStatusHolder statusHolder) {
        invokeCalculationIfNeed(fsItem);
    }

    @Override
    public void afterMove(VfsItem sourceItem, VfsItem targetItem, MutableStatusHolder statusHolder,
            Properties properties) {
        invokeCalculationIfNeed(sourceItem);
        invokeCalculationIfNeed(targetItem);
    }

    @Override
    public void afterCopy(VfsItem sourceItem, VfsItem targetItem, MutableStatusHolder statusHolder,
            Properties properties) {
        invokeCalculationIfNeed(targetItem);
    }

    private void invokeCalculationIfNeed(VfsItem fsItem) {
        RepoPath repoPath = fsItem.getRepoPath();
        String repoKey = repoPath.getRepoKey();
        LocalRepoDescriptor hostingLocalRepo = repositoryService.localRepoDescriptorByKey(repoKey);
        if ((hostingLocalRepo != null) && hostingLocalRepo.isCalculateYumMetadata()) {
            if (repoPath.getPath().endsWith(".rpm") || isYumGroupFile(repoPath.getPath(), hostingLocalRepo)) {
                String[] itemPathFolders = StringUtils.split(repoPath.getParent().getPath(), '/');
                int yumCalculationDepth = hostingLocalRepo.getYumRootDepth();
                if (itemPathFolders.length >= yumCalculationDepth) {
                    StringBuilder folderToCalculate = new StringBuilder();
                    for (int i = 0; i < yumCalculationDepth; i++) {
                        folderToCalculate.append(itemPathFolders[i]);
                        if (i != (yumCalculationDepth - 1)) {
                            folderToCalculate.append("/");
                        }
                    }
                    getYumAddon().requestAsyncRepositoryYumMetadataCalculation(
                            new RepoPathImpl(repoKey, folderToCalculate.toString()));
                }
            }
        }
    }

    private boolean isYumGroupFile(final String path, LocalRepoDescriptor hostingLocalRepo) {
        List<String> yumGroupFileNames = PathUtils.delimitedListToStringList(
                hostingLocalRepo.getYumGroupFileNames(), ",");

        return StringUtils.isNotBlank(Iterables.find(yumGroupFileNames, new Predicate<String>() {
            @Override
            public boolean apply(@Nullable String input) {
                return StringUtils.endsWith(path, YumAddon.REPO_DATA_DIR + input);
            }
        }, null));
    }

    private YumAddon getYumAddon() {
        return addonsManager.addonByType(YumAddon.class);
    }
}
