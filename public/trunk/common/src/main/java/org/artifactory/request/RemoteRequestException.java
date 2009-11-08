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

package org.artifactory.request;

import java.io.IOException;

/**
 * A custom exception that enables retaining an error status code recieved from a remote request exception
 *
 * @author Noam Y. Tenne
 */
public class RemoteRequestException extends IOException {

    private int remoteReturnCode;

    /**
     * @param message          Exception message
     * @param remoteReturnCode Remote returned HTTP status code
     */
    public RemoteRequestException(String message, int remoteReturnCode) {
        super(message);
        this.remoteReturnCode = remoteReturnCode;
    }

    /**
     * Returns the HTTP status code returned from the remote request
     *
     * @return Remote returned HTTP status code
     */
    public int getRemoteReturnCode() {
        return remoteReturnCode;
    }
}