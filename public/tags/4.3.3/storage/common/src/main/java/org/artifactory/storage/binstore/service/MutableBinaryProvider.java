/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2012 JFrog Ltd.
 *
 * Artifactory is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Artifactory is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Artifactory.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.artifactory.storage.binstore.service;

import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.artifactory.storage.StorageProperties;
import org.artifactory.storage.binstore.service.base.BinaryProviderBase;
import org.artifactory.storage.config.model.Property;
import org.artifactory.storage.config.model.ProviderMetaData;

import java.util.List;
import java.util.Set;

/**
 * @author Gidi Shabat
 */
public class MutableBinaryProvider{
    // TODO: Refactor to a smaller interface neccessary for the binary provider
    // The goal is to limit access from Binary Provider to BinaryStore to the minimum needed
    private ProviderMetaData providerMetaData;
    private StorageProperties storageProperties;
    private BinaryProviderBase binaryProvider;
    private BinaryProviderBase parentBinaryProvider;
    private List<BinaryProviderBase> subBinaryProviders = Lists.newArrayList();
    private BinaryProviderBase empty;
    private BinaryStoreServices binaryStoreServices;

    public void initialize() {
    }

    public BinaryProviderBase next() {
        if (binaryProvider == null) {
            return empty;
        }
        return binaryProvider;
    }

    public List<BinaryProviderBase> getSubBinaryProviders() {
        return subBinaryProviders;
    }

    public StorageProperties getStorageProperties() {
        return storageProperties;
    }

    public ProviderMetaData getProviderMetaData() {
        return providerMetaData;
    }

    public BinaryProviderBase getBinaryProvider() {
        return binaryProvider;
    }

    public BinaryStoreServices getBinaryStoreServices() {
        return binaryStoreServices;
    }

    public Set<Property> getproperties() {
        return providerMetaData.getProperties();
    }

    public int getIntParam(String name, int defaultValue) {
        String param = providerMetaData.getParamValue(name);
        if (StringUtils.isBlank(param)) {
            return defaultValue;
        }
        return Integer.valueOf(param);
    }

    public boolean getBooleanParam(String name, boolean defaultValue) {
        String param = providerMetaData.getParamValue(name);
        if (StringUtils.isBlank(param)) {
            return defaultValue;
        }
        return Boolean.valueOf(param);
    }

    public long getLongParam(String name, long defaultValue) {
        String param = providerMetaData.getParamValue(name);
        if (StringUtils.isBlank(param)) {
            return defaultValue;
        }
        return Long.valueOf(param);
    }

    public String getParam(String name, String defaultValue) {
        String param = providerMetaData.getParamValue(name);
        if (StringUtils.isBlank(param)) {
            param = defaultValue;
        }
        return param;
    }

    public BinaryProviderBase getParentBinaryProvider() {
        return parentBinaryProvider;
    }

    public void setBinaryStoreServices(BinaryStoreServices binaryStoreServices) {
        this.binaryStoreServices = binaryStoreServices;
    }

    public void addSubBinaryProvider(BinaryProviderBase binaryProviderBase) {
        subBinaryProviders.add(binaryProviderBase);
    }

    public void setStorageProperties(StorageProperties storageProperties) {
        this.storageProperties = storageProperties;
    }

    public void setProviderMetaData(ProviderMetaData providerMetaData) {
        this.providerMetaData = providerMetaData;
    }

    public void setEmpty(BinaryProviderBase empty) {
        this.empty = empty;
    }

    public void setBinaryProvider(BinaryProviderBase binaryProviderBase) {
        this.binaryProvider = binaryProviderBase;
    }

    public void setParentBinaryProvider(BinaryProviderBase parentBinaryProvider) {
        this.parentBinaryProvider = parentBinaryProvider;
    }
}
