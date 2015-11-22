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

package org.artifactory.support.config.analysis;

import org.artifactory.support.config.CollectConfiguration;
import org.artifactory.support.core.exceptions.BundleConfigurationException;

/**
 * Thread dump collection configuration
 *
 * @author Michael Pasternak
 */
public class ThreadDumpConfiguration extends CollectConfiguration {

    private int count;
    private long interval;

    /**
     * Default .ctr
     */
    public ThreadDumpConfiguration() {
        // we enable all collectors by default (see in #RTFACT-8106)
        setDefaultConfig();
    }

    /**
     * @param enabled is collection enabled
     */
    public ThreadDumpConfiguration(boolean enabled) {
        super(enabled);
        setDefaultConfig();
    }

    /**
     * @param enabled is collection enabled
     * @param count amount of dumps to take
     * @param interval interval between dumps (millis)
     */
    public ThreadDumpConfiguration(boolean enabled, int count, long interval) {
        super(enabled);
        this.count = count;
        this.interval = interval;
    }

    /**
     * @return dumps count
     */
    public int getCount() {
        return count;
    }

    /**
     * @return interval between dumps (millis)
     */
    public long getInterval() {
        return interval;
    }

    /**
     * Sets default configuration on user config absence
     */
    private void setDefaultConfig() {
        count=1;
        interval=0;
    }
}
