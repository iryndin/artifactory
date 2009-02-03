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
package org.artifactory.repo;

import org.apache.log4j.Logger;
import org.springframework.util.Assert;

import java.io.Serializable;

/**
 * An object identity that represents a repository and a groupId
 * <p/>
 * Created by IntelliJ IDEA. User: yoavl
 */
public class RepoPath implements Serializable {
    @SuppressWarnings({"UNUSED_SYMBOL", "UnusedDeclaration"})
    private final static Logger LOGGER = Logger.getLogger(RepoPath.class);

    public static final char REPO_PATH_SEP = ':';

    private String repoKey;
    private String path;

    public RepoPath(String repoKey, String path) {
        this.repoKey = repoKey;
        setPath(path);
    }

    public RepoPath(String id) {
        Assert.notNull(id, "RepoAndGroupIdIdentity cannot have a null id");
        int idx = id.indexOf(REPO_PATH_SEP);
        Assert.state(idx > 0, "Could not determine both repository key and groupId from '" +
                id + "'.");
        repoKey = id.substring(0, idx);
        setPath(id.substring(idx + 1));
    }

    public String getRepoKey() {
        return repoKey;
    }

    public void setRepoKey(String repoKey) {
        this.repoKey = repoKey;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        //Trim leading '/' (casued by webdav requests)
        if (path.startsWith("/")) {
            this.path = path.substring(1);
        } else {
            this.path = path;
        }
    }

    public String getId() {
        return repoKey + REPO_PATH_SEP + path;
    }

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

    public int hashCode() {
        int result;
        result = repoKey.hashCode();
        result = 31 * result + path.hashCode();
        return result;
    }

    public String toString() {
        return getId();
    }
}