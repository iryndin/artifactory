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

import org.artifactory.api.repo.exception.FileExpectedException;
import org.artifactory.api.repo.exception.RepoAccessException;
import org.artifactory.common.ResourceStreamHandle;
import org.artifactory.descriptor.repo.RealRepoDescriptor;
import org.artifactory.resource.RepoResource;

import java.io.IOException;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public interface RealRepo<T extends RealRepoDescriptor> extends Repo<T> {

    String getUrl();

    RepoResource getInfo(String path) throws FileExpectedException;

    boolean isLocal();

    boolean isCache();

    boolean isHandleReleases();

    boolean isHandleSnapshots();

    String getIncludesPattern();

    String getExcludesPattern();

    boolean isBlackedOut();

    ResourceStreamHandle getResourceStreamHandle(RepoResource res)
            throws IOException, RepoAccessException, FileExpectedException;

    boolean accepts(String path);

    boolean allowsDownload(String path);

    boolean handles(String path);

    int getMaxUniqueSnapshots();

    String getProperty(String path) throws IOException;
}
