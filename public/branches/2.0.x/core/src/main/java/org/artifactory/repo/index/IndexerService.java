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
package org.artifactory.repo.index;

import org.artifactory.api.repo.Lock;
import org.artifactory.spring.ReloadableBean;

import java.util.Date;

/**
 * User: freds Date: Aug 13, 2008 Time: 12:04:07 PM
 */
public interface IndexerService extends ReloadableBean {

    void index(Date fireTime);

    @Lock(transactional = true)
    void saveIndexFiles(RepoIndexerData repoIndexerData);

    @Lock(transactional = true)
    void findOrCreateIndex(RepoIndexerData repoIndexerData, Date fireTime);
}
