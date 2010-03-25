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

package org.artifactory.jcr.fs;

import org.artifactory.api.fs.ChecksumInfo;
import org.artifactory.api.fs.ChecksumsInfo;
import org.artifactory.api.fs.FileAdditionalInfo;
import org.artifactory.api.fs.FileInfo;
import org.artifactory.api.mime.NamingUtils;
import org.artifactory.api.repo.RepoPath;
import org.artifactory.log.LoggerFactory;
import org.slf4j.Logger;

import java.util.Set;

/**
 * @author Yoav Landman
 */
public class FileInfoProxy extends ItemInfoProxy<FileInfo> implements FileInfo {
    @SuppressWarnings({"UnusedDeclaration"})
    private static final Logger log = LoggerFactory.getLogger(FileInfoProxy.class);

    public FileInfoProxy(RepoPath repoPath) {
        super(repoPath);
    }

    public boolean isFolder() {
        //Do not materialize
        return false;
    }

    public FileAdditionalInfo getAdditionalInfo() {
        return getMaterialized().getAdditionalInfo();
    }

    public long getAge() {
        return getMaterialized().getAge();
    }

    public String getMimeType() {
        //Do not materialize
        return NamingUtils.getMimeTypeByPathAsString(getRelPath());
    }

    public void setMimeType(String mimeType) {
        getMaterialized().setMimeType(mimeType);
    }

    public ChecksumsInfo getChecksumsInfo() {
        return getMaterialized().getChecksumsInfo();
    }

    public void setAdditionalInfo(FileAdditionalInfo additionalInfo) {
        getMaterialized().setAdditionalInfo(additionalInfo);
    }

    public void createTrustedChecksums() {
        getMaterialized().createTrustedChecksums();
    }

    public void addChecksumInfo(ChecksumInfo info) {
        getMaterialized().addChecksumInfo(info);
    }

    public long getSize() {
        return getMaterialized().getSize();
    }

    public void setSize(long size) {
        getMaterialized().setSize(size);
    }

    public String getSha1() {
        return getMaterialized().getSha1();
    }

    public String getMd5() {
        return getMaterialized().getMd5();
    }

    public Set<ChecksumInfo> getChecksums() {
        return getMaterialized().getChecksums();
    }

    public void setChecksums(Set<ChecksumInfo> checksums) {
        getMaterialized().setChecksums(checksums);
    }
}