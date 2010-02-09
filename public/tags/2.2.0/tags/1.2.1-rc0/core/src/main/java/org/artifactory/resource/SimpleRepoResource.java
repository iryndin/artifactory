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
import org.artifactory.jcr.JcrFile;
import org.artifactory.repo.Repo;
import org.artifactory.security.SecuredResource;

import java.util.Date;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public class SimpleRepoResource implements RepoResource {
    @SuppressWarnings({"UNUSED_SYMBOL", "UnusedDeclaration"})
    private final static Logger LOGGER = Logger.getLogger(SimpleRepoResource.class);

    /**
     * relative path of the file, excluding a leading / and the file name.
     */
    private String path;
    private String repoKey;
    private String name;
    private Date lastModified;
    private Date lastUpdated;
    private long size;

    public SimpleRepoResource(JcrFile jcrFile) {
        setPath(jcrFile.relPath());
        repoKey = jcrFile.repoKey();
        size = jcrFile.size();
        lastModified = jcrFile.lastModified();
        lastUpdated = jcrFile.lastUpdated();
    }

    public SimpleRepoResource(String path, String repoKey) {
        setPath(path);
        this.repoKey = repoKey;
        size = 0;
        lastModified = lastUpdated = new Date();
    }

    public SimpleRepoResource(String path, Repo repo) {
        this(path, repo.getKey());
    }

    public SimpleRepoResource(SecuredResource res) {
        this(res.getPath(), res.getRepoKey());
    }

    public String getPath() {
        return path;
    }

    public String getDirPath() {
        return path.substring(0, path.lastIndexOf('/'));
    }

    public String getAbsPath() {
        return "/" + repoKey + "/" + path;
    }

    public String getName() {
        return name;
    }

    public Date getLastModified() {
        return lastModified;
    }

    public long getLastModifiedTime() {
        return lastModified.getTime();
    }

    public Date getLastUpdated() {
        return lastUpdated;
    }

    public long getAge() {
        return lastUpdated != null ? System.currentTimeMillis() - lastUpdated.getTime() : -1;
    }

    public String getRepoKey() {
        return repoKey;
    }

    public long getSize() {
        return size;
    }

    public void setLastModified(Date lastModified) {
        this.lastModified = lastModified;
    }

    public void setLastModifiedTime(long lastModified) {
        this.lastModified = new Date(lastModified);
    }

    public void setLastUpdated(Date lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public boolean isFound() {
        return true;
    }

    protected void setPath(String path) {
        this.path = path;
        name = path.substring(path.lastIndexOf('/') + 1);
    }

    protected void setRepoKey(String repoKey) {
        this.repoKey = repoKey;
    }

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

    public int hashCode() {
        int result;
        result = path.hashCode();
        result = 31 * result + repoKey.hashCode();
        return result;
    }

    public String toString() {
        return "{" + repoKey + ":" + path + "}";
    }
}
