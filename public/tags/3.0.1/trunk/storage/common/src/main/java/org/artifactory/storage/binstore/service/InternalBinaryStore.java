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

import org.artifactory.api.common.MultiStatusHolder;
import org.artifactory.binstore.BinaryInfo;
import org.artifactory.storage.binstore.GarbageCollectorInfo;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Nullable;
import java.io.File;
import java.util.Collection;

/**
 * Date: 12/11/12
 * Time: 2:13 PM
 *
 * @author freds
 */
public interface InternalBinaryStore extends BinaryStore {

    /**
     * Add the binary data line to this data store based on all params after verification
     * from binary provider delegate.
     * Verification steps depends on configuration and delegates answers.
     * Method will returns null if no binary object with specified info is found,
     * or no filestore delegate was configured.
     *
     * @param sha1   The SHA1 key value
     * @param md5    The md5 of this new entry
     * @param length The length of the binary
     * @return The full binary info object created and stored, or null if no object
     * @throws org.artifactory.storage.binstore.service.BinaryNotFoundException
     *          if verification failed
     */
    @Nullable
    BinaryInfo addBinaryRecord(String sha1, String md5, long length) throws BinaryNotFoundException;

    /**
     * Activate Garbage Collection for this binary store
     *
     * @return The results of the GC process
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    GarbageCollectorInfo garbageCollect();

    /**
     * Delete all left over folders and files in the binaries directory that are not declared
     * at all in this binary store.
     *
     * @param statusHolder A status holder of the process messages
     */
    void prune(MultiStatusHolder statusHolder);

    /**
     * The actual local directory where checksum binary files will be stored.
     *
     * @return the folder for this binary store
     */
    File getBinariesDir();

    /**
     * Add an external checksum filestore that can be used in read only mode.
     * This will create an external filestore binary provider
     * at the end of binary provider chain. So, if no binary found it will be used
     * in read only mode to provide the data stream.
     * The root directory provided should have the Artifactory checksum filestore layout:
     * [2 first characters of the sha1]/[the full sha1]
     * <p/>
     * No files will be deleted from the external filestore except if the connectMode
     * is specified to MOVE mode.
     *
     * @param externalDir The root directory of the files respecting a checksum filestore
     * @param connectMode Define the way the streams and files of the external filestore
     *                    should be handled by this binary store
     */
    void addExternalFilestore(File externalDir, ProviderConnectMode connectMode);

    /**
     * This will disconnect from the chain an existing external filestore added or configured.
     * Before being disconnected, the disconnectMode is going to be used to copy or move
     * all the files from the external filestore to this binary store.
     *
     * @param externalDir    The exact same root directory used previously to add the external filestore
     * @param disconnectMode The mode of disconnection to manage all the files
     *                       needed by this binary store and present in the external filestore
     * @param statusHolder   A collection of messages about the status of the disconnection
     */
    void disconnectExternalFilestore(File externalDir, ProviderConnectMode disconnectMode,
            MultiStatusHolder statusHolder);

    Collection<BinaryInfo> findAllBinaries();

    /**
     * Deletes binary row and all dependent rows from the database
     *
     * @param sha1ToDelete Checksum to delete
     * @return True if deleted. False if not found or error
     */
    @Transactional(propagation = Propagation.REQUIRED)
    boolean deleteEntry(String sha1ToDelete);

    /**
     * Deletes unreferenced archive paths. Shared archive paths might not be used after a binary is deleted.
     */
    @Transactional
    int deleteUnusedArchivePaths();

    /**
     * Deletes unreferenced archive names. Shared archive names might not be used after a binary is deleted.
     */
    @Transactional
    int deleteUnusedArchiveNames();
}
