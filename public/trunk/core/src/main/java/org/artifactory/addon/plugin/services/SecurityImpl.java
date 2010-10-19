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

package org.artifactory.addon.plugin.services;

import org.artifactory.repo.RepoPath;
import org.artifactory.security.InternalSecurityService;
import org.artifactory.security.Security;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

@Component
public class SecurityImpl implements Security {

    @Inject
    private InternalSecurityService securityService;

    public boolean isUpdatableProfile() {
        return securityService.isUpdatableProfile();
    }

    public boolean isAnonAccessEnabled() {
        return securityService.isAnonAccessEnabled();
    }

    public boolean canRead(RepoPath path) {
        return securityService.canRead(path);
    }

    public boolean canImplicitlyReadParentPath(RepoPath repoPath) {
        return securityService.canImplicitlyReadParentPath(repoPath);
    }

    public boolean canAnnotate(RepoPath repoPath) {
        return securityService.canAnnotate(repoPath);
    }

    public boolean canDelete(RepoPath path) {
        return securityService.canDelete(path);
    }

    public boolean canDeploy(RepoPath path) {
        return securityService.canDeploy(path);
    }

    public boolean canAdmin(RepoPath path) {
        return securityService.canAdmin(path);
    }

    public String currentUsername() {
        return securityService.currentUsername();
    }

    public boolean isAdmin() {
        return securityService.isAdmin();
    }

    public boolean isAnonymous() {
        return securityService.isAnonymous();
    }

    public boolean isAuthenticated() {
        return securityService.isAuthenticated();
    }
}