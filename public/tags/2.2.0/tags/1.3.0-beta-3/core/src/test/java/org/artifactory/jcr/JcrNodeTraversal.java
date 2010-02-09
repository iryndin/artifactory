package org.artifactory.jcr;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import java.util.Arrays;

/**
 * Prints recursively a jcr node tree.
 *
 * @author Yossi Shaul
 */
public abstract class JcrNodeTraversal {

    public static void preorder(Node node) {
        System.out.println("********************************************");
        try {
            preorder(node, 0);
        } catch (RepositoryException e) {
            System.err.println("Error traversing tree: " + e.getMessage());
        }
        System.out.println("********************************************");
    }

    private static void preorder(Node node, int depth) throws RepositoryException {
        visit(node, depth);

        // print child nodes
        NodeIterator nodeIterator = node.getNodes();
        while (nodeIterator.hasNext()) {
            preorder(nodeIterator.nextNode(), depth + 1);
        }
    }

    private static void visit(Node node, int depth) throws RepositoryException {
        System.out.println(spaces(depth) + node.getName());
        PropertyIterator properties = node.getProperties();
        while (properties.hasNext()) {
            Property property = properties.nextProperty();
            System.out.print(spaces(depth + 2));
            boolean multiple = property.getDefinition().isMultiple();
            if (!multiple) {
                System.out.println(property.getName() + " = " + property.getValue());
            } else {
                System.out.println(
                        property.getName() + " = " + Arrays.asList(property.getValues()));
            }
        }
    }

    private static String spaces(int ammount) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < ammount; i++) {
            sb.append("| ");
        }
        sb.append("|");
        return sb.toString();
    }
}
