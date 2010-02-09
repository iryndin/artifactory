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
package org.artifactory.security;

import org.artifactory.api.security.AclInfo;
import org.artifactory.spring.ReloadableBean;
import org.artifactory.util.AlreadyExistsException;
import org.springframework.security.acls.NotFoundException;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public interface AclManager extends ReloadableBean {
    List<Acl> getAllAcls();

    @Transactional
    List<Acl> getAllAcls(ArtifactorySid[] sids);

    @Transactional
    List<AclInfo> getAllAclDescriptors();

    @Transactional
    Acl createAcl(Acl acl);

    void deleteAcl(PermissionTarget permissionTarget);

    void deleteAllAcls();

    @Transactional
    Acl updateAcl(Acl acl) throws NotFoundException;

    @Transactional
    Acl createAcl(PermissionTarget permissionTarget) throws AlreadyExistsException;

    @Transactional
    List<PermissionTarget> getAllPermissionTargets();

    /**
     * @param permissionTarget The permission target which is the Acl id.
     * @return An Acl by its id - the permission target. Null if not found
     */
    @Transactional
    Acl findAclById(PermissionTarget permissionTarget);

    @Transactional
    void createAnythingPermision(SimpleUser anonUser);

    void removeAllUserAces(String username);
}