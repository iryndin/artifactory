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

package org.artifactory.model.xstream.fs;

import org.artifactory.checksum.ChecksumInfo;
import org.artifactory.checksum.ChecksumsInfo;
import org.artifactory.fs.FileInfo;
import org.artifactory.fs.ZipEntryInfo;
import org.artifactory.fs.ZipEntryResourceInfo;
import org.artifactory.mime.MimeType;
import org.artifactory.mime.NamingUtils;

import javax.annotation.Nonnull;
import java.util.Set;

/**
 * Holds zip entry info for consumption by the download process. This resource exists inside a zip file.
 *
 * @author Yossi Shaul
 */
public class ZipEntryResourceImpl extends ItemInfoImpl implements ZipEntryResourceInfo {
    /**
     * The file info concerning the zip file that contains this zip entry
     */
    private final FileInfo zipFileInfo;
    /**
     * Basic information about the zip entry
     */
    private final ZipEntryInfo zipEntryInfo;
    /**
     * The actual size of the zip entry computed during checksum calculation. This is required since because the java
     * zip input stream doesn't always know how to read this data.
     */
    private final long actualSize;
    /**
     * Checksum information
     */
    private FileAdditionalInfo additionalInfo;

    public ZipEntryResourceImpl(FileInfo zipFileInfo, ZipEntryInfo zipEntryInfo, long actualSize,
            ChecksumsInfo checksums) {
        super(zipFileInfo);
        //super(RepoPathFactory.archiveResourceRepoPath(zipFileInfo.getRepoPath(), zipEntryInfo.getPath()));
        this.zipFileInfo = zipFileInfo;
        this.zipEntryInfo = zipEntryInfo;
        this.actualSize = actualSize;
        additionalInfo = new FileAdditionalInfo(checksums);
    }

    public ZipEntryResourceImpl(ZipEntryResourceInfo info) {
        super(info.getZipFileInfo());
        this.zipFileInfo = info.getZipFileInfo();
        this.zipEntryInfo = info.getZipEntryInfo();
        this.actualSize = info.getSize();
        additionalInfo = new FileAdditionalInfo((FileAdditionalInfo) ((InternalItemInfo) info).getAdditionalInfo());
    }

    public FileInfo getZipFileInfo() {
        return zipFileInfo;
    }

    public ZipEntryInfo getZipEntryInfo() {
        return zipEntryInfo;
    }

    /**
     * @return The name of the zip entry
     */
    public String getEntryPath() {
        return zipEntryInfo.getPath();
    }

    public ItemAdditionalInfo getAdditionalInfo() {
        return additionalInfo;
    }

    public boolean isFolder() {
        return false;
    }

    public long getAge() {
        return zipFileInfo.getAge();
    }

    public String getMimeType() {
        MimeType contentType = NamingUtils.getMimeType(zipEntryInfo.getName());
        return contentType.getType();
    }

    @Nonnull
    public ChecksumsInfo getChecksumsInfo() {
        return additionalInfo.getChecksumsInfo();
    }

    public long getSize() {
        return actualSize;
    }

    public String getSha1() {
        return getChecksumsInfo().getSha1();
    }

    public String getMd5() {
        return getChecksumsInfo().getMd5();
    }

    public Set<ChecksumInfo> getChecksums() {
        return getChecksumsInfo().getChecksums();
    }
}
