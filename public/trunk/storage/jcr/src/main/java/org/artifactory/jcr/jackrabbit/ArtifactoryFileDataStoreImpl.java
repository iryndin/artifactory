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

package org.artifactory.jcr.jackrabbit;

import org.apache.jackrabbit.core.data.DataIdentifier;
import org.apache.jackrabbit.core.data.DataStoreException;
import org.apache.jackrabbit.core.util.db.DatabaseAware;
import org.artifactory.common.ConstantValues;
import org.artifactory.log.LoggerFactory;
import org.slf4j.Logger;

import java.io.File;

/**
 * @author freds
 */
public class ArtifactoryFileDataStoreImpl extends ArtifactoryDataStore implements DatabaseAware {

    private static final Logger log = LoggerFactory.getLogger(ArtifactoryFileDataStoreImpl.class);

    private String fileStoreDir;

    private File binariesFolder;

    /**
     * @return Get file only if it exists
     * @throws DataStoreException If the file doesn't exist
     */
    @Override
    public File getOrCreateFile(DataIdentifier identifier, long expectedLength) throws DataStoreException {
        File result = getFile(identifier);
        if (result.exists() && result.length() == expectedLength) {
            return result;
        }

        // File should have been found (Filestore class). If reach here file missing....
        MissingOrInvalidDataStoreRecordException e = new MissingOrInvalidDataStoreRecordException(
                "Record not found: '" + identifier + "' as file '" + result.getAbsolutePath() +
                        "' does not exist or has wrong length.");

        //Delete if fix consistency flag is on
        if (v1 && ConstantValues.jcrFixConsistency.getBoolean()) {
            try {
                ArtifactoryDbDataRecord record = getCachedRecord(identifier.toString());
                if (log.isDebugEnabled()) {
                    log.debug("Deleting record " + record + ".", e);
                } else {
                    log.info("Deleting record " + record + " (cause: " + e.getMessage() + ").");
                }
                //rs.delete() will have no effect since we rollback the tx - do it in a separate tx
                deleteEntry(identifier);
                if (record != null) {
                    try {
                        record.markForDeletion();
                        deleteRecord(record);
                    } catch (Exception ignore) {
                        // will be deleted soon
                    }
                }
            } catch (Exception deleteException) {
                log.debug("Could not delete record with identifier '" + identifier + "'.", deleteException);
            }
        }
        throw e;
    }

    @Override
    protected boolean validState(ArtifactoryDbDataRecord record) {
        File file = getFile(record.getIdentifier());
        return file.exists() && file.length() == record.length;
    }

    /**
     * Returns the location this datastore stores the actual binaries (as opposed to db datastore that saves them in the
     * db). This is normally same as {@link org.artifactory.jcr.jackrabbit.ArtifactoryFileDataStoreImpl#fileStoreDir}.
     *
     * @return The location this datastore stores the actual binaries.
     */
    @Override
    public File getBinariesFolder() {
        return binariesFolder;
    }

    @Override
    public boolean isStoreBinariesAsBlobs() {
        return false;
    }

    public String getFileStoreDir() {
        return fileStoreDir;
    }

    public void setFileStoreDir(String fileStoreDir) {
        this.fileStoreDir = fileStoreDir;
    }

    @Override
    public void init(String homeDir) throws DataStoreException {
        initRootStoreDir(homeDir);
        super.init(homeDir);
    }

    private void initRootStoreDir(String homeDir) throws DataStoreException {
        String fsDir = getFileStoreDir();
        if (fsDir != null && fsDir.length() > 0) {
            binariesFolder = new File(fsDir);
        } else {
            binariesFolder = new File(homeDir, "filestore");
        }
        if (!binariesFolder.exists()) {
            if (!binariesFolder.mkdirs()) {
                throw new DataStoreException("Could not create file store folder " + binariesFolder.getAbsolutePath());
            }
        }
    }
}
