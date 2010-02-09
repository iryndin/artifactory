package org.artifactory.jcr.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
public abstract class JcrNodeTraversal {
    private final static Logger log = LoggerFactory.getLogger(JcrNodeTraversal.class);

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
}
