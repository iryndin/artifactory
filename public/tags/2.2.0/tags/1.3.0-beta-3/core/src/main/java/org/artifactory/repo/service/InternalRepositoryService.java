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
import org.artifactory.api.config.ImportableExportable;
import org.artifactory.api.repo.RepositoryService;
import org.artifactory.api.repo.exception.FileExpectedException;
import org.artifactory.api.repo.exception.RepoAccessException;
import org.artifactory.common.ResourceStreamHandle;
import org.artifactory.descriptor.repo.LocalCacheRepoDescriptor;
import org.artifactory.descriptor.repo.LocalRepoDescriptor;
import org.artifactory.descriptor.repo.RemoteRepoDescriptor;
import org.artifactory.repo.LocalRepo;
import org.artifactory.repo.RealRepo;
import org.artifactory.repo.RemoteRepo;
import org.artifactory.repo.Repo;
import org.artifactory.repo.interceptor.LocalRepoInterceptor;
import org.artifactory.repo.virtual.VirtualRepo;
import org.artifactory.resource.RepoResource;
import org.artifactory.spring.PostInitializingBean;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

/**
 * User: freds Date: Jul 31, 2008 Time: 5:50:18 PM
 */
public interface InternalRepositoryService
        extends RepositoryService, ImportableExportable, PostInitializingBean {
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

    void deleteFullRepo(LocalRepoDescriptor repo, StatusHolder status);

    public StatusHolder assertValidPath(RealRepo repo, String path);

    StatusHolder assertValidDeployPath(LocalRepo repo, String path);

    void restartWorkingCopyCommitter();

    void rebuildRepositories();

    void stopWorkingCopyCommitter();

    <T extends RemoteRepoDescriptor> ResourceStreamHandle downloadAndSave(RemoteRepo<T> remoteRepo,
            RepoResource res) throws IOException;

    RepoResource unexpireIfExists(LocalRepo<LocalCacheRepoDescriptor> localCacheRepo, String path
    );

    ResourceStreamHandle unexpireAndRetrieveIfExists(
            LocalRepo<LocalCacheRepoDescriptor> localCacheRepo, String path)
            throws RepoAccessException, IOException;

    ResourceStreamHandle getResourceStreamHandle(RealRepo repo, RepoResource res)
            throws RepoAccessException, IOException;

    @Transactional
    RepoResource retrieveInfo(LocalRepo repo, String path) throws FileExpectedException;
}
