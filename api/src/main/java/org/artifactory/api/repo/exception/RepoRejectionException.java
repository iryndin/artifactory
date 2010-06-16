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

package org.artifactory.api.repo.exception;

/**
 * An exception thrown when a repo rejects the deployment of an artifact
 *
 * @author Noam Y. Tenne
 */
public class RepoRejectionException extends Exception {

    /**
     * Default constructor
     */
    protected RepoRejectionException() {
        super();
    }

    /**
     * Error message constructor
     *
     * @param message Error message
     */
    public RepoRejectionException(String message) {
        super(message);
    }

    /**
     * Cause constructor
     *
     * @param cause A thrown exception to nest
     */
    public RepoRejectionException(Throwable cause) {
        super(cause);
    }

    /**
     * Returns the HTTP error code associated with the thrown exception
     *
     * @return HTTP error code
     */
    public int getErrorCode() {
        //TODO: [by YS] the default should be 500?
        return 403;
    }
}