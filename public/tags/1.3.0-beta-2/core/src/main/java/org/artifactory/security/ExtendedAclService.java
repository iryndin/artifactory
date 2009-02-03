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

import org.springframework.security.acls.AlreadyExistsException;
import org.springframework.security.acls.MutableAcl;
import org.springframework.security.acls.MutableAclService;
import org.springframework.security.acls.NotFoundException;
import org.springframework.security.acls.objectidentity.ObjectIdentity;
import org.springframework.security.acls.sid.Sid;

import java.util.List;
import java.util.Map;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public interface ExtendedAclService extends MutableAclService {
    RepoPathAcl createAcl(RepoPathAcl acl);

    List<SecuredRepoPath> getAllRepoPaths();

    List<RepoPathAcl> getAllAcls();

    RepoPathAcl updateAcl(RepoPathAcl acl) throws NotFoundException;

    RepoPathAcl createAcl(ObjectIdentity objectIdentity)
            throws AlreadyExistsException;

    RepoPathAcl updateAcl(MutableAcl acl) throws NotFoundException;

    RepoPathAcl readAclById(ObjectIdentity object) throws NotFoundException;

    RepoPathAcl readAclById(ObjectIdentity object, Sid[] sids) throws NotFoundException;

    Map<SecuredRepoPath, RepoPathAcl> readAclsById(ObjectIdentity[] objects)
            throws NotFoundException;

    Map<SecuredRepoPath, RepoPathAcl> readAclsById(ObjectIdentity[] objects, Sid[] sids)
            throws NotFoundException;
}