/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2011 JFrog Ltd.
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

package org.artifactory.traffic;

import org.artifactory.api.repo.Async;
import org.artifactory.jcr.JcrSession;
import org.artifactory.spring.ReloadableBean;

import java.util.Calendar;

/**
 * @author yoavl
 */
public interface InternalTrafficService extends ReloadableBean, TrafficService {

    /**
     * Collect new traffic entries since the last time collected.
     */
    void collect();

    /**
     * Remove old collected entries that have expired, according to the retention period.
     */
    void cleanup();

    /**
     * Get an iterator for traffic entries for the specified time window (edges inclusive)
     */
    TrafficEntriesIterator getDatabaseEntries(Calendar from, Calendar to, JcrSession session);

    Calendar getLastCollected(TrafficCollectorResolution resolution, JcrSession session);

    void updateLastCollected(TrafficCollectorResolution resolution, Calendar lastCollected, JcrSession session);

    void addTrafficCollectorListener(TrafficCollectorResolution resolution, TrafficCollectorListener listener);

    void removeTrafficCollectorListener(TrafficCollectorResolution resolution,
            TrafficCollectorListener listener);

    /**
     * Notify listeners about collection event asynchronously
     */
    @Async
    void notifyCollectionListener(TrafficCollectorListener listener, TrafficCollectionEvent event);
}