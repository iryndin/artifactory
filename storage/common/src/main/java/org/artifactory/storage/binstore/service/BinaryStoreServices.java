package org.artifactory.storage.binstore.service;

import java.util.Set;

/**
 * @author Gidi Shabat
 */
public interface BinaryStoreServices {
    void decrementNoDeleteLock(String sha1);

    int incrementNoDeleteLock(String sha1);

    boolean isActivelyUsed(String sha1);

    void addGCListener(GarbageCollectorListener garbageCollectorListener);

    Set<String> isInStore(Set<String> filesInFolder);

    Boolean getAndResetBinaryProvidersStatus(boolean promotOtherNodes);

    void notifyError(String id, String type);

}
