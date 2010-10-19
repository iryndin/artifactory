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

import org.apache.commons.lang.StringUtils;
import org.artifactory.api.fs.FileInfoImpl;
import org.artifactory.api.fs.InternalFileInfo;
import org.artifactory.checksum.ChecksumInfo;
import org.artifactory.checksum.ChecksumType;
import org.artifactory.md.MetadataInfo;
import org.artifactory.repo.RepoPath;

import javax.jcr.Node;

import static org.artifactory.repo.jcr.JcrHelper.*;

/**
 * @author freds
 */
public class FileInfoPersistenceHandler extends AbstractPersistenceHandler<InternalFileInfo> {

    public FileInfoPersistenceHandler(XmlMetadataProvider<InternalFileInfo> xmlProvider) {
        super(xmlProvider);
    }

    public boolean hasMetadata(MetadataAware metadataAware) {
        return metadataAware.isFile();
    }

    public InternalFileInfo read(MetadataAware metadataAware) {
        RepoPath repoPath = metadataAware.getRepoPath();
        Node node = metadataAware.getNode();
        FileInfoImpl fileInfo = new FileInfoImpl(repoPath);
        fillItemInfoFromNode(node, fileInfo);

        Node resNode = getResourceNode(node);
        fileInfo.setSize(getLength(resNode));
        fileInfo.setMimeType(getMimeType(resNode));

        for (ChecksumType type : ChecksumType.values()) {
            String original = getStringProperty(node, type.getOriginalPropName(), null, true);
            String actual = getStringProperty(node, type.getActualPropName(), null, true);
            if (StringUtils.isNotBlank(actual) || StringUtils.isNotBlank(original)) {
                fileInfo.addChecksumInfo(new ChecksumInfo(type, original, actual));
            }
        }
        return fileInfo;
    }

    public void update(MetadataAware metadataAware, InternalFileInfo fileInfo) {
        Node node = metadataAware.getNode();
        setPropertiesInNodeFromInfo(node, fileInfo);

        setMimeType(node, fileInfo.getMimeType());

        for (ChecksumType type : ChecksumType.values()) {
            ChecksumInfo checksumInfo = fileInfo.getChecksumsInfo().getChecksumInfo(type);
            String actual = checksumInfo != null ? checksumInfo.getActual() : null;
            String original = getRealOriginal(checksumInfo);
            setStringProperty(node, type.getOriginalPropName(), original);
            setStringProperty(node, type.getActualPropName(), actual);
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

    public InternalFileInfo copy(InternalFileInfo original) {
        return new FileInfoImpl(original);
    }

    public MetadataInfo getMetadataInfo(MetadataAware metadataAware) {
        // TODO: Can actual build one from FileInfo
        return null;
    }
}
