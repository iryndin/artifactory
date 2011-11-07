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

import org.apache.commons.lang.StringUtils;
import org.artifactory.checksum.ChecksumInfo;
import org.artifactory.checksum.ChecksumStorageHelper;
import org.artifactory.checksum.ChecksumType;
import org.artifactory.factory.InfoFactoryHolder;
import org.artifactory.fs.FileInfo;
import org.artifactory.fs.MutableFileInfo;
import org.artifactory.md.MetadataInfo;
import org.artifactory.repo.RepoPath;

import javax.jcr.Node;

import static org.artifactory.jcr.utils.JcrHelper.*;

/**
 * @author freds
 */
public class FileInfoPersistenceHandler extends AbstractPersistenceHandler<FileInfo, MutableFileInfo> {

    public FileInfoPersistenceHandler(XmlMetadataProvider<FileInfo, MutableFileInfo> xmlProvider) {
        super(xmlProvider);
    }

    public boolean hasMetadata(MetadataAware metadataAware) {
        return metadataAware.isFile();
    }

    public FileInfo read(MetadataAware metadataAware) {
        RepoPath repoPath = metadataAware.getRepoPath();
        Node node = metadataAware.getNode();
        MutableFileInfo fileInfo = InfoFactoryHolder.get().createFileInfo(repoPath);
        fillItemInfoFromNode(node, fileInfo);

        Node resNode = getResourceNode(node);
        fileInfo.setSize(getLength(resNode));
        fileInfo.setMimeType(getMimeType(resNode));

        for (ChecksumType type : ChecksumType.values()) {
            String original = getStringProperty(node, ChecksumStorageHelper.getOriginalPropName(type), null, true);
            String actual = getStringProperty(node, ChecksumStorageHelper.getActualPropName(type), null, true);
            if (StringUtils.isNotBlank(actual) || StringUtils.isNotBlank(original)) {
                fileInfo.addChecksumInfo(new ChecksumInfo(type, original, actual));
            }
        }
        return fileInfo;
    }

    public void update(MetadataAware metadataAware, MutableFileInfo fileInfo) {
        Node node = metadataAware.getNode();
        setPropertiesInNodeFromInfo(node, fileInfo);

        setMimeType(node, fileInfo.getMimeType());

        for (ChecksumType type : ChecksumType.values()) {
            ChecksumInfo checksumInfo = fileInfo.getChecksumsInfo().getChecksumInfo(type);
            String actual = checksumInfo != null ? checksumInfo.getActual() : null;
            String original = getRealOriginal(checksumInfo);
            setStringProperty(node, ChecksumStorageHelper.getOriginalPropName(type), original);
            setStringProperty(node, ChecksumStorageHelper.getActualPropName(type), actual);
        }
    }

    private String getRealOriginal(ChecksumInfo checksumInfo) {
        if (checksumInfo != null) {
            if (checksumInfo.isMarkedAsTrusted()) {
                return ChecksumInfo.TRUSTED_FILE_MARKER;
            } else {
                return checksumInfo.getOriginal();
            }
        }
        return null;
    }

    public void remove(MetadataAware metadataAware) {
        throw new IllegalArgumentException("Removing basic file metadata is forbidden!\n" +
                "Done on file " + metadataAware);
    }

    public MutableFileInfo copy(FileInfo original) {
        return InfoFactoryHolder.get().copyFileInfo(original);
    }

    public MetadataInfo getMetadataInfo(MetadataAware metadataAware) {
        // TODO: Can actual build one from FileInfo
        return null;
    }
}
