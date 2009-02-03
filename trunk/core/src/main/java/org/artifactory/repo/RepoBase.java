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

import org.artifactory.api.security.AuthorizationService;
import org.artifactory.descriptor.repo.RepoDescriptor;
import org.artifactory.repo.service.InternalRepositoryService;
import org.artifactory.spring.InternalContextHelper;

public abstract class RepoBase<T extends RepoDescriptor> implements Repo<T> {
    private T descriptor;
    private InternalRepositoryService repositoryService;

    protected RepoBase(InternalRepositoryService repositoryService) {
        this.repositoryService = repositoryService;
    }

    protected RepoBase(InternalRepositoryService repositoryService, T descriptor) {
        this(repositoryService);
        setDescriptor(descriptor);
    }

    public void setDescriptor(T descriptor) {
        this.descriptor = descriptor;
    }

    public T getDescriptor() {
        return descriptor;
    }

    public InternalRepositoryService getRepositoryService() {
        return repositoryService;
    }

    public String getKey() {
        return descriptor.getKey();
    }

    public String getDescription() {
        return descriptor.getDescription();
    }

    public boolean isReal() {
        return getDescriptor().isReal();
    }

    @Override
    public String toString() {
        return getKey();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RepoBase)) {
            return false;
        }
        RepoBase base = (RepoBase) o;
        return descriptor.equals(base.descriptor);
    }

    @Override
    public int hashCode() {
        return descriptor.hashCode();
    }

    protected AuthorizationService getAuthorizationService() {
        // TODO: Analyze the optimization if made as a member
        return InternalContextHelper.get().getAuthorizationService();
    }
}