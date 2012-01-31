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

package org.artifactory.io.checksum;

import org.apache.jackrabbit.core.data.DataIdentifier;
import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.id.PropertyId;
import org.apache.jackrabbit.core.observation.EventImpl;
import org.apache.jackrabbit.core.observation.EventState;
import org.apache.jackrabbit.core.observation.SynchronousEventListener;
import org.apache.jackrabbit.core.state.ChildNodeEntry;
import org.apache.jackrabbit.core.state.ItemStateException;
import org.apache.jackrabbit.core.state.ItemStateManager;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.core.state.PropertyState;
import org.apache.jackrabbit.core.value.BLOBFileValue;
import org.apache.jackrabbit.core.value.InternalValue;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.commons.name.NameConstants;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.log.LoggerFactory;
import org.artifactory.util.PathUtils;
import org.slf4j.Logger;

import javax.jcr.Binary;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import java.util.Map;

/**
 * Listens to repository events to update the checksum paths
 *
 * @author Yoav Landman
 */
public class ChecksumPathsListener implements SynchronousEventListener {

    private static final Logger log = LoggerFactory.getLogger(ChecksumPathsListener.class);

    private ItemStateManager ism;
    private ChecksumPaths _cspaths;

    public ChecksumPathsListener(ItemStateManager ism) {
        this.ism = ism;
    }

    /**
     * When a property is added, store the itemId of jcr:content with the path of the jcr:content containing node. When
     * a property is removed pick indetify the entry by jcr:content itemId and mark the path removed. Sample path for
     * jcr:data binary prop - /trash/914545/1138912581/test-1.0.jar/jcr:content(nt:resource)@jcr:data
     * <p/>
     * Assumes that jcr:content is the only container used for jcr:data binary data.
     *
     * @param events Jcr tx change events
     */
    @Override
    public void onEvent(EventIterator events) {
        try {
            //Only commit the tx if it has been started here
            boolean started = getCspaths().txBegin();

            processEvents(events);

            if (started) {
                getCspaths().txEnd(true);
            }
        } catch (RuntimeException e) {
            getCspaths().txEnd(false);
            throw e;
        }
    }

    /**
     * Multiple listeners of different sessions may be called-back here from a session commit by
     * SharedItemStateManager$Update.end to update the state of affected not-yet-committed sessions. E.g. a tx may
     * include events about a property that has been added before it was committed by another committed tx to have the
     * latest commited state (if no conflicts).
     *
     * @param events
     */
    private void processEvents(EventIterator events) {
        while (events.hasNext()) {
            EventImpl ev = (EventImpl) events.nextEvent();
            try {
                log.trace("*{}* = {}", EventState.valueOf(ev.getType()), ev);
                //Filter-out non-local events
                if (!ev.getEventListenerSession().equals(ev.getEventSession())) {
                    log.trace("Skipping non-local event {} from {} (local is {}).",
                            new Object[]{ev, ev.getEventListenerSession(), ev.getEventSession()});
                    continue;
                }
                switch (ev.getType()) {
                    case Event.PROPERTY_CHANGED:
                        //Clear by the id of the jcr:content parent for the old val first (same id, duplicate entries)
                        //Remove by the id of the jcr:content parent
                        Name propertyName = ev.getQPath().getName();
                        if (isJcrDataProp(propertyName)) {
                            NodeId nodeId = ev.getParentId();
                            log.debug("Clearing checksumPath (prop_change) for jcr:content node id {}.", nodeId);
                            getCspaths().deleteChecksumPath(nodeId.toString());
                            //Continue to add the new value
                        } else {
                            break;
                        }
                    case Event.PROPERTY_ADDED:
                        ChecksumPathInfo cpInfo = createChecksumPathInfoFromBinaryAddedEvent(ev);
                        if (cpInfo != null) {
                            log.debug("Adding checksumPath (prop_add) for jcr:content node id {}.",
                                    cpInfo.getBinaryNodeId());
                            getCspaths().addChecksumPath(cpInfo);
                        }
                        break;
                    case Event.PROPERTY_REMOVED:
                        //Clear by the id of the jcr:content parent
                        propertyName = ev.getQPath().getName();
                        if (isJcrDataProp(propertyName)) {
                            NodeId nodeId = ev.getParentId();
                            log.debug("Clearing checksumPath (prop_remove) for jcr:content node id {}.", nodeId);
                            getCspaths().deleteChecksumPath(nodeId.toString());
                        }
                        break;
                    case Event.NODE_REMOVED:
                        //No reachable state for a removed node to work with so we need the jcr:content itemId.
                        if (Node.JCR_CONTENT.equals(ev.getQPath().getName().toString())) {
                            NodeId nodeId = ev.getChildId();
                            log.debug("Clearing checksumPath (node_remove) for jcr:content node id {}.", nodeId);
                            getCspaths().deleteChecksumPath(nodeId.toString());
                        }
                        break;
                    case Event.NODE_MOVED:
                        // if the node moved to
                        log.debug("Node moved event:" + ev);
                        Map<String, String> eventInfo = ev.getInfo();
                        String destination = eventInfo.get("destAbsPath");
                        //String source = eventInfo.get("srcAbsPath");
                        if (destination != null && !destination.startsWith("/trash")) {
                            ChecksumPathInfo checksumPathInfo = createChecksumPathInfoFromMoveEvent(ev);
                            if (checksumPathInfo != null) {
                                getCspaths().updateChecksumPath(checksumPathInfo);
                            }
                        }
                }
            } catch (Exception e) {
                throw new RuntimeException("Could not process item event.", e);
            }
        }
    }

    private ChecksumPathInfo createChecksumPathInfoFromBinaryAddedEvent(EventImpl ev) throws ItemStateException {
        try {
            Name propName = ev.getQPath().getName();
            if (isJcrDataProp(propName)) {
                NodeId nodeId = ev.getParentId();
                // event path ends with /jcr:content/jcr:data get grandparent path to the containing node path
                String path = PathUtils.getAncesstor(ev.getPath(), 2);
                return createChecksumPathInfo(ev, nodeId, path);
            }
            return null;
        } catch (Exception e) {
            throw new RuntimeException("Could not calculate ChecksumPath info for event: " + ev, e);
        }
    }

    private ChecksumPathInfo createChecksumPathInfoFromMoveEvent(EventImpl ev)
            throws ItemStateException, RepositoryException {
        try {
            NodeState itemState = (NodeState) ism.getItemState(ev.getChildId());
            ChildNodeEntry childNodeEntry = itemState.getChildNodeEntry(NameConstants.JCR_CONTENT, 1);
            if (childNodeEntry == null) {
                log.debug("Skipping move node with no content child node: {}", ev);
                return null;
            }
            return createChecksumPathInfo(ev, childNodeEntry.getId(), ev.getPath());
        } catch (ItemStateException e) {
            //Node or property may not exist because of an external change event - ignore it
            if (ev.isExternal()) {
                log.info("Skipping node - no longer available {}", ev);
                return null;
            } else {
                throw e;
            }
        }
    }

    private ChecksumPathInfo createChecksumPathInfo(EventImpl ev, NodeId binaryNodeId, String path)
            throws ItemStateException, RepositoryException {
        PropertyState propState;
        try {
            //Get the id of the node holding the jcr:data property (jcr:content)
            NodeState nodeState = (NodeState) ism.getItemState(binaryNodeId);
            if (nodeState == null) {
                return null;
            }
            PropertyId id = new PropertyId(nodeState.getNodeId(), NameConstants.JCR_DATA);
            propState = (PropertyState) ism.getItemState(id);
        } catch (ItemStateException ise) {
            //Node or property may not exist because of an external change event - ignore it
            if (ev.isExternal()) {
                log.info("Skipping node - no longer available {}", ev);
                return null;
            } else {
                throw ise;
            }
        }
        InternalValue value = propState.getValues()[0];
        Binary binary = value.getBinary();
        DataIdentifier identifier = ((BLOBFileValue) binary).getDataIdentifier();
        if (identifier == null) {
            //Can happen for memory blobs that are extracted from a bundle, in older versions
            return null;
        }
        String checksum = identifier.toString();
        long size = binary.getSize();
        String id = binaryNodeId.toString();
        log.trace("Calculating ChecksumPath for {} in parent node {}", propState.getName(), id);
        return new ChecksumPathInfo(path, checksum, size, id);
    }

    private boolean isJcrDataProp(Name propertyName) {
        return Property.JCR_DATA.equals(propertyName.toString());
    }

    private ChecksumPaths getCspaths() {
        if (_cspaths == null) {
            _cspaths = ContextHelper.get().beanForType(ChecksumPaths.class);
        }
        return _cspaths;
    }
}
