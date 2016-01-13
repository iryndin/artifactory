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

package org.artifactory.security.exceptions;

import org.springframework.security.authentication.LockedException;

/**
 * Thrown when user got locked due to incorrect login
 *
 * @author Michael Pasternak
 */
public class UserLockedException extends LockedException {

    public UserLockedException(String msg) {
        super(msg);
    }

    /**
     * Produces user error
     *
     * @param userName
     * @return {@link UserLockedException}
     */
    public static UserLockedException userLocked(String userName) {
        return new UserLockedException(getUserErrorMessage(userName));
    }

    /**
     * Produces session/request error
     *
     * @return {@link UserLockedException}
     */
    public static UserLockedException sessionLocked() {
        return new UserLockedException(getSessionErrorMessage());
    }

    /**
     * Produces context aware error message
     *
     * @param userName
     *
     * @return error message
     */
    private static String getUserErrorMessage(String userName) {
        return String.format("User %s is Locked.\nContact System Administrator to Unlock The Account.", userName);
    }

    /**
     * Produces context aware error message
     *
     * @return error message
     */
    private static String getSessionErrorMessage() {
        return String.format("This request is locked due to recurrent log on errors");
    }
}
