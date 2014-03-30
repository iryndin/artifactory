/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2013 JFrog Ltd.
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

package org.artifactory.storage;

import org.artifactory.binstore.BinaryInfo;

/**
 * All exceptions that means the request can be redone (dup keys or others).
 *
 * @author Fred Simon
 */
public class StorageRetryException extends StorageException {
    private final BinaryInfo binaryInfo;

    public StorageRetryException(BinaryInfo binaryInfo, Throwable cause) {
        super("Insertion of " + binaryInfo + " failed. Auto retry exception", cause);
        this.binaryInfo = binaryInfo;
    }

    public BinaryInfo getBinaryInfo() {
        return binaryInfo;
    }
}
