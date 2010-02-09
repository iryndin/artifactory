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

import org.artifactory.api.fs.RepoResourceInfo;
import org.artifactory.api.repo.RepoPath;

public class ExpiredRepoResource implements RepoResource {

    private RepoResource wrappedResource;

    public ExpiredRepoResource(RepoResource wrappedResource) {
        this.wrappedResource = wrappedResource;
    }

    public RepoPath getRepoPath() {
        return wrappedResource.getRepoPath();
    }

    public RepoResourceInfo getInfo() {
        return wrappedResource.getInfo();
    }

    public String getParentPath() {
        return wrappedResource.getParentPath();
    }

    public boolean hasSize() {
        return wrappedResource.hasSize();
    }

    public long getSize() {
        return wrappedResource.getSize();
    }

    public long getAge() {
        return wrappedResource.getAge();
    }

    public long getLastModified() {
        return wrappedResource.getLastModified();
    }

    public String getMimeType() {
        return wrappedResource.getMimeType();
    }

    public boolean isFound() {
        return false;
    }

    public boolean isExpired() {
        return true;
    }

    public boolean isMetadata() {
        return wrappedResource.isMetadata();
    }

    @Override
    public String toString() {
        return wrappedResource.getRepoPath().toString();
    }
}