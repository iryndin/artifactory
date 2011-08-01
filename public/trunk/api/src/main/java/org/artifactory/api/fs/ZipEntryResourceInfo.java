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

package org.artifactory.api.fs;

import org.artifactory.api.mime.NamingUtils;
import org.artifactory.api.tree.fs.ZipEntryInfo;
import org.artifactory.checksum.ChecksumInfo;
import org.artifactory.checksum.ChecksumsInfo;
import org.artifactory.fs.FileInfo;
import org.artifactory.mime.MimeType;

import java.util.Set;

/**
 * Holds zip entry info for consumption by the download process. This resource exists inside a zip file.
 *
 * @author Yossi Shaul
 */
public class ZipEntryResourceInfo extends ItemInfoImpl implements FileInfo {
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

    public ZipEntryResourceInfo(FileInfo zipFileInfo, ZipEntryInfo zipEntryInfo, long actualSize,
            Set<ChecksumInfo> checksums) {
        super(zipFileInfo);
        this.zipFileInfo = zipFileInfo;
        this.zipEntryInfo = zipEntryInfo;
        this.actualSize = actualSize;
        additionalInfo = new FileAdditionalInfo();
        additionalInfo.setChecksums(checksums);
    }

    /**
     * @return The name of the zip entry
     */
    public String getEntryName() {
        return zipEntryInfo.getName();
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

    public void setMimeType(String mimeType) {
        // not supported
    }

    public ChecksumsInfo getChecksumsInfo() {
        return additionalInfo.getChecksumsInfo();
    }

    public void createTrustedChecksums() {
        //TODO: [by YS] maybe implementing FileInfo is not the best fit
        // not relevant here
    }

    public void addChecksumInfo(ChecksumInfo info) {
        // not relevant here
    }

    public long getSize() {
        return actualSize;
    }

    public void setSize(long size) {
        // the size ins taken from the entry
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

    public void setChecksums(Set<ChecksumInfo> checksums) {
        // not supported here
    }
}
