package org.artifactory.storage.binstore.service;

import org.artifactory.storage.binstore.GarbageCollectorInfo;

import java.util.Collection;

/**
 * @author Gidi Shabat
 */
public interface GarbageCollectorListener {
    void start();
    void toDelete(Collection<BinaryData> binsToDelete);
    void finished(GarbageCollectorInfo result);
}
