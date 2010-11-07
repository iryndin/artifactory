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

package org.artifactory.security;

import org.artifactory.api.security.AclInfo;
import org.artifactory.api.security.AclManager;
import org.artifactory.spring.ReloadableBean;
import org.artifactory.util.AlreadyExistsException;
import org.springframework.security.acls.model.NotFoundException;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

/**
 * @author Yoav Landman
 */
public interface InternalAclManager extends ReloadableBean, AclManager {
    /**
     * Provide the whole list of ACLs in the security system
     *
     * @return a copy of the internal list, so can be modified.
     */
    @Transactional(readOnly = true)
    List<Acl> getAllAcls();

    @Transactional
    List<Acl> getAllAcls(ArtifactorySid[] sids);

    @Transactional
    Acl createAcl(Acl acl);

    void deleteAcl(PermissionTarget permissionTarget);

    void deleteAllAcls();

    @Transactional
    Acl updateAcl(Acl acl) throws NotFoundException;

    @Transactional
    Acl createAcl(AclInfo aclInfo) throws AlreadyExistsException;

    @Transactional
    List<PermissionTarget> getAllPermissionTargets();

    /**
     * @param permissionTarget The permission target which is the Acl id.
     * @return An Acl by its id - the permission target. Null if not found
     */
    @Transactional
    Acl findAclById(PermissionTarget permissionTarget);

    /**
     * Creates the default permissions for the anonymous user and the readers group.
     *
     * @param anonUser     The anonymous user object
     * @param readersGroup The readers group object
     */
    @Transactional
    void createDefaultSecurityEntities(SimpleUser anonUser, Group readersGroup);

    void removeAllUserAces(String username);

    /**
     * Reload and return all the configured ACLs
     *
     * @return List of reloaded ACLs
     */
    @Transactional
    Collection<Acl> reloadAndReturnAcls();
}