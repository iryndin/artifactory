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

package org.artifactory.jcr.factory;

import org.artifactory.fs.FileInfo;
import org.artifactory.fs.FolderInfo;
import org.artifactory.jcr.fs.FileInfoProxy;
import org.artifactory.jcr.fs.FolderInfoProxy;
import org.artifactory.jcr.fs.JcrFile;
import org.artifactory.jcr.fs.JcrFolder;
import org.artifactory.jcr.fs.JcrFsItem;
import org.artifactory.jcr.md.MetadataDefinition;
import org.artifactory.repo.RepoPath;
import org.artifactory.sapi.fs.VfsFile;
import org.artifactory.sapi.fs.VfsFolder;
import org.artifactory.sapi.fs.VfsItem;

import java.util.Set;

/**
 * Date: 8/4/11
 * Time: 2:37 PM
 *
 * @author Fred Simon
 */
public class VfsItemFactory {
    public static VfsFile createVfsFile(RepoPath repoPath, JcrFsItemFactory repo) {
        return new JcrFile(repoPath, repo);
    }

    public static VfsFolder createVfsFolder(RepoPath repoPath, JcrFsItemFactory repo) {
        return new JcrFolder(repoPath, repo);
    }

    public static JcrFsItemFactory getStoringRepo(VfsItem vfsItem) {
        return ((JcrFsItem) vfsItem).getRepo();
    }

    public static Set<MetadataDefinition<?, ?>> getExistingMetadata(VfsItem vfsItem, boolean includeInternal) {
        return ((JcrFsItem) vfsItem).getExistingMetadata(includeInternal);
    }

    public static MetadataDefinition getMetadataDefinition(VfsItem vfsItem, String metadataName) {
        return ((JcrFsItem) vfsItem).getMetadataDefinition(metadataName);
    }

    public static FileInfo createFileInfoProxy(RepoPath repoPath) {
        return new FileInfoProxy(repoPath);
    }

    public static FolderInfo createFolderInfoProxy(RepoPath repoPath) {
        return new FolderInfoProxy(repoPath);
    }
}
