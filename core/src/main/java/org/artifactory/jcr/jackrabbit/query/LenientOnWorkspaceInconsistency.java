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
    @SuppressWarnings({"UnusedDeclaration"})
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
                        node.getNodeId().getUUID().toString(),
                        resolver.getJCRName(child.getName()),
                        child.getId().getUUID().toString()
                });
        //throw exception;
    }
}