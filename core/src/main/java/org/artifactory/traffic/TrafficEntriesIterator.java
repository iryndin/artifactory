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

package org.artifactory.traffic;

import org.artifactory.api.repo.exception.RepositoryRuntimeException;
import org.artifactory.jcr.JcrTypes;
import org.artifactory.log.LoggerFactory;
import org.artifactory.traffic.entry.TokenizedTrafficEntryFactory;
import org.artifactory.traffic.entry.TrafficEntry;
import org.slf4j.Logger;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import java.util.Iterator;

/**
 * @author yoavl
 */
public class TrafficEntriesIterator implements Iterator<TrafficEntry> {
    private static final Logger log = LoggerFactory.getLogger(TrafficEntriesIterator.class);

    private NodeIterator nodeIterator;
    private Node currentNode;

    public TrafficEntriesIterator(NodeIterator nodeIterator) {
        this.nodeIterator = nodeIterator;
        /*final long size = nodeIterator.getSize();
        if (size > 0) {
            log.trace("Found {} traffic entries to process.", size);
        }*/
    }

    public boolean hasNext() {
        return nodeIterator.hasNext();
    }

    public TrafficEntry next() {
        currentNode = nodeIterator.nextNode();
        final Property trafficEntryProperty;
        try {
            trafficEntryProperty = currentNode.getProperty(JcrTypes.PROP_ARTIFACTORY_TRAFFIC_ENTRY);

            final String trafficEntryString = trafficEntryProperty.getString();
            TrafficEntry trafficEntry = TokenizedTrafficEntryFactory.newTrafficEntry(trafficEntryString);
            return trafficEntry;
        } catch (RepositoryException e) {
            throw new RepositoryRuntimeException("Could not read traffic entry.", e);
        }
    }

    public void remove() {
        //Actually removes the entry
        if (currentNode == null) {
            throw new IllegalStateException("Cannot remove non-iteratable node.");
        }
        try {
            currentNode.remove();
        } catch (RepositoryException e) {
            throw new RepositoryRuntimeException("Could not remove traffic entry.", e);
        }
    }

    public long size() {
        return nodeIterator.getSize();
    }
}