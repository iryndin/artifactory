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

package org.artifactory.jcr.fs;

//import org.artifactory.api.fs.FileAdditionalInfo;
//import org.artifactory.api.fs.InternalFileInfo;

import org.artifactory.checksum.ChecksumInfo;
import org.artifactory.checksum.ChecksumsInfo;
import org.artifactory.fs.MutableFileInfo;
import org.artifactory.mime.NamingUtils;
import org.artifactory.repo.RepoPath;

import java.util.Set;

/**
 * @author Yoav Landman
 */
public class FileInfoProxy extends ItemInfoProxy<MutableFileInfo> implements MutableFileInfo {

    public FileInfoProxy(RepoPath repoPath) {
        super(repoPath);
    }

    @Override
    public boolean isFolder() {
        //Do not materialize
        return false;
    }

    @Override
    public long getAge() {
        return getMaterialized().getAge();
    }

    @Override
    public String getMimeType() {
        //Do not materialize
        return NamingUtils.getMimeTypeByPathAsString(getRelPath());
    }

    @Override
    public void setMimeType(String mimeType) {
        getMaterialized().setMimeType(mimeType);
    }

    @Override
    public ChecksumsInfo getChecksumsInfo() {
        return getMaterialized().getChecksumsInfo();
    }

    @Override
    public void createTrustedChecksums() {
        getMaterialized().createTrustedChecksums();
    }

    @Override
    public void addChecksumInfo(ChecksumInfo info) {
        getMaterialized().addChecksumInfo(info);
    }

    @Override
    public long getSize() {
        return getMaterialized().getSize();
    }

    @Override
    public void setSize(long size) {
        getMaterialized().setSize(size);
    }

    @Override
    public String getSha1() {
        return getMaterialized().getSha1();
    }

    @Override
    public String getMd5() {
        return getMaterialized().getMd5();
    }

    @Override
    public Set<ChecksumInfo> getChecksums() {
        return getMaterialized().getChecksumsInfo().getChecksums();
    }

    @Override
    public void setChecksums(Set<ChecksumInfo> checksums) {
        getMaterialized().setChecksums(checksums);
    }
}
