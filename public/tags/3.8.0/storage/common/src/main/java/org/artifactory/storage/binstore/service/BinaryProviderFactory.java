package org.artifactory.storage.binstore.service;

import com.google.common.collect.Lists;
import org.artifactory.storage.StorageProperties;
import org.artifactory.storage.config.model.ChainMetaData;
import org.artifactory.storage.config.model.Param;
import org.artifactory.storage.config.model.ProviderMetaData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * @author Gidi Shabat
 */
public class BinaryProviderFactory {
    private static final Logger log = LoggerFactory.getLogger(BinaryProviderFactory.class);
    public static <T extends BinaryProviderBase> T create(BinaryProviderTypes type, String id,
            StorageProperties properties) {
        return create(type, id, properties, null);
    }

    public static <T extends BinaryProviderBase> T create(BinaryProviderTypes type, String id,
            StorageProperties properties, InternalBinaryStore binaryStore) {
        ProviderMetaData providerMetaData = new ProviderMetaData(id, type.getType());
        return create(providerMetaData, properties, binaryStore);

    }

    public static <T extends BinaryProviderBase> T create(ProviderMetaData providerMetaData,
            StorageProperties properties, InternalBinaryStore binaryStore) {
        BinaryProviderTypes type = BinaryProviderTypes.getBinaryProviderTypeById(providerMetaData.getType());
        BinaryProviderBase instance = type.instance();
        instance.setProviderMetaData(providerMetaData);
        instance.setStorageProperties(properties);
        instance.setBinaryStore(binaryStore);
        instance.setEmpty(new EmptyBinaryProvider());
        instance.initialize();
        return (T) instance;
    }

    public static BinaryProviderBase buildProviders(ChainMetaData configChain, InternalBinaryStore binaryStore,
            StorageProperties storageProperties) {
        log.debug("Initializing providers by chain; '{}'", configChain.getTemplate());
        List<BinaryProviderBase> binaryProviders = Lists.newArrayList();
        binaryProviders.add(build(configChain.getProviderMetaData(), binaryStore, storageProperties));
        return binaryProviders.get(0);
    }

    private static BinaryProviderBase build(ProviderMetaData providerMetaData, InternalBinaryStore binaryStore,
            StorageProperties storageProperties) {
        if (providerMetaData == null) {
            return null;
        }
        BinaryProviderBase binaryProvider = create(providerMetaData, storageProperties, binaryStore);
        binaryProvider.setBinaryProvider(build(providerMetaData.getProviderMetaData(), binaryStore, storageProperties));
        for (ProviderMetaData subProviderMetaData : providerMetaData.getSubProviderMetaDataList()) {
            binaryProvider.addSubBinaryProvider(build(subProviderMetaData, binaryStore, storageProperties));
        }
        binaryProvider.initialize();
        return binaryProvider;
    }


    public static FileBinaryProvider searchForFileBinaryProvider(BinaryProviderBase binaryProvider) {
        if (binaryProvider == null) {
            return null;
        }
        BinaryProviderBase next = binaryProvider.getBinaryProvider();
        if (next instanceof FileBinaryProvider) {
            return (FileBinaryProvider) next;
        }
        FileBinaryProvider fileBinaryProvider = searchForFileBinaryProvider(next);
        if (fileBinaryProvider != null) {
            return fileBinaryProvider;
        }
        for (BinaryProviderBase binaryProviderBase : binaryProvider.getSubBinaryProviders()) {
            if (binaryProviderBase instanceof FileBinaryProvider) {
                return (FileBinaryProvider) binaryProviderBase;
            } else {
                fileBinaryProvider = searchForFileBinaryProvider(binaryProviderBase);
                if (fileBinaryProvider != null) {
                    return fileBinaryProvider;
                }
            }
        }
        return null;
    }

    public static List<BinaryProviderBase> createExternalBinaryProviders(String mode, String externalDir,
            String filestoreDir, StorageProperties storageProperties, InternalBinaryStore binaryStore) {
        List<BinaryProviderBase> result = Lists.newArrayList();
        if (externalDir != null) {
            if (mode != null) {
                result.add(createExternalWrapperBinaryProvider(mode, filestoreDir, storageProperties, binaryStore));
            }
            result.add(createExternalFileBinaryProvider(externalDir, storageProperties, binaryStore));
        }
        return result;
    }

    public static BinaryProviderBase createExternalWrapperBinaryProvider(String mode, String dir,
            StorageProperties storageProperties, InternalBinaryStore binaryStore) {
        ProviderMetaData providerMetaData = new ProviderMetaData("external-wrapper", "external-wrapper");
        providerMetaData.addParam(new Param("connectMode", mode));
        providerMetaData.addParam(new Param("dir", dir));
        return BinaryProviderFactory.create(providerMetaData, storageProperties, binaryStore);
    }

    public static BinaryProviderBase createExternalFileBinaryProvider(String dir,
            StorageProperties storageProperties, InternalBinaryStore binaryStore) {
        ProviderMetaData providerMetaData = new ProviderMetaData("external-file", "external-file");
        providerMetaData.addParam(new Param("dir", dir));
        return BinaryProviderFactory.create(providerMetaData, storageProperties, binaryStore);
    }
}
