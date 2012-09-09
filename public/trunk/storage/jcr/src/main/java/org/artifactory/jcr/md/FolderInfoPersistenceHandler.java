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

package org.artifactory.jcr.md;

import org.artifactory.factory.InfoFactoryHolder;
import org.artifactory.fs.FolderInfo;
import org.artifactory.fs.MutableFolderInfo;
import org.artifactory.md.MetadataInfo;
import org.artifactory.repo.RepoPath;

import javax.jcr.Node;

/**
 * @author freds
 */
public class FolderInfoPersistenceHandler extends AbstractPersistenceHandler<FolderInfo, MutableFolderInfo> {

    public FolderInfoPersistenceHandler(XmlMetadataProvider<FolderInfo, MutableFolderInfo> xmlProvider) {
        super(xmlProvider);
    }

    @Override
    public boolean hasMetadata(MetadataAware metadataAware) {
        return metadataAware.isDirectory();
    }

    @Override
    public FolderInfo read(MetadataAware metadataAware) {
        RepoPath repoPath = metadataAware.getRepoPath();
        Node node = metadataAware.getNode();
        MutableFolderInfo folderInfo = InfoFactoryHolder.get().createFolderInfo(repoPath);
        fillItemInfoFromNode(node, folderInfo);
        return folderInfo;
    }

    @Override
    public void update(MetadataAware metadataAware, MutableFolderInfo folderInfo) {
        Node node = metadataAware.getNode();
        setPropertiesInNodeFromInfo(node, folderInfo);
    }

    @Override
    public void remove(MetadataAware metadataAware) {
        throw new IllegalArgumentException("Removing basic folder metadata is forbidden!\n" +
                "Done on folder " + metadataAware);
    }

    @Override
    public MutableFolderInfo copy(FolderInfo original) {
        return InfoFactoryHolder.get().copyFolderInfo(original);
    }

    @Override
    public MetadataInfo getMetadataInfo(MetadataAware metadataAware) {
        // TODO: Can actual build one from FolderInfo
        return null;
    }
}