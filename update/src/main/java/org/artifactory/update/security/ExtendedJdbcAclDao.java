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
package org.artifactory.update.security;

import org.springframework.security.acl.basic.BasicAclExtendedDao;
import org.springframework.security.acl.basic.SimpleAclEntry;

import javax.sql.DataSource;
import java.util.List;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public interface ExtendedJdbcAclDao extends BasicAclExtendedDao {

    DataSource getDataSource();

    void createAclObjectIdentity(
            RepoPath aclObjectIdentity, RepoPath aclParentObjectIdentity);

    List<RepoPath> getAllRepoPaths();

    void deleteAcls(RepoPath identity);

    void deleteAcls(String recipient);

    /**
     * Delete any existing acl matching the same identity and recipient then recreate it with the
     * new data
     *
     * @param aclEntry
     */
    void update(SimpleAclEntry aclEntry);
}
