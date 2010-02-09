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

import org.apache.log4j.Logger;
import org.artifactory.api.fs.FileInfo;
import org.artifactory.api.repo.RepoPath;
import org.artifactory.repo.RealRepo;
import org.artifactory.security.RepoResource;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public class SimpleRepoResource implements org.artifactory.resource.RepoResource {
    @SuppressWarnings({"UNUSED_SYMBOL", "UnusedDeclaration"})
    private final static Logger LOGGER = Logger.getLogger(SimpleRepoResource.class);

    /**
     * relative path of the file, excluding a leading / and the file name.
     */
    private String path;
    private String repoKey;
    private String name;
    private long lastModified;
    private long lastUpdated;
    private long size;

    public SimpleRepoResource(FileInfo fileInfo) {
        setPath(fileInfo.getRelPath());
        repoKey = fileInfo.getRepoKey();
        size = fileInfo.getSize();
        lastModified = fileInfo.getLastModified();
        lastUpdated = fileInfo.getLastUpdated();
    }

    public SimpleRepoResource(String repoKey, String path) {
        setPath(path);
        this.repoKey = repoKey;
        size = 0;
        lastModified = lastUpdated = System.currentTimeMillis();
    }

    public SimpleRepoResource(RealRepo repo, String path) {
        this(repo.getKey(), path);
    }

    public SimpleRepoResource(RepoResource res) {
        this(res.getRepoKey(), res.getPath());
    }

    public RepoPath getRepoPath() {
        return new RepoPath(getRepoKey(), getPath());
    }

    public String getPath() {
        return path;
    }

    public String getDirPath() {
        return path.substring(0, path.lastIndexOf('/'));
    }

    public String getName() {
        return name;
    }

    public long getLastModified() {
        return lastModified;
    }

    public long getLastUpdated() {
        return lastUpdated;
    }

    public long getAge() {
        return lastUpdated != 0 ? System.currentTimeMillis() - lastUpdated : -1;
    }

    public String getRepoKey() {
        return repoKey;
    }

    public long getSize() {
        return size;
    }

    public void setLastModified(long lastModified) {
        this.lastModified = lastModified;
    }

    public void setLastUpdated(long lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public boolean isFound() {
        return true;
    }

    public boolean hasSize() {
        return size > 0;
    }

    protected void setPath(String path) {
        this.path = path;
        name = path.substring(path.lastIndexOf('/') + 1);
    }

    protected void setRepoKey(String repoKey) {
        this.repoKey = repoKey;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SimpleRepoResource resource = (SimpleRepoResource) o;
        return path.equals(resource.path) && repoKey.equals(resource.repoKey);

    }

    @Override
    public int hashCode() {
        int result;
        result = path.hashCode();
        result = 31 * result + repoKey.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "{" + repoKey + ":" + path + "}";
    }
}
