/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.core.state;

import org.apache.jackrabbit.core.ItemId;
import org.apache.jackrabbit.core.NodeId;
import org.apache.jackrabbit.core.PropertyId;
import org.apache.jackrabbit.spi.Name;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;

/**
 * Internal utility class used for merging concurrent changes that occurred on different <code>NodeState</code>
 * instances representing the same node.
 * <p/>
 * See http://issues.apache.org/jira/browse/JCR-584.
 */
class NodeStateMerger {

    private static final Logger log = LoggerFactory.getLogger(NodeStateMerger.class);


    /**
     * Tries to silently merge the given <code>state</code> with its externally (e.g. through another session) modified
     * overlayed state in order to avoid an <code>InvalidItemStateException</code>.
     * <p/>
     * See http://issues.apache.org/jira/browse/JCR-584.
     *
     * @param state   node state whose modified overlayed state should be merged
     * @param context used for analyzing the context of the modifications
     * @return true if the changes could be successfully merged into the given node state; false otherwise
     */
    static boolean merge(NodeState state, MergeContext context) {

        //If we have no overlay to marge against, we may be after org.apache.jackrabbit.core.ItemImpl#save that
        //persisted and disposed transient states, but before SessionItemStateManager.disposeTransientItemState removed
        //the state from the sessions transient map - so we handle a merge event.
        //In this case, if the state's status matches the expected status, merge against the transient state, to verify
        //that merging does not conflict (no actual state will be persisted).
        NodeState overlayedState = (NodeState) state.getOverlayedState();
        int status = state.getStatus();
        boolean overlayMissingOrSynced = false;
        if (overlayedState == null) {
            log.debug("Already disposed overlay merging required: overlayedState={} ({}) status={}.",
                    new Object[]{overlayedState, new Short(state.getModCount()), new Integer(status)});
            overlayMissingOrSynced = true;
        } else if (state.getModCount() == overlayedState.getModCount()) {
            log.debug("Mod count merging required: overlayedState={} ({}) status={}.",
                    new Object[]{overlayedState, new Short(state.getModCount()), new Integer(status)});
            overlayMissingOrSynced = true;
        }
        if (overlayMissingOrSynced) {
            switch (status) {
                case ItemState.STATUS_UNDEFINED:
                case ItemState.STATUS_NEW:
                case ItemState.STATUS_EXISTING_MODIFIED:
                    overlayedState = state;
                    break;
                default:
                    log.debug("Cannot merge - overlay conflict: overlayedState={} ({}) status={}.",
                            new Object[]{overlayedState, new Short(state.getModCount()), new Integer(status)});
                    return false;
            }
        }

        synchronized (overlayedState) {
            synchronized (state) {
                /**
                 * some examples for trivial non-conflicting changes:
                 * - s1 added child node a, s2 removes child node b
                 * - s1 adds child node a, s2 adds child node b
                 * - s1 adds child node a, s2 adds mixin type
                 *
                 * conflicting changes causing staleness:
                 * - s1 added non-sns child node or property a,
                 *   s2 added non-sns child node or property a => name clash
                 * - either session reordered child nodes
                 *   (some combinations could possibly be merged)
                 * - either session moved node
                 */

                // compare current transient state with externally modified
                // overlayed state and determine what has been changed by whom

                // child node entries order
                if (!state.getReorderedChildNodeEntries().isEmpty()) {
                    // for now we don't even try to merge the result of
                    // a reorder operation
                    return false;
                }

                // mixin types
                if (!state.getMixinTypeNames().equals(overlayedState.getMixinTypeNames())) {
                    // the mixins have been modified but by just looking at the diff we
                    // can't determine where the change happened since the diffs of either
                    // removing a mixin from the overlayed or adding a mixin to the
                    // transient state would look identical...
                    return false;
                }

                // parent id
                if (state.getParentId() != null
                        && !state.getParentId().equals(overlayedState.getParentId())) {
                    return false;
                }

                // child node entries
                if (!state.getChildNodeEntries().equals(
                        overlayedState.getChildNodeEntries())) {
                    ArrayList added = new ArrayList();
                    ArrayList removed = new ArrayList();

                    for (Iterator iter = state.getAddedChildNodeEntries().iterator();
                         iter.hasNext();) {
                        ChildNodeEntry cne =
                                (ChildNodeEntry) iter.next();

                        if (context.isAdded(cne.getId()) || context.isModified(cne.getId())) {
                            // a new child node entry has been added to this state;
                            // check for name collisions with other state
                            if (overlayedState.hasChildNodeEntry(cne.getName())) {
                                // conflicting names
                                if (cne.getIndex() < 2) {
                                    // check if same-name siblings are allowed
                                    if (!context.allowsSameNameSiblings(cne.getId())) {
                                        return false;
                                    }
                                }
                                // assume same-name siblings are allowed since index is >= 2
                            }

                            added.add(cne);
                        } else {
                            // externally added
                        }
                    }

                    for (Iterator iter = state.getRemovedChildNodeEntries().iterator();
                         iter.hasNext();) {
                        ChildNodeEntry cne =
                                (ChildNodeEntry) iter.next();
                        if (context.isDeleted(cne.getId()) || context.isModified(cne.getId())) {
                            // a child node entry has been removed from this node state
                            removed.add(cne);
                        } else {
                            // externally removed
                        }
                    }

                    // copy child node entries from other state and
                    // re-apply changes made on this state
                    state.setChildNodeEntries(overlayedState.getChildNodeEntries());
                    for (Iterator iter = added.iterator(); iter.hasNext();) {
                        ChildNodeEntry cne =
                                (ChildNodeEntry) iter.next();
                        state.addChildNodeEntry(cne.getName(), cne.getId());
                    }
                    for (Iterator iter = removed.iterator(); iter.hasNext();) {
                        ChildNodeEntry cne =
                                (ChildNodeEntry) iter.next();
                        state.removeChildNodeEntry(cne.getId());
                    }
                }

                // property names
                if (!state.getPropertyNames().equals(
                        overlayedState.getPropertyNames())) {
                    HashSet added = new HashSet();
                    HashSet removed = new HashSet();

                    for (Iterator iter = state.getAddedPropertyNames().iterator();
                         iter.hasNext();) {
                        Name name = (Name) iter.next();
                        PropertyId propId =
                                new PropertyId(state.getNodeId(), name);
                        if (context.isAdded(propId)) {
                            added.add(name);
                        }
                    }
                    for (Iterator iter = state.getRemovedPropertyNames().iterator();
                         iter.hasNext();) {
                        Name name = (Name) iter.next();
                        PropertyId propId =
                                new PropertyId(state.getNodeId(), name);
                        if (context.isDeleted(propId)) {
                            // a property name has been removed from this state
                            removed.add(name);
                        }
                    }

                    // copy property names from other and
                    // re-apply changes made on this state
                    state.setPropertyNames(overlayedState.getPropertyNames());
                    for (Iterator iter = added.iterator(); iter.hasNext();) {
                        Name name = (Name) iter.next();
                        state.addPropertyName(name);
                    }
                    for (Iterator iter = removed.iterator(); iter.hasNext();) {
                        Name name = (Name) iter.next();
                        state.removePropertyName(name);
                    }
                }

                // finally sync modification count
                state.setModCount(overlayedState.getModCount());

                return true;
            }
        }
    }

    //-----------------------------------------------------< inner interfaces >

    static interface MergeContext {
        boolean isAdded(ItemId id);

        boolean isDeleted(ItemId id);

        boolean isModified(ItemId id);

        boolean allowsSameNameSiblings(NodeId id);
    }
}
