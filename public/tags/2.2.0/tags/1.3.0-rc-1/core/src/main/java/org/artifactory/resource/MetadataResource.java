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
package org.artifactory.resource;

import org.artifactory.api.fs.MetadataInfo;
import org.artifactory.api.mime.ContentType;
import org.artifactory.api.repo.RepoPath;
import org.artifactory.utils.PathUtils;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public class MetadataResource implements RepoResource {

    private final MetadataInfo info;

    public MetadataResource(MetadataInfo info) {
        this.info = info;
    }

    public MetadataResource(RepoPath repoPath) {
        this.info = new MetadataInfo(repoPath);
    }

    public RepoPath getRepoPath() {
        return info.getRepoPath();
    }

    public MetadataInfo getInfo() {
        return info;
    }

    public String getParentPath() {
        return PathUtils.getParent(getRepoPath().getPath());
    }

    public boolean isFound() {
        return true;
    }

    public boolean isExpired() {
        return false;
    }

    public boolean isMetadata() {
        return true;
    }

    public boolean hasSize() {
        return info.getSize() > 0;
    }

    public long getSize() {
        return info.getSize();
    }

    public long getLastModified() {
        return info.getLastModified();
    }

    public String getMimeType() {
        return ContentType.applicationXml.getMimeType();
    }

    public long getAge() {
        long lastUpdated = info.getLastModified();
        if (lastUpdated <= 0) {
            return -1;
        }
        return System.currentTimeMillis() - lastUpdated;
    }

    @Override
    public String toString() {
        return info.getRepoPath().toString();
    }
}