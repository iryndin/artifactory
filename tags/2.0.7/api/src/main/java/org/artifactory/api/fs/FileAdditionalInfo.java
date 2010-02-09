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
}