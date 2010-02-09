/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2010 JFrog Ltd.
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

import org.artifactory.api.fs.FolderInfo;
import org.artifactory.api.fs.FolderInfoImpl;
import org.artifactory.api.fs.MetadataInfo;
import org.artifactory.api.repo.RepoPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;

/**
 * @author freds
 */
public class FolderInfoPersistenceHandler extends AbstractPersistenceHandler<FolderInfo> {
    @SuppressWarnings({"UnusedDeclaration"})
    private static final Logger log = LoggerFactory.getLogger(FolderInfoPersistenceHandler.class);


    public FolderInfoPersistenceHandler(XmlMetadataProvider<FolderInfo> xmlProvider) {
        super(xmlProvider);
    }

    public boolean hasMetadata(MetadataAware metadataAware) {
        return metadataAware.isDirectory();
    }

    public FolderInfo read(MetadataAware metadataAware) {
        RepoPath repoPath = metadataAware.getRepoPath();
        Node node = metadataAware.getNode();
        FolderInfoImpl folderInfo = new FolderInfoImpl(repoPath);
        fillItemInfoFromNode(node, folderInfo);
        return folderInfo;
    }

    public void update(MetadataAware metadataAware, FolderInfo folderInfo) {
        Node node = metadataAware.getNode();
        setPropertiesInNodeFromInfo(node, folderInfo);
    }

    public void remove(MetadataAware metadataAware) {
        throw new IllegalArgumentException("Removing basic folder metadata is forbidden!\n" +
                "Done on folder " + metadataAware);
    }

    public FolderInfo copy(FolderInfo original) {
        return new FolderInfoImpl(original);
    }

    public MetadataInfo getMetadataInfo(MetadataAware metadataAware) {
        // TODO: Can actual build one from FolderInfo
        return null;
    }
}