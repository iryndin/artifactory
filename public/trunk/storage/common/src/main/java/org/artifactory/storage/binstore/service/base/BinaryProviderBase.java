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

package org.artifactory.storage.binstore.service.base;

import com.google.common.collect.Lists;
import org.artifactory.storage.StorageProperties;
import org.artifactory.storage.binstore.service.BinaryProvider;
import org.artifactory.storage.binstore.service.BinaryStoreServices;
import org.artifactory.storage.binstore.service.MutableBinaryProvider;
import org.artifactory.storage.config.model.Property;
import org.artifactory.storage.config.model.ProviderMetaData;

import java.util.List;
import java.util.Set;

/**
 * Date: 12/12/12
 * Time: 3:03 PM
 *
 * @author freds
 */
public abstract class BinaryProviderBase implements BinaryProvider {
    MutableBinaryProvider mutableBinaryProvider;

    public void initialize() {
        mutableBinaryProvider.initialize();
    }

    public BinaryProviderBase next() {
        return mutableBinaryProvider.next();
    }

    public long getLongParam(String name, long defaultValue) {
        return mutableBinaryProvider.getLongParam(name, defaultValue);
    }

    public int getIntParam(String name, int defaultValue) {
        return mutableBinaryProvider.getIntParam(name, defaultValue);
    }

    public boolean getBooleanParam(String name, boolean defaultValue) {
        return mutableBinaryProvider.getBooleanParam(name, defaultValue);
    }

    public Set<Property> getProperties() {
        return mutableBinaryProvider.getproperties();
    }

    public BinaryStoreServices getBinaryStoreServices() {
        return mutableBinaryProvider.getBinaryStoreServices();
    }

    public ProviderMetaData getProviderMetaData() {
        return mutableBinaryProvider.getProviderMetaData();
    }

    public List<BinaryProviderBase> getSubBinaryProviders() {
        return mutableBinaryProvider.getSubBinaryProviders();
    }

    public BinaryProviderBase getBinaryProvider() {
        return mutableBinaryProvider.getBinaryProvider();
    }

    public String getParam(String name, String defaultValue) {
        return mutableBinaryProvider.getParam(name, defaultValue);
    }

    public String getProperty(String name) {
        return mutableBinaryProvider.getProperty(name);
    }

    public StorageProperties getStorageProperties() {
        return mutableBinaryProvider.getStorageProperties();
    }

    public <T>List<T> visit(BinaryProviderVisitor<T> binaryProviderVisitor) {
        List<T> result= Lists.newArrayList();
        result.addAll(binaryProviderVisitor.visit(this));
        if(next()!=null) {
            result.addAll(next().visit(binaryProviderVisitor));
        }
        for (BinaryProviderBase providerBase : getSubBinaryProviders()) {
            result.addAll(providerBase.visit(binaryProviderVisitor));
        }
        return result;
    }
}
