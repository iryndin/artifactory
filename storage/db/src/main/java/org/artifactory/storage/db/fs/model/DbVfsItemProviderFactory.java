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

package org.artifactory.storage.db.fs.model;

import org.artifactory.repo.RepoPath;
import org.artifactory.storage.fs.VfsFileProvider;
import org.artifactory.storage.fs.VfsFolderProvider;
import org.artifactory.storage.fs.VfsItemProvider;
import org.artifactory.storage.fs.VfsItemProviderFactory;
import org.artifactory.storage.fs.lock.FsItemsVault;
import org.artifactory.storage.fs.repo.StoringRepo;
import org.springframework.stereotype.Component;

/**
 * Factory of of DB items provider.
 *
 * @author Yossi Shaul
 */
@Component
public class DbVfsItemProviderFactory implements VfsItemProviderFactory {
    @Override
    public VfsItemProvider createItemProvider(StoringRepo storingRepo, RepoPath repoPath, FsItemsVault fsItemsVault) {
        return new DbFsItemProvider(storingRepo, repoPath, fsItemsVault);
    }

    @Override
    public VfsFileProvider createFileProvider(StoringRepo storingRepo, RepoPath repoPath, FsItemsVault fsItemsVault) {
        return new DbFileProvider(storingRepo, repoPath, fsItemsVault);
    }

    @Override
    public VfsFolderProvider createFolderProvider(StoringRepo storingRepo, RepoPath repoPath,
            FsItemsVault fsItemsVault) {
        return new DbFolderProvider(storingRepo, repoPath, fsItemsVault);
    }
}
