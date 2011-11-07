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

import org.artifactory.log.LoggerFactory;
import org.artifactory.sapi.data.MutableVfsNode;
import org.artifactory.sapi.data.VfsNode;
import org.artifactory.storage.StorageConstants;
import org.artifactory.traffic.entry.TokenizedTrafficEntryFactory;
import org.artifactory.traffic.entry.TrafficEntry;
import org.slf4j.Logger;

import java.util.Iterator;

/**
 * @author yoavl
 */
public class TrafficEntriesIterator implements Iterator<TrafficEntry> {
    private static final Logger log = LoggerFactory.getLogger(TrafficEntriesIterator.class);

    private final Iterator<VfsNode> nodeIterator;
    private final long resultSize;
    private VfsNode currentNode;

    public TrafficEntriesIterator(Iterable<VfsNode> nodeIterator, long size) {
        this.nodeIterator = nodeIterator.iterator();
        this.resultSize = size;
        if (size > 0) {
            log.trace("Found {} traffic entries to process.", size);
        }
    }

    public boolean hasNext() {
        return nodeIterator.hasNext();
    }

    public TrafficEntry next() {
        currentNode = nodeIterator.next();
        final String trafficEntryString = currentNode.getStringProperty(
                StorageConstants.PROP_ARTIFACTORY_TRAFFIC_ENTRY);
        TrafficEntry trafficEntry = TokenizedTrafficEntryFactory.newTrafficEntry(trafficEntryString);
        return trafficEntry;
    }

    public void remove() {
        //Actually removes the entry
        if (currentNode == null) {
            throw new IllegalStateException("Cannot remove non-iteratable node.");
        }
        ((MutableVfsNode) currentNode).delete();
    }

    public long size() {
        return resultSize;
    }
}