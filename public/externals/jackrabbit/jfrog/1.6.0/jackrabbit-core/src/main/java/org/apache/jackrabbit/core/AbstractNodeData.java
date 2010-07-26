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

import javax.jcr.nodetype.NodeDefinition;
import org.apache.jackrabbit.core.state.NodeState;

/**
 * Data object representing a node.
 */
public abstract class AbstractNodeData extends ItemData {

    /** Primary parent id of a shareable node. */
    private NodeId primaryParentId;

    /**
     * Create a new instance of this class.
     *
     * @param state node state
     * @param definition node definition
     */
    protected AbstractNodeData(NodeState state, NodeDefinition definition) {
        super(state, definition);

        if (state.isShareable()) {
            this.primaryParentId = state.getParentId();
        }
    }

    /**
     * Create a new instance of this class.
     *
     * @param id item id
     */
    protected AbstractNodeData(ItemId id) {
        super(id);
    }

    /**
     * Return the associated node state.
     *
     * @return node state
     */
    public NodeState getNodeState() {
        return (NodeState) getState();
    }

    /**
     * Return the associated node defintion.
     *
     * @return node definition
     */
    public NodeDefinition getNodeDefinition() {
        return (NodeDefinition) getDefinition();
    }

    /**
     * Sets the associated node defintion.
     *
     * @param definition new node definition
     */
    public void setNodeDefinition(NodeDefinition definition) {
        setDefinition(definition);
    }

    /**
     * Return the parent id of this node. Every shareable node in a shared set
     * has a different parent.
     *
     * @return parent id
     */
    public NodeId getParentId() {
        if (primaryParentId != null) {
            return primaryParentId;
        }
        return getState().getParentId();
    }

    /**
     * Return the primary parent id of this node. Every shareable node in a
     * shared set has a different primary parent. Returns <code>null</code>
     * for nodes that are not shareable.
     *
     * @return primary parent id or <code>null</code>
     */
    public NodeId getPrimaryParentId() {
        return primaryParentId;
    }

    /**
     * Set the primary parent id of this node.
     *
     * @param primaryParentId primary parent id
     */
    protected void setPrimaryParentId(NodeId primaryParentId) {
        this.primaryParentId = primaryParentId;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isNode() {
        return true;
    }
}
