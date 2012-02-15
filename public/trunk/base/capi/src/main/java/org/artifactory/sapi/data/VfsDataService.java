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

package org.artifactory.sapi.data;

import org.artifactory.sapi.common.RequiresTransaction;
import org.artifactory.sapi.common.TxPropagation;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.InputStream;

/**
 * All method to read and write VfsNode.
 * All methods here need to be access from inside a transaction since it returns a VfsNode object that is linked
 * to the transaction.
 * <p/>
 * Date: 8/3/11
 * Time: 4:58 PM
 *
 * @author Fred Simon
 */
@RequiresTransaction(TxPropagation.ENFORCED)
public interface VfsDataService {

    @Nullable
    VfsNode findByPath(String absolutePath);

    @Nullable
    VfsNode findByStorageId(String storageId);

    boolean delete(VfsNode node);

    /**
     * Create a binary using UTF-8 encoding
     *
     * @param mimeType
     * @param content
     * @return
     */
    @Nonnull
    BinaryContent createBinary(String mimeType, String content);

    /**
     * Create a default unstructured node at the absolute path
     *
     * @param absolutePath
     * @return
     */
    @Nonnull
    MutableVfsNode getOrCreate(String absolutePath);

    @Nonnull
    MutableVfsNode getOrCreate(String absolutePath, VfsNodeType type);

    @Nonnull
    MutableVfsNode getOrCreate(VfsNode parent, String relPath, VfsNodeType type);

    @Nonnull
    MutableBinaryContent createBinary(String mimeType, String content, String encoding);

    @Nonnull
    MutableBinaryContent createBinary(String mimeType, InputStream stream);

    MutableVfsNode makeMutable(VfsNode vfsNode);

    boolean pathExists(String absolutePath);
}
