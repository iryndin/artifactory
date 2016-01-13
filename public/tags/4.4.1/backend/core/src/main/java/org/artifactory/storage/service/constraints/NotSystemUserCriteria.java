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

package org.artifactory.storage.service.constraints;

import org.artifactory.api.security.SecurityService;
import org.springframework.stereotype.Component;

/**
 * @author Michael Pasternak
 */
@Component
public class NotSystemUserCriteria implements Criteria<String> {

    private static final int USER_SYSTEM_HASH_CODE = SecurityService.USER_SYSTEM.hashCode();

    /**
     * Calculates whether given constraint
     * answering desired criteria
     *
     * @param user to filter out
     *
     * @return true if content should be excluded,
     *         otherwise false
     */
    @Override
    public boolean meet(String user) {
        return !(
                user != null &&
                USER_SYSTEM_HASH_CODE == user.hashCode() &&
                SecurityService.USER_SYSTEM.equals(user)
        );
    }
}
