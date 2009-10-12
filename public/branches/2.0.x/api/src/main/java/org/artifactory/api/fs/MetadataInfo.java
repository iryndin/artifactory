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

import org.artifactory.api.mime.NamingUtils;
import org.artifactory.api.repo.RepoPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

/**
 * @author yoavl
 */
public class MetadataInfo implements RepoResourceInfo {
    @SuppressWarnings({"UnusedDeclaration"})
    private final static Logger log = LoggerFactory.getLogger(MetadataInfo.class);

    private String name;
    private final RepoPath repoPath;
    private long created;
    private long lastModified;
    private String lastModifiedBy;
    private long size;
    private ChecksumsInfo checksumsInfo;

    public MetadataInfo(RepoPath parentRepoPath, String metadataName) {
        this.repoPath = getMetadataRepoPath(parentRepoPath, metadataName);
        this.name = metadataName;
        this.checksumsInfo = new ChecksumsInfo();
    }

    public MetadataInfo(RepoPath repoPath) {
        this.repoPath = repoPath;
        this.name = NamingUtils.getMetadataName(repoPath.getPath());
        this.checksumsInfo = new ChecksumsInfo();
    }

    public RepoPath getRepoPath() {
        return repoPath;
    }

    public ChecksumsInfo getChecksumsInfo() {
        return checksumsInfo;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getCreated() {
        return created;
    }

    public void setCreated(long created) {
        this.created = created;
    }

    public long getLastModified() {
        return lastModified;
    }

    public void setLastModified(long lastModified) {
        this.lastModified = lastModified;
    }

    public String getLastModifiedBy() {
        return lastModifiedBy;
    }

    public void setLastModifiedBy(String lastModifiedBy) {
        this.lastModifiedBy = lastModifiedBy;
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

    public void setChecksums(Set<ChecksumInfo> checksums) {
        checksumsInfo.setChecksums(checksums);
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    @Override
    public String toString() {
        return "MetadataInfo{repoPath=" + repoPath + '}';
    }

    private RepoPath getMetadataRepoPath(RepoPath parentRepoPath, String metadataName) {
        String path = parentRepoPath.getPath();
        boolean alreadyMetadataPath = NamingUtils.isMetadata(path);
        if (alreadyMetadataPath) {
            log.warn("ALREADY");
        }
        //TODO: [by yl] Evaluate the impact of normalizing the path to use the standard metadata format (a.jar#mdname)
        return new RepoPath(parentRepoPath.getRepoKey(),
                alreadyMetadataPath ? path : path + NamingUtils.METADATA_PREFIX + metadataName);
    }
}
