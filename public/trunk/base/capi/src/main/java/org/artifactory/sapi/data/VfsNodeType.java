package org.artifactory.sapi.data;

import static org.artifactory.storage.StorageConstants.*;

/**
 * Date: 8/4/11
 * Time: 4:49 PM
 *
 * @author Fred Simon
 */
public enum VfsNodeType {
    REPO_ROOT("rep:root"),
    UNSTRUCTURED(NT_UNSTRUCTURED, "mix:referenceable"),
    FOLDER(NT_ARTIFACTORY_FOLDER, MIX_ARTIFACTORY_BASE),
    FILE(NT_ARTIFACTORY_FILE, MIX_ARTIFACTORY_BASE),
    METADATA(NT_ARTIFACTORY_METADATA, MIX_ARTIFACTORY_BASE),
    STATS(NT_ARTIFACTORY_METADATA, MIX_ARTIFACTORY_BASE, MIX_ARTIFACTORY_STATS),
    LOG(NT_ARTIFACTORY_LOG_ENTRY);

    public final String storageTypeName;
    public final String[] mixinNames;

    VfsNodeType(String storageType, String... mixins) {
        this.storageTypeName = storageType;
        this.mixinNames = mixins;
    }
}
