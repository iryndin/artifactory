/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.artifactory.api.fs;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import org.artifactory.api.mime.NamingUtils;
import org.artifactory.api.repo.RepoPath;
import org.artifactory.utils.PathUtils;

import java.util.Set;

/**
 * Basic information about the file. Internally not stored as XML but as node properties
 *
 * @author yoavl
 */
@XStreamAlias(FileInfo.ROOT)
public class FileInfo extends ItemInfo implements RepoResourceInfo {
    public static final String ROOT = "artifactory-file";

    private long size;
    private String mimeType;
    private FileAdditionalInfo additionalInfo;

    public FileInfo(RepoPath repoPath) {
        super(repoPath);
        this.size = 0;
        //Force a mime type
        setMimeType(null);
        this.additionalInfo = new FileAdditionalInfo();
    }

    public FileInfo(FileInfo info) {
        super(info);
        this.size = info.getSize();
        setMimeType(info.getMimeType());
        this.additionalInfo = new FileAdditionalInfo(info.additionalInfo);
    }

    @Override
    public boolean isFolder() {
        return false;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public long getAge() {
        return getLastUpdated() != 0 ?
                System.currentTimeMillis() - getLastUpdated() : -1;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
        if (this.mimeType == null) {
            this.mimeType = NamingUtils.getMimeTypeByPathAsString(getRelPath());
        }
    }

    public long getLastUpdated() {
        return additionalInfo.getLastUpdated();
    }

    public void setLastUpdated(long lastUpdated) {
        additionalInfo.setLastUpdated(lastUpdated);
    }

    public String getModifiedBy() {
        return additionalInfo.getModifiedBy();
    }

    public String getCreatedBy() {
        return additionalInfo.getCreatedBy();
    }

    public String getSha1() {
        return additionalInfo.getSha1();
    }

    public String getMd5() {
        return additionalInfo.getMd5();
    }

    public ChecksumsInfo getChecksumsInfo() {
        return additionalInfo.getChecksumsInfo();
    }

    @Override
    public String toString() {
        return "FileInfo{" +
                super.toString() +
                ", size=" + size +
                ", mimeType='" + mimeType + '\'' +
                ", extension=" + additionalInfo +
                '}';
    }

    @Override
    public boolean isIdentical(ItemInfo info) {
        if (!(info instanceof FileInfo)) {
            return false;
        }
        FileInfo fileInfo = (FileInfo) info;
        return this.size == fileInfo.size &&
                PathUtils.safeStringEquals(this.mimeType, fileInfo.mimeType) &&
                this.additionalInfo.isIdentical(fileInfo.additionalInfo) &&
                super.isIdentical(info);
    }

    /**
     * {@inheritDoc}
     *
     * @return
     */
    @Override
    @Deprecated
    public FileAdditionalInfo getInernalXmlInfo() {
        return additionalInfo;
    }

    /**
     * Should not be called by clients - for internal use
     *
     * @return
     */
    public void setAdditionalInfo(FileAdditionalInfo additionalInfo) {
        this.additionalInfo = additionalInfo;
    }

    public Set<ChecksumInfo> getChecksums() {
        return additionalInfo.getChecksums();
    }

    public void setChecksums(Set<ChecksumInfo> checksums) {
        additionalInfo.setChecksums(checksums);
    }

    public void createTrustedChecksums() {
        this.additionalInfo.createTrustedChecksums();
    }

    public void addChecksumInfo(ChecksumInfo info) {
        additionalInfo.addChecksumInfo(info);
    }
}
