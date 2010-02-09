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
package org.artifactory.api.repo;

import org.artifactory.api.common.Info;
import org.artifactory.api.mime.NamingUtils;
import org.artifactory.api.security.PermissionTargetInfo;
import org.artifactory.utils.PathUtils;

/**
 * An object identity that represents a repository and a groupId
 * <p/>
 * Created by IntelliJ IDEA. User: yoavl
 */
public final class RepoPath implements Info {

    public static final char REPO_PATH_SEP = ':';

    private final String repoKey;
    private final String path;

    /**
     * @param repoKey The key of any repo
     * @param path    The relativ path inside the repo
     */
    public RepoPath(String repoKey, String path) {
        this.repoKey = repoKey;
        this.path = PathUtils.formatRelativePath(path);
    }

    /**
     * Create a repo path representing the child of parent
     *
     * @param parent the repo path of the parent folder
     * @param child  the child name
     */
    public RepoPath(RepoPath parent, String child) {
        this.repoKey = parent.repoKey;
        this.path = PathUtils.formatRelativePath(parent.path + "/" + child);
    }

    public RepoPath(String id) {
        if (id == null || id.length() == 0) {
            throw new IllegalArgumentException(
                    "RepoAndGroupIdIdentity cannot have a null id");
        }
        int idx = id.indexOf(REPO_PATH_SEP);
        if (idx <= 0) {
            throw new IllegalArgumentException(
                    "Could not determine both repository key and groupId from '" +
                            id + "'.");
        }
        this.repoKey = id.substring(0, idx);
        this.path = PathUtils.formatRelativePath(id.substring(idx + 1));
    }

    public String getRepoKey() {
        return repoKey;
    }

    public String getPath() {
        return path;
    }

    public String getId() {
        return repoKey + REPO_PATH_SEP + path;
    }

    public String getName() {
        return PathUtils.getName(getPath());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        RepoPath repoPath = (RepoPath) o;
        return path.equals(repoPath.path) && repoKey.equals(repoPath.repoKey);
    }

    @Override
    public int hashCode() {
        int result;
        result = repoKey.hashCode();
        result = 31 * result + path.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return getId();
    }

    public static RepoPath repoPathForRepo(String repoKey) {
        return new RepoPath(repoKey, PermissionTargetInfo.ANY_PATH);
    }

    public static RepoPath repoPathForPath(String path) {
        return new RepoPath(PermissionTargetInfo.ANY_REPO, path);
    }

    public static RepoPath repoPathForAny() {
        return new RepoPath(PermissionTargetInfo.ANY_REPO, PermissionTargetInfo.ANY_PATH);
    }

    public static RepoPath getMetadataContainerRepoPath(RepoPath metdadataRepoPath) {
        String path = metdadataRepoPath.getPath();
        if (NamingUtils.isMetadata(path)) {
            String fsItemPath = NamingUtils.getMetadataParentPath(path);
            return new RepoPath(metdadataRepoPath.getRepoKey(), fsItemPath);
        } else {
            return metdadataRepoPath;
        }
    }
}