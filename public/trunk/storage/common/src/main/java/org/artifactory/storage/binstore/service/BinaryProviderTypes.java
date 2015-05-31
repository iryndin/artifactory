package org.artifactory.storage.binstore.service;

/**
 * @author Gidi Shabat
 */
public enum BinaryProviderTypes {
    cachefs("cache-fs", "org.artifactory.storage.binstore.service.providers.FileCacheBinaryProviderImpl"),
    fileSystem("file-system", "org.artifactory.storage.binstore.service.providers.FileBinaryProviderImpl"),
    eventual("eventual", "org.artifactory.addon.filestore.eventual.EventuallyPersistedBinaryProvider"),
    s3("s3", "org.artifactory.addon.filestore.s3.S3JCloudsBinaryProvider"),
    retry("retry", "org.artifactory.storage.binstore.service.providers.RetryBinaryProvider"),
    empty("empty", "org.artifactory.storage.db.binstore.service.EmptyBinaryProvider"),
    userTracking("user-tracking", "org.artifactory.storage.db.binstore.service.UsageTrackingBinaryProvider"),
    blob("blob", "org.artifactory.storage.db.binstore.service.BlobBinaryProviderImpl"),
    tracking("tracking", "org.artifactory.storage.db.binstore.service.UsageTrackingBinaryProvider"),
    doubleStorage("double", "org.artifactory.storage.binstore.service.providers.DoubleFileBinaryProviderImpl"),
    dynamic("dynamic", "org.artifactory.storage.binstore.service.providers.DynamicFileBinaryProviderImpl"),
    externalWrapper("external-wrapper",
            "org.artifactory.storage.binstore.service.providers.ExternalWrapperBinaryProviderImpl"),
    externalFile("external-file", "org.artifactory.storage.db.binstore.service.ExternalFileBinaryProviderImpl");

    private String type;
    private String className;

    BinaryProviderTypes(String type, String className) {
        this.type = type;
        this.className = className;
    }

    public static BinaryProviderTypes getBinaryProviderTypeById(String type) {
        for (BinaryProviderTypes binaryProviderType : values()) {
            if (binaryProviderType.type.equals(type)) {
                return binaryProviderType;
            }
        }
        return null;
    }

    public String getType() {
        return type;
    }

    public BinaryProviderBase instance() {
        try {
            Class<?> providerClass = Class.forName(className);
            return (BinaryProviderBase) providerClass.newInstance();
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(
                    "Failed to load binary providers. Reason: couldn't fund implementation for provider: " + type, e);
        }
    }
}