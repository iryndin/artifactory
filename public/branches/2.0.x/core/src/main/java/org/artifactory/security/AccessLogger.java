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

import org.artifactory.api.repo.RepoPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.Authentication;
import org.springframework.security.ui.WebAuthenticationDetails;

/**
 * Created by IntelliJ IDEA. User: yoav
 */
public abstract class AccessLogger {
    private static final Logger log = LoggerFactory.getLogger(AccessLogger.class);

    public enum RepoPathAction {
        DOWNLOAD, DEPLOY, DELETE, SEARCH
    }

    public static void downloaded(RepoPath repoPath) {
        downloaded(repoPath, false, SecurityServiceImpl.getAuthentication());
    }

    public static void downloadDenied(RepoPath repoPath) {
        downloaded(repoPath, true, SecurityServiceImpl.getAuthentication());
    }

    public static void downloaded(RepoPath repoPath, boolean denied,
            Authentication authentication) {
        logRepoPathAction(repoPath, RepoPathAction.DOWNLOAD, denied, authentication);
    }

    public static void deployed(RepoPath repoPath) {
        deployed(repoPath, false, SecurityServiceImpl.getAuthentication());
    }

    public static void deployDenied(RepoPath repoPath) {
        deployed(repoPath, true, SecurityServiceImpl.getAuthentication());
    }

    public static void deployed(RepoPath repoPath, boolean denied, Authentication authentication) {
        logRepoPathAction(repoPath, RepoPathAction.DEPLOY, denied, authentication);
    }

    public static void deleted(RepoPath repoPath) {
        deleted(repoPath, false, SecurityServiceImpl.getAuthentication());
    }

    public static void deleteDenied(RepoPath repoPath) {
        deleted(repoPath, true, SecurityServiceImpl.getAuthentication());
    }

    public static void deleted(RepoPath repoPath, boolean denied, Authentication authentication) {
        logRepoPathAction(repoPath, RepoPathAction.DELETE, denied, authentication);
    }

    public static void unauthorizedSearch() {
        logRepoPathAction(null, RepoPathAction.SEARCH, true, SecurityServiceImpl.getAuthentication());
    }

    public static void logRepoPathAction(
            RepoPath repoPath, RepoPathAction action, boolean denied, Authentication authentication) {
        if (authentication != null) {
            Object details = authentication.getDetails();
            String address = null;
            if (details != null && details instanceof WebAuthenticationDetails) {
                address = ((WebAuthenticationDetails) details).getRemoteAddress();
            }
            log.info(
                    (denied ? "[DENIED " : "[ACCEPTED ") + action.name() + "] " + (repoPath != null ? repoPath : "") +
                            " for " + authentication.getName() + (address != null ? "/" + address : "") + ".");
        }
    }
}
