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

package org.artifactory.jcr.md;

import org.artifactory.api.fs.FolderInfoImpl;
import org.artifactory.api.fs.InternalFolderInfo;
import org.artifactory.md.MetadataInfo;
import org.artifactory.repo.RepoPath;

import javax.jcr.Node;

/**
 * @author freds
 */
public class FolderInfoPersistenceHandler extends AbstractPersistenceHandler<InternalFolderInfo> {

    public FolderInfoPersistenceHandler(XmlMetadataProvider<InternalFolderInfo> xmlProvider) {
        super(xmlProvider);
    }

    public boolean hasMetadata(MetadataAware metadataAware) {
        return metadataAware.isDirectory();
    }

    public InternalFolderInfo read(MetadataAware metadataAware) {
        RepoPath repoPath = metadataAware.getRepoPath();
        Node node = metadataAware.getNode();
        FolderInfoImpl folderInfo = new FolderInfoImpl(repoPath);
        fillItemInfoFromNode(node, folderInfo);
        return folderInfo;
    }

    public void update(MetadataAware metadataAware, InternalFolderInfo folderInfo) {
        Node node = metadataAware.getNode();
        setPropertiesInNodeFromInfo(node, folderInfo);
    }

    public void remove(MetadataAware metadataAware) {
        throw new IllegalArgumentException("Removing basic folder metadata is forbidden!\n" +
                "Done on folder " + metadataAware);
    }

    public InternalFolderInfo copy(InternalFolderInfo original) {
        return new FolderInfoImpl(original);
    }

    public MetadataInfo getMetadataInfo(MetadataAware metadataAware) {
        // TODO: Can actual build one from FolderInfo
        return null;
    }
}