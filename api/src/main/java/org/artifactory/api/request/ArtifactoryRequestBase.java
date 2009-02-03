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

import org.apache.log4j.Logger;
import org.artifactory.api.common.PackagingType;
import org.artifactory.api.repo.RepoPath;

public abstract class ArtifactoryRequestBase implements ArtifactoryRequest {
    @SuppressWarnings({"UnusedDeclaration"})
    private static final Logger LOGGER = Logger.getLogger(ArtifactoryRequestBase.class);

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

    public boolean isMetaData() {
        return PackagingType.isMavenMetadata(getPath());
    }

    public boolean isResourceProperty() {
        return PackagingType.isChecksum(getPath());
    }

    public boolean isChecksum() {
        return PackagingType.isChecksum(getPath());
    }

    public boolean isPom() {
        return PackagingType.isPom(getPath());
    }

    public String getName() {
        String path = getPath();
        int dirEndIdx = path.lastIndexOf('/');
        if (dirEndIdx != -1) {
            return path.substring(dirEndIdx + 1);
        } else {
            return path;
        }
    }

    public String getDir() {
        String path = getPath();
        int dirEndIdx = path.lastIndexOf('/');
        if (dirEndIdx != -1) {
            return path.substring(0, dirEndIdx);
        } else {
            return null;
        }
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
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Neither If-Modified-Since nor Last-Modified are set");
                }
                return -1;
            }
            if (getLastModified() >= 0 && getIfModifiedSince() >= 0
                    && getLastModified() != getIfModifiedSince()) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.warn("If-Modified-Since (" + getIfModifiedSince()
                            + ") AND Last-Modified ("
                            + getLastModified() + ") both set and unequal");
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