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
package org.artifactory.api.security;

import org.artifactory.api.repo.RepoPath;

/**
 * These are the usage of security data and logged in user methods.
 */
public interface AuthorizationService {
    String ROLE_USER = "user";
    String ROLE_ADMIN = "admin";

    /**
     * @return True if the current user can update her profile.
     */
    boolean isUpdatableProfile();

    /**
     * @return True if anonymous access is allowed.
     */
    boolean isAnonAccessEnabled();

    /**
     * @return True if the current user can read the specified path.
     */
    boolean canRead(RepoPath path);

    /**
     * @return True if the current user can delete the specified path.
     */
    boolean canDelete(RepoPath path);

    /**
     * @return True if the current user can deploy to the specified path.
     */
    boolean canDeploy(RepoPath path);

    /**
     * @return True if the current user has admin permissions on a target info that includes this path..
     */
    boolean canAdmin(RepoPath path);

    /**
     * @return True if the current user can deploy to some path.
     */
    boolean hasDeployPermissions();

    /**
     * @return True if the user can read the specified path.
     */
    boolean canRead(UserInfo user, RepoPath path);

    /**
     * @return True if the user can delete the specified path.
     */
    boolean canDelete(UserInfo user, RepoPath path);

    /**
     * @return True if the user can deploy to the specified path.
     */
    boolean canDeploy(UserInfo user, RepoPath path);

    /**
     * @return True if the user can administer the specified path.
     */
    boolean canAdmin(UserInfo user, RepoPath path);

    /**
     * @return True if users in the group can read the specified path.
     */
    boolean canRead(GroupInfo group, RepoPath path);

    /**
     * @return True if users in the group can delete the specified path.
     */
    boolean canDelete(GroupInfo group, RepoPath path);

    /**
     * @return True if users in the group can deploy to the specified path.
     */
    boolean canDeploy(GroupInfo group, RepoPath path);

    /**
     * @return True if users in the group can administer the specified path.
     */
    boolean canAdmin(GroupInfo group, RepoPath path);

    /**
     * @return True if the current user can administer at least on permission target.
     */
    boolean canAdminPermissionTarget();


    /**
     * @return True if the current is a system administrator.
     */
    boolean isAdmin();

    /**
     * @return True if the current is a anonymous.
     */
    boolean isAnonymous();

    /**
     * @return The current logged in username.
     */
    String currentUsername();

    boolean isAuthenticated();
}
