package org.artifactory.storage.binstore.service.base;

import org.artifactory.storage.binstore.service.MutableBinaryProvider;

/**
 * @author Gidi Shabat
 */
public class MutableBinaryProviderInjector {
    public static MutableBinaryProvider getMutableBinaryProvider(BinaryProviderBase binaryProviderBase){
        return binaryProviderBase.mutableBinaryProvider;
    }

    public static void setMutableBinaryProvider(BinaryProviderBase binaryProviderBase,
            MutableBinaryProvider mutableBinaryProvider){
        binaryProviderBase.mutableBinaryProvider=mutableBinaryProvider;
    }
}
