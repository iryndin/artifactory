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
import org.artifactory.spring.PostInitializingBean;
import org.springframework.security.acls.AlreadyExistsException;
import org.springframework.security.acls.NotFoundException;

import java.util.List;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public interface AclManager extends PostInitializingBean {
    List<Acl> getAllAcls();

    List<Acl> getAllAcls(ArtifactorySid[] sids);

    List<AclInfo> getAllAclDescriptors();

    Acl createAcl(Acl acl);

    void deleteAcl(PermissionTarget permissionTarget);

    void deleteAllAcls();

    Acl updateAcl(Acl acl) throws NotFoundException;

    Acl createAcl(PermissionTarget permissionTarget) throws AlreadyExistsException;

    List<PermissionTarget> getAllPermissionTargets();

    /**
     * @param permissionTarget The permission target which is the Acl id.
     * @return An Acl by its id - the permission target. Null if not found
     */
    Acl findAclById(PermissionTarget permissionTarget);

    void createAnythingPermision(SimpleUser anonUser);

    void removeAllUserAces(String username);
}