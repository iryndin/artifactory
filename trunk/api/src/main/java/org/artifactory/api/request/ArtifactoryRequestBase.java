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
package org.artifactory.api.request;

import org.artifactory.api.mime.PackagingType;
import org.artifactory.api.repo.RepoPath;
import org.artifactory.utils.PathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class ArtifactoryRequestBase implements ArtifactoryRequest {
    @SuppressWarnings({"UnusedDeclaration"})
    private static final Logger log = LoggerFactory.getLogger(ArtifactoryRequestBase.class);

    private RepoPath repoPath;

    private long modificationTime = -1;

    public RepoPath getRepoPath() {
        return repoPath;
    }

    public String getRepoKey() {
        return repoPath.getRepoKey();
    }

    public String getPath() {
        return repoPath.getPath();
    }

    public boolean isSnapshot() {
        return PackagingType.isSnapshot(getPath());
    }

    public boolean isMetadata() {
        boolean metadata = PackagingType.isMetadata(getPath());
        return metadata;
    }

    public boolean isResourceProperty() {
        return PackagingType.isChecksum(getPath());
    }

    public boolean isChecksum() {
        return PackagingType.isChecksum(getPath());
    }

    public String getResourcePath() {
        String path = getPath();
        String resourcePath;
        /*if (isMetadata()) {
            //For metadata get the resource containing the metadata (alwyas a version or an artifact folder
            resourcePath = path.substring(0, path.lastIndexOf("/"));
        } else*/
        if (isResourceProperty()) {
            //For checksums search the containing resource
            resourcePath = path.substring(0, path.lastIndexOf("."));
        } else {
            resourcePath = path;
        }
        return resourcePath;
    }

    public String getName() {
        String path = getPath();
        return PathUtils.getName(path);
    }

    public String getDir() {
        String path = getPath();
        int dirEndIdx = path.lastIndexOf('/');
        if (dirEndIdx == -1) {
            return null;
        }

        return path.substring(0, dirEndIdx);
    }

    public boolean isNewerThanResource(long resourceLastModified) {
        long modificationTime = getModificationTime();
        //Check that the res has a modification time and that it is older than the request's one
        return resourceLastModified >= 0 && resourceLastModified <= modificationTime;
    }

    public long getModificationTime() {
        //If not calculated yet
        if (modificationTime < 0) {
            //These headers are not filled by mvn lw-http wagon (doesn't call "getIfNewer")
            if (getLastModified() < 0 && getIfModifiedSince() < 0) {
                if (log.isDebugEnabled()) {
                    log.debug("Neither If-Modified-Since nor Last-Modified are set");
                }
                return -1;
            }
            if (getLastModified() >= 0 && getIfModifiedSince() >= 0 && getLastModified() != getIfModifiedSince()) {
                if (log.isDebugEnabled()) {
                    log.warn(
                            "If-Modified-Since (" + getIfModifiedSince() + ") AND Last-Modified (" + getLastModified() +
                                    ") both set and unequal");
                }

            }
            modificationTime = Math.max(getLastModified(), getIfModifiedSince());
        }
        return modificationTime;
    }

    protected void setRepoPath(RepoPath repoPath) {
        this.repoPath = repoPath;
    }

    public static long round(long time) {
        if (time != -1) {
            return time / 1000 * 1000;
        }
        return time;
    }
}