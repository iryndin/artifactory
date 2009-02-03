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
package org.artifactory.api.webdav;

import org.artifactory.api.repo.Lock;
import org.artifactory.api.request.ArtifactoryRequest;
import org.artifactory.api.request.ArtifactoryResponse;

import java.io.IOException;

/**
 * User: freds Date: Jul 27, 2008 Time: 9:26:56 PM
 */
public interface WebdavService {
    /**
     * PROPFIND Method.
     *
     * @throws java.io.IOException
     */
    @Lock(transactional = true)
    void handlePropfind(ArtifactoryRequest request,
            ArtifactoryResponse response) throws IOException;

    @Lock(transactional = true)
    void handleMkcol(ArtifactoryRequest request,
            ArtifactoryResponse response) throws IOException;

    @Lock(transactional = true)
    void handleDelete(ArtifactoryRequest request,
            ArtifactoryResponse response) throws IOException;

    void handleOptions(ArtifactoryResponse response) throws IOException;
}
