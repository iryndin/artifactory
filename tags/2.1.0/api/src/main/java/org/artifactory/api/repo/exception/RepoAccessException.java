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

package org.artifactory.api.repo.exception;

import org.artifactory.api.repo.RepoPath;

public class RepoAccessException extends Exception {
    private final String username;
    private final RepoPath repoPath;
    private final String action;

    public RepoAccessException(String message, RepoPath repoPath, String action, String username) {
        super(message);
        this.username = username;
        this.repoPath = repoPath;
        this.action = action;
    }

    public RepoAccessException(String message, RepoPath repoPath, String action, String username, Throwable cause) {
        super(message, cause);
        this.username = username;
        this.repoPath = repoPath;
        this.action = action;
    }

    @Override
    public String getMessage() {
        return super.getMessage() + " Action '" + action + "' is unauthorized for user '" + username + "' on '" +
                repoPath + "'.";
    }
}