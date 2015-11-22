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

package org.artifactory.support.config;

/**
 * @author Michael Pasternak
 */
public class CollectConfiguration implements Configuration {

    // we enable all collectors by default (see in #RTFACT-8106)
    private boolean enabled = true;

    /**
     * serialization .ctr
     */
    public CollectConfiguration() {
    }

    public CollectConfiguration(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * @return whether given configuration is enabled
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * @return {@link CollectConfiguration}
     */
    @Override
    public CollectConfiguration get() {
        return this;
    }
}
