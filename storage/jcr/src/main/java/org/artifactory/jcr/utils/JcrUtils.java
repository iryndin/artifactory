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

package org.artifactory.jcr.utils;

import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.core.JcrCoreUtils;
import org.apache.jackrabbit.core.RepositoryImpl;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.data.DataIdentifier;
import org.apache.jackrabbit.core.data.DataStore;
import org.apache.jackrabbit.core.data.db.DbDataStore;
import org.apache.jackrabbit.core.data.db.ExtendedDbDataStoreWrapper;
import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.id.PropertyId;
import org.apache.jackrabbit.core.persistence.IterablePersistenceManager;
import org.apache.jackrabbit.core.state.NoSuchItemStateException;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.core.state.PropertyState;
import org.apache.jackrabbit.core.value.BLOBFileValue;
import org.apache.jackrabbit.core.value.InternalValue;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.commons.conversion.IllegalNameException;
import org.apache.jackrabbit.spi.commons.conversion.NameParser;
import org.artifactory.jcr.JcrService;
import org.artifactory.jcr.JcrSession;
import org.artifactory.jcr.jackrabbit.ArtifactoryDataStore;
import org.artifactory.jcr.jackrabbit.ExtendedDbDataStore;
import org.artifactory.jcr.jackrabbit.MissingOrInvalidDataStoreRecordException;
import org.artifactory.jcr.spring.StorageContextHelper;
import org.artifactory.log.LoggerFactory;
import org.artifactory.sapi.common.RepositoryRuntimeException;
import org.artifactory.schedule.TaskInterruptedException;
import org.artifactory.util.ExceptionUtils;
import org.slf4j.Logger;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import javax.jcr.Item;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Prints recursively a jcr node tree.
 *
 * @author Yossi Shaul
 */
public abstract class JcrUtils extends JcrCoreUtils {
    private static final Logger log = LoggerFactory.getLogger(JcrUtils.class);

    private JcrUtils() {
        // utility class
    }

    public static void preorder(Node node) {
        preorder(node, Collections.<String>emptyList());
    }

    public static void preorder(Node node, List<String> excludedNames) {
        log.info("********************************************");
        try {
            preorder(node, 0, excludedNames);
        } catch (RepositoryException e) {
            log.error("Error traversing tree: " + e.getMessage());
        }
        log.info("********************************************");
    }

    private static void preorder(Node node, int depth, List<String> excludedNames)
            throws RepositoryException {
        if (!excludedNames.contains(node.getName())) {
            visit(node, depth);

            // print child nodes
            NodeIterator nodeIterator = node.getNodes();
            while (nodeIterator.hasNext()) {
                preorder(nodeIterator.nextNode(), depth + 1, excludedNames);
            }
        } else {
            log.debug("Skipping excluded node name {}", node.getName());
        }
    }

    private static void visit(Node node, int depth) throws RepositoryException {
        StringBuilder sb = new StringBuilder();
        sb.append(indent(depth, node));
        sb.append(nodeToString(node));
        log.info(sb.toString());
    }

    public static String nodeToString(Node node) throws RepositoryException {
        StringBuilder sb = new StringBuilder();
        sb.append(node.getName()).append("[");
        sb.append(node.getPath()).append(",");

        PropertyIterator properties = node.getProperties();
        while (properties.hasNext()) {
            Property prop = properties.nextProperty();
            sb.append("@").append(prop.getName()).append("=");
            if (prop.getType() == PropertyType.BINARY) {
                sb.append("BINARY");
            } else {
                boolean multivalue = prop.getDefinition().isMultiple();
                if (!multivalue) {
                    sb.append(prop.getString());
                } else {
                    // multiple values
                    Value[] values = prop.getValues();
                    for (int i = 0; i < values.length; i++) {
                        Value value = values[i];
                        sb.append(value.getString());
                        if (i < values.length - 1) {
                            // not last value
                            sb.append(',');
                        }
                    }
                }
            }
            if (properties.hasNext()) {
                // comma between properties
                sb.append(", ");
            }
        }
        sb.append("]");
        return sb.toString();
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public static void dump(String absPath) {
        JcrSession session = null;
        JcrService jcr = StorageContextHelper.get().getJcrService();
        boolean inTx = TransactionSynchronizationManager.isSynchronizationActive();
        try {
            if (inTx) {
                session = jcr.getManagedSession();
            } else {
                session = jcr.getUnmanagedSession();
            }
            Item node = session.getItem(absPath);
            if (node.isNode()) {
                dump((Node) node);
            } else {
                throw new RepositoryRuntimeException("Not a node path: " + absPath);
            }
        } finally {
            if (!inTx && session != null) {
                session.logout();
            }
        }
    }

    /**
     * Dumps the contents of the given node with its children to standard output. INTERNAL for debugging only.
     *
     * @param node the node to be dumped
     * @throws javax.jcr.RepositoryException on repository errors
     */
    public static void dump(Node node) {
        try {
            dump(node, 0);
        } catch (RepositoryException e) {
            throw new RepositoryRuntimeException("Cannot dump node", e);
        }
    }

    private static void dump(Node node, int depth) throws RepositoryException {
        String nodeLine = (depth > 0 ? String.format("%-" + (depth * 2) + "s", "") : "") + node.getPath();
        System.out.println(nodeLine);
        PropertyIterator properties = node.getProperties();
        while (properties.hasNext()) {
            Property property = properties.nextProperty();
            String propLine = String.format("%-" + (depth * 2 + 2) + "s#" + property.getPath() + "=", "");
            System.out.print(propLine);
            if (property.getDefinition().isMultiple()) {
                Value[] values = property.getValues();
                for (int i = 0; i < values.length; i++) {
                    if (i > 0) {
                        System.out.print(", ");
                    }
                    System.out.print(values[i].getString());
                }
            } else {
                System.out.print(property.getString());
            }
            System.out.println();
        }
        NodeIterator nodes = node.getNodes();
        while (nodes.hasNext()) {
            Node child = nodes.nextNode();
            dump(child, depth++);
        }
    }

    private static String indent(int ammount, Node node) throws RepositoryException {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < ammount; i++) {
            sb.append("|  ");
        }
        if (node.hasNodes()) {
            sb.append("+-");
        } else {
            sb.append("\\-");
        }
        return sb.toString();
    }

    public static String nodePathFromUuid(String uuid) {
        String nodePath = null;
        JcrSession session = null;
        JcrService jcr = StorageContextHelper.get().getJcrService();
        boolean inTx = TransactionSynchronizationManager.isSynchronizationActive();
        try {
            if (inTx) {
                session = jcr.getManagedSession();
            } else {
                session = jcr.getUnmanagedSession();
            }
            Node node = session.getNodeByIdentifier(uuid);
            nodePath = node.getPath();
        } catch (RepositoryException e1) {
            log.error(e1.getMessage(), e1);
        } finally {
            if (!inTx && session != null) {
                session.logout();
            }
        }
        return nodePath;
    }

    public static DataStore getDataDtore(JcrSession session) {
        RepositoryImpl rep = (RepositoryImpl) session.getRepository();
        return rep.getDataStore();
    }

    public static ExtendedDbDataStore getExtendedDataStore(JcrSession session) {
        RepositoryImpl rep = (RepositoryImpl) session.getRepository();
        return getExtendedDataStore(rep);
    }

    public static ArtifactoryDataStore getArtifactoryDataStore() {
        JcrService jcrService = StorageContextHelper.get().getJcrService();
        JcrSession usession = null;
        try {
            usession = jcrService.getUnmanagedSession();
            return (ArtifactoryDataStore) JcrUtils.getDataDtore(usession);
        } finally {
            if (usession != null) {
                usession.logout();
            }
        }
    }

    public static ExtendedDbDataStore getExtendedDataStore(RepositoryImpl repositoryImpl) {
        DataStore store = repositoryImpl.getDataStore();
        if (store instanceof ExtendedDbDataStore) {
            return (ExtendedDbDataStore) store;
        } else if (store instanceof DbDataStore) {
            return new ExtendedDbDataStoreWrapper((DbDataStore) store);
        } else {
            Class<? extends DataStore> dsClass = store != null ? store.getClass() : null;
            throw new IllegalArgumentException(
                    "Repository is configured with an incompatible datastore: " + dsClass + ".");
        }
    }

    public static BinariesInfo getBinariesInfo(JcrSession jcrSession) {
        long size = 0;
        List<String> bereavedNodePaths = new ArrayList<String>();
        ArtifactoryDataStore store = (ArtifactoryDataStore) JcrUtils.getDataDtore(jcrSession);
        IterablePersistenceManager[] pmList = getIterablePersistenceManagers(jcrSession);
        SessionImpl session = (SessionImpl) jcrSession.getSession();
        try {
            Name[] binaryProps = new Name[]{session.getQName(JcrConstants.JCR_DATA)};
            long nodesCount = 0;
            for (IterablePersistenceManager pm : pmList) {
                Iterable<NodeId> ids = pm.getAllNodeIds(null, 0);
                for (NodeId id : ids) {
                    if ((++nodesCount % 500L) == 0) {
                        if (ArtifactoryDataStore.pauseOrBreak()) {
                            throw new TaskInterruptedException();
                        }
                    }
                    try {
                        NodeState state = pm.load(id);
                        for (Name name : binaryProps) {
                            if (state.hasPropertyName(name)) {
                                PropertyId pid = new PropertyId(id, name);
                                PropertyState ps = pm.load(pid);
                                InternalValue[] values = ps.getValues();
                                for (InternalValue value : values) {
                                    BLOBFileValue blobFileValue = value.getBLOBFileValue();
                                    long length = 0;
                                    boolean bereaved = false;
                                    try {
                                        DataIdentifier identifier = blobFileValue.getDataIdentifier();
                                        if (identifier != null) {
                                            length = store.getRecord(identifier).getLength();
                                            File storeFile = store.getFile(identifier);
                                            bereaved = storeFile == null || !storeFile.exists() ||
                                                    length != storeFile.length();
                                        }
                                    } catch (MissingOrInvalidDataStoreRecordException e) {
                                        bereaved = true;
                                    }
                                    if (bereaved) {
                                        log.info("Node '{}' is bereaved", state.getId());
                                        String nodePath =
                                                session.getJCRPath(
                                                        session.getHierarchyManager().getPath(state.getId()));
                                        String propertyName = name.getLocalName();
                                        log.warn("Could not read binary property '{}' on '{}' due to previous error!",
                                                name,
                                                nodePath);
                                        if (bereavedNodePaths != null) {
                                            //The datastore record of the binary data has not been found - schedule node for cleanup
                                            bereavedNodePaths.add(nodePath);
                                            log.warn(
                                                    "Cannot determine the length of property {}. Node {} will be discarded.",
                                                    propertyName, nodePath);
                                        }
                                    }
                                    if (length > 0) {
                                        size += length;
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        Throwable cause = ExceptionUtils
                                .getCauseOfTypes(e, ItemNotFoundException.class, NoSuchItemStateException.class);
                        if (cause == null) {
                            throw new RepositoryException(e);
                        }
                        // the node may have been deleted or moved in the meantime
                        // ignore it
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Could not retreive binaries info.", e);
        }
        return new BinariesInfo(size, bereavedNodePaths);
    }

    public static boolean isValidJcrName(String value) {
        try {
            assertValidJcrName(value);
        } catch (org.artifactory.exception.IllegalNameException e) {
            log.debug(e.getMessage());
            return false;
        }
        return true;
    }

    public static void assertValidJcrName(String value) {
        try {
            NameParser.checkFormat(value);
        } catch (IllegalNameException e) {
            throw new org.artifactory.exception.IllegalNameException("Invalid jcr name: " + e.getMessage());
        }
    }

    public static void cleanBereavedNodes(JcrSession session, List<String> bereavedNodePaths) {
        for (String bereaved : bereavedNodePaths) {
            Item item;
            try {
                item = session.getItem(bereaved);
            } catch (Exception e) {
                log.warn(
                        "Bereaved path item {} could not be retrieved (container node was probably removed first): {}.",
                        bereaved, e.getMessage());
                if (log.isDebugEnabled()) {
                    log.debug("Bereaved path item {} retrieval error.", bereaved, e);
                }
                continue;
            }
            try {
                log.warn("Removing binary node with no matching datastore data: {}.", bereaved);
                if (JcrConstants.JCR_CONTENT.equals(item.getName())) {
                    //If we are a jcr:content (of a file node container), remove the container node
                    Node parent = item.getParent();
                    parent.remove();
                } else {
                    item.remove();
                }
            } catch (RepositoryException e) {
                log.warn("Could not remove bereaved node path {}: {}", e.getMessage());
                if (log.isDebugEnabled()) {
                    log.debug("Could not remove bereaved node path {}.", e);
                }
            }
        }
        if (bereavedNodePaths.size() > 0) {
            session.save();
        }
    }

    public static class BinariesInfo {
        private final long binaryPropertiesTotalBytes;
        private final List<String> bereavedNodePaths;

        public BinariesInfo(long bytes, List<String> paths) {
            binaryPropertiesTotalBytes = bytes;
            bereavedNodePaths = paths;
        }

        public long getBinaryPropertiesTotalBytes() {
            return binaryPropertiesTotalBytes;
        }

        public List<String> getBereavedNodePaths() {
            return bereavedNodePaths;
        }
    }
}
