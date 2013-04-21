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

import org.artifactory.factory.InfoFactoryHolder;
import org.artifactory.mime.MavenNaming;
import org.artifactory.repo.RepoPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;

/**
 * @author Yoav Landman
 */
public abstract class AccessLogger {
    private static final Logger log = LoggerFactory.getLogger(AccessLogger.class);

    public enum Action {
        ANNOTATE, ANNOTATE_DELETE, DOWNLOAD, DEPLOY, DELETE, SEARCH, LOGIN, CONFIGURATION_CHANGE, CREATE, UPDATE, APPROVE, DISAPPROVE
    }

    private AccessLogger() {
        // utility class
    }

    public static void annotated(RepoPath repoPath, String metadataName) {
        if (!MavenNaming.MAVEN_METADATA_NAME.equals(metadataName)) {
            // don't log maven metadata since it is mostly internal
            RepoPath metadataPath = InfoFactoryHolder.get()
                    .createRepoPath(repoPath.getRepoKey(), repoPath.getPath() + ":" + metadataName);
            annotated(metadataPath);
        }
    }

    public static void annotated(RepoPath repoPath) {
        annotated(repoPath, false, AuthenticationHelper.getAuthentication());
    }

    public static void annotationDeleted(RepoPath repoPath, String metadataName) {
        RepoPath metadataPath = InfoFactoryHolder.get()
                .createRepoPath(repoPath.getRepoKey(), repoPath.getPath() + ":" + metadataName);
        logAction(metadataPath, Action.ANNOTATE_DELETE, false, AuthenticationHelper.getAuthentication(), null);
    }

    public static void annotateDenied(RepoPath repoPath) {
        annotated(repoPath, true, AuthenticationHelper.getAuthentication());
    }

    public static void annotated(RepoPath repoPath, boolean denied, Authentication authentication) {
        logAction(repoPath, Action.ANNOTATE, denied, authentication);
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

    public static void deleted(String message) {
        deleted(null, false, AuthenticationHelper.getAuthentication(), message);
    }

    public static void deleted(RepoPath repoPath) {
        deleted(repoPath, false, AuthenticationHelper.getAuthentication(), null);
    }

    public static void deleteDenied(RepoPath repoPath) {
        deleted(repoPath, true, AuthenticationHelper.getAuthentication(), null);
    }

    public static void deleted(RepoPath repoPath, boolean denied, Authentication authentication, String message) {
        logAction(repoPath, Action.DELETE, denied, authentication, message);
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

    public static void configurationChanged() {
        logAction(null, Action.CONFIGURATION_CHANGE, false, AuthenticationHelper.getAuthentication());
    }

    public static void created(String message) {
        logAction(null, Action.CREATE, false, AuthenticationHelper.getAuthentication(), message);
    }

    public static void updated(String message) {
        logAction(null, Action.UPDATE, false, AuthenticationHelper.getAuthentication(), message);
    }

    public static void approved(String message) {
        logAction(null, Action.APPROVE, false, AuthenticationHelper.getAuthentication(), message);
    }

    public static void disapproved(String message) {
        logAction(null, Action.DISAPPROVE, false, AuthenticationHelper.getAuthentication(), message);
    }

    public static void logAction(RepoPath repoPath, Action action, boolean denied, Authentication authentication) {
        logAction(repoPath, action, denied, authentication, null);
    }

    public static void logAction(RepoPath repoPath, Action action, boolean denied, Authentication authentication,
            String message) {
        if (authentication != null) {
            String address = AuthenticationHelper.getRemoteAddress(authentication);
            log.info(
                    (denied ? "[DENIED " : "[ACCEPTED ") + action.name() + "] " + (repoPath != null ? repoPath : "") +
                            (message != null ? message : "") + " for " + authentication.getName() + (address != null ?
                            "/" + address : "") + ".");
        }
    }
}
