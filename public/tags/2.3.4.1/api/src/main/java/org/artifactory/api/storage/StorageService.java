/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2011 JFrog Ltd.
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

package org.artifactory.api.storage;

import org.artifactory.api.common.MultiStatusHolder;

/**
 * @author yoavl
 */
public interface StorageService {

    void compress(MultiStatusHolder statusHolder);

    boolean isDerbyUsed();

    void logStorageSizes();

    /**
     * @return The artifacts storage size in bytes
     */
    long getStorageSize();

    void exportDbDataStore(String destDir);

    /**
     * Check that manual garbage collection can be run, and then activate the double GC asynchronously.
     * @param statusHolder
     */
    void callManualGarbageCollect(MultiStatusHolder statusHolder);
}
