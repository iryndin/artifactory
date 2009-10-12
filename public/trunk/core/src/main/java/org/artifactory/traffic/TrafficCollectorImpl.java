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

package org.artifactory.traffic;

import org.artifactory.log.LoggerFactory;
import org.artifactory.traffic.entry.TrafficEntry;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * A basic traffic events collector
 *
 * @author yoavl
 */
public class TrafficCollectorImpl implements TrafficCollector {

    private static final Logger log = LoggerFactory.getLogger(TrafficCollectorImpl.class);


    private final TrafficCollectorResolution resolution;
    private final Queue<TrafficCollectorListener> listeners;
    private final int[] fieldsToElapse;
    private final Calendar collectorNextCollection;

    public TrafficCollectorImpl(TrafficCollectorResolution resolution, int... fieldsToElapse) {
        this.resolution = resolution;
        this.listeners = new ConcurrentLinkedQueue<TrafficCollectorListener>();
        this.fieldsToElapse = fieldsToElapse;
        //Initialize next collection to now + interval
        collectorNextCollection = Calendar.getInstance();
        calcNextCollection(collectorNextCollection);
    }

    public String getName() {
        return resolution.getName();
    }

    public TrafficCollectorResolution getResolution() {
        return resolution;
    }

    public void addListener(TrafficCollectorListener listener) {
        listeners.add(listener);
    }

    public void removeListener(TrafficCollectorListener listener) {
        listeners.remove(listener);
    }

    public void collect(InternalTrafficService trafficService, Calendar entriesCollection) {
        Calendar collectorLastCollected = trafficService.getLastCollected(resolution);
        if (collectorLastCollected == null) {
            //No last collected
            log.trace("#### {}: Never collected before.", resolution.getName());
        }

        //Base the next collection time on the last collection time (if one exits) + collection interval
        if (collectorLastCollected != null) {
            calcNextCollection(collectorLastCollected);
        }

        log.trace("#### {}: collectorLastCollected: {}, collectorNextCollection: {}",
                new Object[]{resolution.getName(), calendarToDateString(collectorLastCollected),
                        collectorNextCollection.getTime()});

        //Check if it's time to collect (we are after the collector's next collection time)
        if (collectorNextCollection.before(entriesCollection)) {
            //Select all entries that are part of our time window - from our last collected to our next collection
            //(exclusive)
            final TrafficEntriesIterator entriesIterator =
                    trafficService.getDatabaseEntries(collectorLastCollected, collectorNextCollection);
            log.trace("#### {}: Collecting {} from {} to {}",
                    new Object[]{resolution.getName(), entriesIterator.size(),
                            calendarToDateString(collectorLastCollected), collectorNextCollection.getTime()});
            //Send the collection event to listeners
            fireCollectionEvent(trafficService, entriesIterator);
            //Update the collector's last collected
            trafficService.updateLastCollected(resolution, collectorNextCollection);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TrafficCollectorImpl)) {
            return false;
        }
        TrafficCollectorImpl collector = (TrafficCollectorImpl) o;
        return resolution == collector.resolution;
    }

    @Override
    public int hashCode() {
        return resolution.hashCode();
    }

    void elapseColletionFields(Calendar collection) {
        for (int field : fieldsToElapse) {
            collection.clear(field);
        }
    }

    private void calcNextCollection(Calendar lastCollected) {
        collectorNextCollection.setTime(lastCollected.getTime());
        //Remove irrelevant fields (sec, mins etc.)
        elapseColletionFields(collectorNextCollection);
        //Add the resolution to get the next collection time
        collectorNextCollection.add(Calendar.SECOND, resolution.getSecs());
    }

    private void fireCollectionEvent(InternalTrafficService service, TrafficEntriesIterator entriesIterator) {
        List<TrafficEntry> collectedEntries = new ArrayList<TrafficEntry>((int) entriesIterator.size());
        while (entriesIterator.hasNext()) {
            collectedEntries.add(entriesIterator.next());
        }
        final TrafficCollectionEvent trafficCollectionEvent = new TrafficCollectionEvent(collectedEntries);
        for (TrafficCollectorListener listener : listeners) {
            //Notify asynchronously
            service.notifyCollectionListener(listener, trafficCollectionEvent);
        }
    }

    private String calendarToDateString(Calendar collectorLastCollected) {
        return collectorLastCollected != null ? "" + collectorLastCollected.getTime() : "any";
    }
}