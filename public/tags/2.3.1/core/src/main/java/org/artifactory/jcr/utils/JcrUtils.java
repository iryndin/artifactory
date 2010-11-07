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

package org.artifactory.jcr.utils;

import org.apache.jackrabbit.core.RepositoryImpl;
import org.apache.jackrabbit.core.data.DataStore;
import org.apache.jackrabbit.core.data.db.DbDataStore;
import org.apache.jackrabbit.core.data.db.ExtendedDbDataStoreWrapper;
import org.apache.jackrabbit.spi.commons.conversion.IllegalNameException;
import org.apache.jackrabbit.spi.commons.conversion.NameParser;
import org.artifactory.api.repo.exception.RepositoryRuntimeException;
import org.artifactory.jcr.JcrService;
import org.artifactory.jcr.JcrSession;
import org.artifactory.jcr.jackrabbit.ExtendedDbDataStore;
import org.artifactory.log.LoggerFactory;
import org.artifactory.spring.InternalContextHelper;
import org.slf4j.Logger;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import java.util.Collections;
import java.util.List;

/**
 * Prints recursively a jcr node tree.
 *
 * @author Yossi Shaul
 */
public abstract class JcrUtils {
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
        JcrService jcr = InternalContextHelper.get().getJcrService();
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
        JcrService jcr = InternalContextHelper.get().getJcrService();
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

    public static ExtendedDbDataStore getDataStore(RepositoryImpl repositoryImpl) {
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
}
