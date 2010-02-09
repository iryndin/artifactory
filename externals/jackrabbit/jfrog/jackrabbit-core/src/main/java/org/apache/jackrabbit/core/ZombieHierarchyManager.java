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
package org.apache.jackrabbit.core;

import org.apache.jackrabbit.core.state.ItemState;
import org.apache.jackrabbit.core.state.ItemStateException;
import org.apache.jackrabbit.core.state.ItemStateManager;
import org.apache.jackrabbit.core.state.NoSuchItemStateException;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.core.state.ChildNodeEntry;
import org.apache.jackrabbit.spi.Name;

import java.util.Iterator;

/**
 * <code>HierarchyManager</code> implementation that is also able to
 * build/resolve paths of those items that have been moved or removed
 * (i.e. moved to the attic).
 * <p/>
 * todo make use of path caching
 */
public class ZombieHierarchyManager extends HierarchyManagerImpl {

    /**
     * the attic
     */
    protected ItemStateManager attic;

    public ZombieHierarchyManager(HierarchyManagerImpl parent,
                                  ItemStateManager provider,
                                  ItemStateManager attic) {
        super(parent.getRootNodeId(), provider);
        this.attic = attic;
    }

    /**
     * {@inheritDoc}
     * <p/>
     * Delivers state from attic if such exists, otherwise calls base class.
     */
    protected ItemState getItemState(ItemId id)
            throws NoSuchItemStateException, ItemStateException {
        // always check attic first
        if (attic.hasItemState(id)) {
            return attic.getItemState(id);
        }
        // delegate to base class
        return super.getItemState(id);
    }

    /**
     * {@inheritDoc}
     * <p/>
     * Returns <code>true</code>  if there's state on the attic for the
     * requested item; otherwise delegates to base class.
     */
    protected boolean hasItemState(ItemId id) {
        // always check attic first
        if (attic.hasItemState(id)) {
            return true;
        }
        // delegate to base class
        return super.hasItemState(id);
    }

    /**
     * {@inheritDoc}
     * <p/>
     * Also allows for removed items.
     */
    protected NodeId getParentId(ItemState state) {
        if (state.hasOverlayedState()) {
            // use 'old' parent in case item has been removed
            return state.getOverlayedState().getParentId();
        }
        // delegate to base class
        return super.getParentId(state);
    }

    /**
     * {@inheritDoc}
     * <p/>
     * Also allows for removed/renamed child node entries.
     */
    protected ChildNodeEntry getChildNodeEntry(NodeState parent,
                                                         Name name,
                                                         int index) {
        // check removed child node entries first
        Iterator iter = parent.getRemovedChildNodeEntries().iterator();
        while (iter.hasNext()) {
            ChildNodeEntry entry =
                    (ChildNodeEntry) iter.next();
            if (entry.getName().equals(name)
                    && entry.getIndex() == index) {
                return entry;
            }
        }
        // no matching removed child node entry found in parent,
        // delegate to base class
        return super.getChildNodeEntry(parent, name, index);
    }

    /**
     * {@inheritDoc}
     * <p/>
     * Also allows for removed child node entries.
     */
    protected ChildNodeEntry getChildNodeEntry(NodeState parent,
                                                         NodeId id) {
        // check removed child node entries first
        Iterator iter = parent.getRemovedChildNodeEntries().iterator();
        while (iter.hasNext()) {
            ChildNodeEntry entry =
                    (ChildNodeEntry) iter.next();
            if (entry.getId().equals(id)) {
                return entry;
            }
        }
        // no matching removed child node entry found in parent,
        // delegate to base class
        return super.getChildNodeEntry(parent, id);
    }
}
