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

package org.artifactory.security;

import org.artifactory.api.repo.RepoPath;
import org.artifactory.log.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.security.Authentication;

/**
 * Created by IntelliJ IDEA. User: yoav
 */
public abstract class AccessLogger {
    private static final Logger log = LoggerFactory.getLogger(AccessLogger.class);

    public enum Action {
        DOWNLOAD, DEPLOY, DELETE, SEARCH, LOGIN
    }

    public static void downloaded(RepoPath repoPath) {
        downloaded(repoPath, false, AuthenticationHelper.getAuthentication());
    }

    public static void downloadDenied(RepoPath repoPath) {
        downloaded(repoPath, true, AuthenticationHelper.getAuthentication());
    }

    public static void downloaded(RepoPath repoPath, boolean denied, Authentication authentication) {
        logAction(repoPath, Action.DOWNLOAD, denied, authentication);
    }

    public static void deployed(RepoPath repoPath) {
        deployed(repoPath, false, AuthenticationHelper.getAuthentication());
    }

    public static void deployDenied(RepoPath repoPath) {
        deployed(repoPath, true, AuthenticationHelper.getAuthentication());
    }

    public static void deployed(RepoPath repoPath, boolean denied, Authentication authentication) {
        logAction(repoPath, Action.DEPLOY, denied, authentication);
    }

    public static void deleted(RepoPath repoPath) {
        deleted(repoPath, false, AuthenticationHelper.getAuthentication());
    }

    public static void deleteDenied(RepoPath repoPath) {
        deleted(repoPath, true, AuthenticationHelper.getAuthentication());
    }

    public static void deleted(RepoPath repoPath, boolean denied, Authentication authentication) {
        logAction(repoPath, Action.DELETE, denied, authentication);
    }

    public static void unauthorizedSearch() {
        logAction(null, Action.SEARCH, true, AuthenticationHelper.getAuthentication());
    }

    public static void loggedIn(Authentication authentication) {
        logAction(null, Action.LOGIN, false, authentication);
    }

    public static void loginDenied(Authentication authentication) {
        logAction(null, Action.LOGIN, true, authentication);
    }

    public static void logAction(
            RepoPath repoPath, Action action, boolean denied, Authentication authentication) {
        if (authentication != null) {
            String address = AuthenticationHelper.getRemoteAddress(authentication);
            log.info(
                    (denied ? "[DENIED " : "[ACCEPTED ") + action.name() + "] " + (repoPath != null ? repoPath : "") +
                            " for " + authentication.getName() + (address != null ? "/" + address : "") + ".");
        }
    }
}
