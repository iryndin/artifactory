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
 *
 * Based on: /org/apache/jackrabbit/jackrabbit-core/1.6.0/jackrabbit-core-1.6.0.jar!
 * /org/apache/jackrabbit/core/query/OnWorkspaceInconsistency.class
 */

package org.artifactory.jcr.jackrabbit.query;

import org.apache.jackrabbit.core.query.OnWorkspaceInconsistency;
import org.apache.jackrabbit.core.query.QueryHandler;
import org.apache.jackrabbit.core.state.ChildNodeEntry;
import org.apache.jackrabbit.core.state.ItemStateException;
import org.apache.jackrabbit.core.state.NoSuchItemStateException;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.commons.conversion.DefaultNamePathResolver;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.artifactory.log.LoggerFactory;
import org.slf4j.Logger;

import javax.jcr.RepositoryException;

/**
 * @author Yoav Landman
 */
public class LenientOnWorkspaceInconsistency extends OnWorkspaceInconsistency {
    private static final Logger log = LoggerFactory.getLogger(LenientOnWorkspaceInconsistency.class);

    public static final OnWorkspaceInconsistency LENIENT = new LenientOnWorkspaceInconsistency("lenient");

    public static void init() {
        //noinspection unchecked
        OnWorkspaceInconsistency.INSTANCES.put(LENIENT.getName(), LENIENT);
    }

    /**
     * Protected constructor.
     */
    protected LenientOnWorkspaceInconsistency(String name) {
        super(name);
    }

    @Override
    public void handleMissingChildNode(NoSuchItemStateException exception,
            QueryHandler handler,
            Path path,
            NodeState node,
            ChildNodeEntry child)
            throws RepositoryException, ItemStateException {
        NamePathResolver resolver = new DefaultNamePathResolver(
                handler.getContext().getNamespaceRegistry());
        log.error("Node {} ({}) has missing child '{}' ({})",
                new Object[]{
                        resolver.getJCRPath(path),
                        node.getNodeId().toString(),
                        resolver.getJCRName(child.getName()),
                        child.getId().toString()
                });
        //throw exception;
    }
}