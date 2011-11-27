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
package org.apache.jackrabbit.core.persistence;

import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.state.ItemStateException;

import javax.jcr.RepositoryException;

/**
 * The iterable persistence manager can return the list of node ids that are stored.
 * Possible applications are backup, migration (copying a workspace or repository),
 * and data store garbage collection.
 */
public interface IterablePersistenceManager extends PersistenceManager {

    /**
     * Get all node ids.
     * A typical application will call this method multiple times, where 'after'
     * is the last row read. The maxCount parameter defines the maximum number of
     * node ids returned, 0 meaning no limit. The order of the node ids is specific for the
     * given persistent manager. Items that are added concurrently may not be included.
     *
     * @param after the lower limit, or null for no limit.
     * @param maxCount the maximum number of node ids to return, or 0 for no limit.
     * @return an iterator of all bundles.
     * @throws ItemStateException if an error while loading occurs.
     * @throws RepositoryException if a repository exception occurs
     */
    Iterable<NodeId> getAllNodeIds(NodeId after, int maxCount)
            throws ItemStateException, RepositoryException;

}
