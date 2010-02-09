/*
 * This file is part of Artifactory.
 *
 * Artifactory is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Artifactory is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Artifactory.  If not, see <http://www.gnu.org/licenses/>.
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
     * @return True if the current user is transient
     */
    boolean isTransientUser();

    /**
     * @return True if anonymous access is allowed.
     */
    boolean isAnonAccessEnabled();

    /**
     * @return True if the current user can read the specified path.
     */
    boolean canRead(RepoPath path);

    /**
     * @return True if the current user can read the specified path implicitly by having a read permissions on part of
     *         the path
     */
    boolean canImplicitlyReadParentPath(RepoPath repoPath);

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
     * Indicates if the current user has the given permission, no matter the target
     *
     * @param artifactoryPermission Permission to check
     * @return True if the current user has such permission. False if not
     */
    boolean hasPermission(ArtifactoryPermission artifactoryPermission);

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
     * @return True if the current is a system administrator.
     */
    boolean isAdmin();

    /**
     * @return True if the current user is a anonymous.
     */
    boolean isAnonymous();

    /**
     * @return The current logged in username.
     */
    String currentUsername();

    boolean isAuthenticated();
}
