/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2010 JFrog Ltd.
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

import org.artifactory.api.repo.Lock;

import java.util.List;

/**
 * User: freds Date: Aug 5, 2008 Time: 8:46:40 PM
 */
public interface AclService {

    /**
     * Returns a list of permission targets for the type of permission given
     *
     * @param artifactoryPermission Type of permission to find
     * @return List of permission target info objects
     */
    List<PermissionTargetInfo> getPermissionTargets(ArtifactoryPermission artifactoryPermission);

    /**
     * @return Returns all the AclInfos
     */
    List<AclInfo> getAllAcls();

    /**
     * @param target The permission target to check.
     * @return True if the current logged in user has admin permissions on the permission target
     */
    boolean canAdmin(PermissionTargetInfo target);

    /**
     * @return True is the user or a group the user belongs to has read permissions on the target
     */
    boolean canRead(UserInfo user, PermissionTargetInfo target);

    /**
     * @return True is the user or a group the user belongs to has annotate permissions on the target
     */
    boolean canAnnotate(UserInfo user, PermissionTargetInfo target);

    /**
     * @return True is the user or a group the user belongs to has deploy permissions on the target
     */
    boolean canDeploy(UserInfo user, PermissionTargetInfo target);

    /**
     * @return True is the user or a group the user belongs to has delete permissions on the target
     */
    boolean canDelete(UserInfo user, PermissionTargetInfo target);

    /**
     * @return True is the user or a group the user belongs to has admin permissions on the target
     */
    boolean canAdmin(UserInfo user, PermissionTargetInfo target);

    @Lock(transactional = true)
    AclInfo createAcl(AclInfo entity);

    @Lock(transactional = true)
    void deleteAcl(PermissionTargetInfo target);

    AclInfo getAcl(PermissionTargetInfo permissionTarget);

    void updateAcl(AclInfo acl);
}
