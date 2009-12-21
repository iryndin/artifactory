/*
 * This file is part of Artifactory.
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

import com.thoughtworks.xstream.annotations.XStreamAlias;

import java.util.Set;

/**
 * @author freds
 * @date Oct 12, 2008
 */
@XStreamAlias(FileAdditionalInfo.ROOT)
public class FileAdditionalInfo extends ItemAdditionalInfo {
    public static final String ROOT = "artifactory-file-ext";

    private ChecksumsInfo checksumsInfo;

    public FileAdditionalInfo() {
        super();
        this.checksumsInfo = new ChecksumsInfo();
    }

    public FileAdditionalInfo(FileAdditionalInfo additionalInfo) {
        super(additionalInfo);
        this.checksumsInfo = additionalInfo.getChecksumsInfo();
    }

    public ChecksumsInfo getChecksumsInfo() {
        return checksumsInfo;
    }

    @Override
    public boolean isIdentical(ItemAdditionalInfo other) {
        if (!(other instanceof FileAdditionalInfo)) {
            return false;
        }
        FileAdditionalInfo fileExtraInfo = (FileAdditionalInfo) other;
        return !(checksumsInfo != null ? !checksumsInfo.equals(fileExtraInfo.checksumsInfo) :
                fileExtraInfo.checksumsInfo != null) && super.isIdentical(other);

    }

    public String getSha1() {
        return checksumsInfo.getSha1();
    }

    public String getMd5() {
        return checksumsInfo.getMd5();
    }

    public Set<ChecksumInfo> getChecksums() {
        return checksumsInfo.getChecksums();
    }

    public void createTrustedChecksums() {
        checksumsInfo.createTrustedChecksums();
    }

    public void setChecksums(Set<ChecksumInfo> checksums) {
        checksumsInfo.setChecksums(checksums);
    }

    public void addChecksumInfo(ChecksumInfo info) {
        checksumsInfo.addChecksumInfo(info);
    }

    @Override
    public String toString() {
        return "FileAdditionalInfo{" + super.toString() + "}";
    }
}