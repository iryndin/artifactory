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

package org.artifactory.support.config.security;

import org.artifactory.support.config.CollectConfiguration;

import java.util.Optional;

/**
 * SecurityInfo collection configuration
 *
 * @author Michael Pasternak
 */
public class SecurityInfoConfiguration extends CollectConfiguration {

    private boolean hideUserDetails = true;

    /**
     * serialization .ctr
     */
    public SecurityInfoConfiguration() {
    }

    public SecurityInfoConfiguration(boolean enabled, Optional<Boolean> hideUserDetails) {
        super(enabled);
        if (hideUserDetails.isPresent())
            this.hideUserDetails = hideUserDetails.get();
    }

    public boolean isHideUserDetails() {
        return hideUserDetails;
    }
}
