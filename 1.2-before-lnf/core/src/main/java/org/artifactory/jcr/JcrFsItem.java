package org.artifactory.jcr;

import org.apache.log4j.Logger;
import static org.artifactory.jcr.ArtifactoryJcrConstants.PROP_ARTIFACTORY_REPO_KEY;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public abstract class JcrFsItem implements Comparable<JcrFsItem> {
    @SuppressWarnings({"UNUSED_SYMBOL", "UnusedDeclaration"})
    private final static Logger LOGGER = Logger.getLogger(JcrFsItem.class);

    protected final Node node;

    public JcrFsItem(Node fileNode) {
        this.node = fileNode;
    }

    public String getName() {
        try {
            return node.getName();
        } catch (RepositoryException e) {
            throw new RuntimeException("Failed to retrieve node's name.", e);
        }
    }

    /**
     * Get the absolute path of the item
     */
    public String absPath() {
        try {
            return node.getPath();
        } catch (RepositoryException e) {
            throw new RuntimeException("Failed to retrieve node's absolute path.", e);
        }
    }

    /**
     * Get the relative path of the item
     */
    @SuppressWarnings({"UnnecessaryLocalVariable"})
    public String relPath() {
        String repoKey = repoKey();
        String absPath = absPath();
        int relPathBegin = absPath.indexOf(repoKey) + repoKey.length() + 1;
        String rePath = absPath.substring(relPathBegin);
        return rePath;
    }

    public String repoKey() {
        try {
            return node.getProperty(PROP_ARTIFACTORY_REPO_KEY).getString();
        } catch (RepositoryException e) {
            throw new RuntimeException("Failed to retrieve repository key.", e);
        }
    }

    public void remove() {
        NodeLocker nodeLocker;
        nodeLocker = new NodeLocker(node);
        try {
            nodeLocker.lock();
            node.remove();
        } catch (RepositoryException e) {
            throw new RuntimeException("Failed to remove node.", e);
        } finally {
            nodeLocker.unlock();

        }
    }

    @SuppressWarnings({"UnnecessaryLocalVariable"})
    public JcrFolder getParent() {
        try {
            Node parent = node.getParent();
            JcrFolder parentFolder = new JcrFolder(parent);
            return parentFolder;
        } catch (RepositoryException e) {
            throw new RuntimeException("Failed to get node's parent folder.", e);
        }
    }

    public int compareTo(JcrFsItem item) {
        return getName().compareTo(item.getName());
    }

    public abstract boolean isDirectory();
}
