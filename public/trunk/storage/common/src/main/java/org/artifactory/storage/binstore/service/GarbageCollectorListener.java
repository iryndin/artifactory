package org.artifactory.storage.binstore.service;

import org.artifactory.storage.binstore.GarbageCollectorInfo;

import java.util.Collection;

/**
 * Listener interface used in Binary Store to get event during Artifactory File store GC steps.
 *
 * @author Gidi Shabat
 */
public interface GarbageCollectorListener {
    /**
     * The garbage collection is starting.
     * Called at the very beginning of the process.
     */
    void start();

    /**
     * Once the list of potential checksums unreferenced is done, this method is called
     * with the collection of binaries to delete.
     * Every delete event is ending as a delete(sha1) call on the binary providers.
     * @param binsToDelete all binary data entries that GC will try to delete
     */
    void toDelete(Collection<BinaryData> binsToDelete);

    /**
     * Once all deletion was done this is called with result data object.
     * @param result Information struct about what happened during GC
     */
    void finished(GarbageCollectorInfo result);

    /**
     * Called when Artifactory server is going down.
     * all resources should be cleaned after this call.
     */
    void destroy();
}
