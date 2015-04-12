package org.artifactory.addon;

import org.artifactory.storage.StorageProperties;
import org.artifactory.storage.binstore.service.BinaryProviderBase;

import java.util.List;

/**
 * @author Gidi Shabat
 */
public interface FileStoreAddon extends Addon {
    List<BinaryProviderBase> getS3JClouds(StorageProperties storageProperties);
}
