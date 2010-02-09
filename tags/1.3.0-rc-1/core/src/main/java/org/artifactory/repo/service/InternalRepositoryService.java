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
package org.artifactory.repo.service;

import org.artifactory.api.common.StatusHolder;
import org.artifactory.api.config.ExportSettings;
import org.artifactory.api.repo.Lock;
import org.artifactory.api.repo.RepositoryService;
import org.artifactory.api.repo.exception.RepoAccessException;
import org.artifactory.common.ResourceStreamHandle;
import org.artifactory.descriptor.repo.LocalCacheRepoDescriptor;
import org.artifactory.descriptor.repo.RemoteRepoDescriptor;
import org.artifactory.repo.LocalRepo;
import org.artifactory.repo.RealRepo;
import org.artifactory.repo.RemoteRepo;
import org.artifactory.repo.Repo;
import org.artifactory.repo.interceptor.LocalRepoInterceptor;
import org.artifactory.repo.virtual.VirtualRepo;
import org.artifactory.resource.RepoResource;
import org.artifactory.spring.ReloadableBean;
import org.artifactory.worker.WorkMessage;
import org.jetlang.channels.Publisher;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

/**
 * User: freds Date: Jul 31, 2008 Time: 5:50:18 PM
 */
public interface InternalRepositoryService
        extends Publisher<WorkMessage>, RepositoryService, ReloadableBean {
    boolean isAnonAccessEnabled();

    LocalRepoInterceptor getLocalRepoInterceptor();

    VirtualRepo virtualRepositoryByKey(String key);

    LocalRepo localOrCachedRepositoryByKey(String key);

    List<RealRepo> getLocalAndRemoteRepositories();

    Collection<VirtualRepo> getDeclaredVirtualRepositories();

    List<LocalRepo> getLocalAndCachedRepositories();

    LocalRepo localRepositoryByKey(String key);

    RemoteRepo remoteRepositoryByKey(String key);

    Repo nonCacheRepositoryByKey(String key);

    StatusHolder assertValidPath(RealRepo repo, String path);

    @Lock(transactional = true)
    StatusHolder assertValidDeployPath(LocalRepo repo, String path);

    @Lock(transactional = true)
    void rebuildRepositories();

    @Lock(transactional = true)
    <T extends RemoteRepoDescriptor> ResourceStreamHandle downloadAndSave(
            RemoteRepo<T> remoteRepo, RepoResource res) throws IOException;

    @Lock(transactional = true)
    RepoResource unexpireIfExists(LocalRepo<LocalCacheRepoDescriptor> localCacheRepo, String path);

    @Lock(transactional = true)
    ResourceStreamHandle unexpireAndRetrieveIfExists(
            LocalRepo<LocalCacheRepoDescriptor> localCacheRepo, String path) throws IOException;

    @Lock(transactional = true)
    ResourceStreamHandle getResourceStreamHandle(RealRepo repo, RepoResource res)
            throws IOException, RepoAccessException;

    @Lock(transactional = true)
    String getChecksum(RealRepo repo, String path) throws IOException;

    @Lock(transactional = true, readOnly = true)
    void exportTo(ExportSettings settings, StatusHolder status);

    @Lock(transactional = true)
    void executeMessage(WorkMessage message);
}
