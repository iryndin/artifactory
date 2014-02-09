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

package org.artifactory.storage.mbean;

import org.artifactory.storage.StorageService;
import org.artifactory.storage.binstore.service.InternalBinaryStore;

/**
 * StorageMBean implementation.
 *
 * @author Yossi Shaul
 */
public class ManagedStorage implements ManagedStorageMBean {
    private final StorageService storageService;
    private final InternalBinaryStore binaryStore;

    public ManagedStorage(StorageService storageService, InternalBinaryStore binaryStore) {
        this.storageService = storageService;
        this.binaryStore = binaryStore;
    }

    @Override
    public long getSize() {
        return binaryStore.getStorageSize();
    }

    public void pingTest() {
        binaryStore.ping();
    }
}
