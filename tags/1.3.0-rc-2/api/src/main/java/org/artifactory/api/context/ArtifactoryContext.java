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
package org.artifactory.api.context;

import org.artifactory.api.config.CentralConfigService;
import org.artifactory.api.config.ImportableExportable;
import org.artifactory.api.repo.RepositoryService;
import org.artifactory.api.security.AuthorizationService;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public interface ArtifactoryContext extends ImportableExportable {
    CentralConfigService getCentralConfig();

    public <T> T beanForType(Class<T> type);

    public Object getBean(String name);

    RepositoryService getRepositoryService();

    AuthorizationService getAuthorizationService();

    long getUptime();
}
