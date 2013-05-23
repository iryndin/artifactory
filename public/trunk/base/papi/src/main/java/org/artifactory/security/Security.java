/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2012 JFrog Ltd.
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

package org.artifactory.security;

import org.artifactory.repo.RepoPath;

/**
 * These are the usage of security data and logged in user methods.
 */
public interface Security {

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
     * @return True if the current user can read the specified path implicitly by having a read permissions on part of
     *         the path
     * @deprecated Equivalent to canRead on a folder path - Will be removed in next major version
     */
    @Deprecated
    boolean canImplicitlyReadParentPath(RepoPath repoPath);

    /**
     * @return True if the current user can annotate the specified path.
     */
    boolean canAnnotate(RepoPath repoPath);

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
     * @deprecated Use {@link #canManage()} instead
     */
    @Deprecated
    boolean canAdmin(RepoPath path);

    /**
     * @return True if the current user has manage permissions on a target info that includes this path..
     */
    boolean canManage(RepoPath path);

    /**
     * @return The current logged-in user name.
     * @since 2.3.3
     */
    String getCurrentUsername();

    /**
     * The current logged in-user name.
     *
     * @return The current logged in-user name
     * @deprecated Use  {@link #getCurrentUsername()} instead
     */
    @Deprecated
    String currentUsername();


    /**
     * The group names for the current logged-in user.
     *
     * @return A list of group names associated with the current user.
     * @since 2.3.3
     */
    String[] getCurrentUserGroupNames();

    /**
     * @return True if the current is a system administrator.
     */
    boolean isAdmin();

    /**
     * @return True if the current user is a anonymous.
     */
    boolean isAnonymous();

    boolean isAuthenticated();

    /**
     * @return The encrypted password of the current user
     */
    String getEncryptedPassword();

    /**
     * @return The encrypted password of the current user properly escaped for inclusion in xml settings
     */
    String getEscapedEncryptedPassword();
}
