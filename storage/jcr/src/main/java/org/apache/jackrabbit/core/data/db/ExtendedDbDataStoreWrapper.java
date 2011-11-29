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

package org.apache.jackrabbit.core.data.db;

import org.apache.jackrabbit.core.data.DataIdentifier;
import org.apache.jackrabbit.core.data.DataRecord;
import org.apache.jackrabbit.core.data.DataStoreException;
import org.apache.jackrabbit.core.util.db.ArtifactoryConnectionHelper;
import org.apache.jackrabbit.core.util.db.ConnectionHelper;
import org.artifactory.api.common.MultiStatusHolder;
import org.artifactory.jcr.jackrabbit.ExtendedDbDataStoreBase;

import java.io.InputStream;
import java.util.Iterator;

/**
 * A generic db datastore wrapper
 *
 * @author freds
 * @date Jun 23, 2009
 */
public class ExtendedDbDataStoreWrapper extends ExtendedDbDataStoreBase {

    private final DbDataStore store;

    public ExtendedDbDataStoreWrapper(DbDataStore store) {
        this.store = store;
    }

    public DataRecord addRecord(InputStream stream) throws DataStoreException {
        return store.addRecord(stream);
    }

    public int deleteAllOlderThan(long min) throws DataStoreException {
        return store.deleteAllOlderThan(min);
    }

    public Iterator<DataIdentifier> getAllIdentifiers() throws DataStoreException {
        return store.getAllIdentifiers();
    }

    public int getMinRecordLength() {
        return store.getMinRecordLength();
    }

    public DataRecord getRecord(DataIdentifier identifier) throws DataStoreException {
        return store.getRecord(identifier);
    }

    public DataRecord getRecordIfStored(DataIdentifier identifier) throws DataStoreException {
        return store.getRecordIfStored(identifier);
    }

    public void init(String homeDir) throws DataStoreException {
        store.init(homeDir);
    }

    public void updateModifiedDateOnAccess(long before) {
        store.updateModifiedDateOnAccess(before);
    }

    public String getDatabaseType() {
        return store.getDatabaseType();
    }

    public ArtifactoryConnectionHelper getConnectionHelper() {
        ConnectionHelper conHelper = store.conHelper;
        if (ArtifactoryConnectionHelper.class.getCanonicalName().equals(conHelper.getClass().getCanonicalName())) {
            //Note: no subclass check to avoid slowness of instanceof and variants
            return (ArtifactoryConnectionHelper) conHelper;
        } else {
            return new ArtifactoryConnectionHelper(conHelper);
        }
    }

    public void close() throws DataStoreException {
        store.close();
    }

    public void clearInUse() {
        store.clearInUse();
    }

    @Override
    public void pruneUnreferencedFileInDataStore(MultiStatusHolder statusHolder) {
        // Nothing
    }

    @Override
    public void ping() {
        // Nothing
    }
}
