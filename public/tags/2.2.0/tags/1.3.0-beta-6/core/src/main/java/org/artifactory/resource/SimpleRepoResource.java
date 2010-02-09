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

import org.artifactory.api.fs.FileInfo;
import org.artifactory.api.repo.RepoPath;
import org.artifactory.utils.PathUtils;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public class SimpleRepoResource implements RepoResource {

    private final FileInfo info;

    public SimpleRepoResource(FileInfo fileInfo) {
        this.info = new FileInfo(fileInfo);
    }

    public SimpleRepoResource(RepoPath repoPath) {
        this.info = new FileInfo(repoPath);
    }

    public RepoPath getRepoPath() {
        return info.getRepoPath();
    }

    public FileInfo getInfo() {
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

    public boolean hasSize() {
        return info.getSize() > 0;
    }

    public long getLastModified() {
        return info.getLastModified();
    }

    public long getAge() {
        long lastUpdated = info.getExtension().getLastUpdated();
        if (lastUpdated <= 0) {
            return -1;
        }
        return System.currentTimeMillis() - lastUpdated;
    }
}
