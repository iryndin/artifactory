package org.artifactory.jcr;

import org.apache.log4j.Logger;

import javax.jcr.InvalidItemStateException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.lock.Lock;
import javax.jcr.lock.LockException;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public class NodeLocker {
    @SuppressWarnings({"UNUSED_SYMBOL", "UnusedDeclaration"})
    private final static Logger LOGGER = Logger.getLogger(NodeLocker.class);

    private Node node;
    private int maxRetries;
    private int msBetweenRetries;

    public NodeLocker(Node node) {
        this(node, 3, 1000);
    }

    public NodeLocker(Node node, int maxRetries, int msBetweenRetries) {
        this.node = node;
        this.maxRetries = maxRetries;
        this.msBetweenRetries = msBetweenRetries;
    }

    @SuppressWarnings({"UnnecessaryLocalVariable"})
    public Lock lock() {
        int retries = maxRetries;
        while (true) {
            try {
                if (node.isLocked()) {
                    //First try to get an existing lock
                    return node.getLock();
                } else if (node.isModified() || node.isCheckedOut()) {
                    //If there are pending changes save them or locking will fail
                    node.save();
                }
                Lock lock = node.lock(false, true);
                return lock;
            } catch (LockException e) {
                retries--;
                if (retries > 0) {
                    try {
                        Thread.sleep(msBetweenRetries);
                    } catch (InterruptedException e1) {
                        //Do nothing
                    }
                } else {
                    throw new RuntimeException(
                            "Failed to acquire node lock after " + maxRetries + " retries.");
                }
            } catch (RepositoryException e) {
                throw new RuntimeException("Failed to acquire node lock.", e);
            }
        }
    }

    public void unlock() {
        boolean locked = false;
        try {
            locked = node.holdsLock();
        } catch (InvalidItemStateException e) {
            //Node has been removed
            return;
        } catch (RepositoryException e) {
            //Ignore
        }
        if (!locked) {
            LOGGER.warn("Failed to release non-existent node lock.");
        } else {
            try {
                node.save();
            } catch (RepositoryException e) {
                LOGGER.warn("Failed to save node: " + e.getMessage());
                try {
                    node.refresh(false);
                } catch (RepositoryException e1) {
                    throw new RuntimeException("Failed to revert node.", e);
                }
            }
            try {
                node.unlock();
            } catch (RepositoryException e) {
                throw new RuntimeException("Failed to release node lock.", e);
            }
        }
    }
}
