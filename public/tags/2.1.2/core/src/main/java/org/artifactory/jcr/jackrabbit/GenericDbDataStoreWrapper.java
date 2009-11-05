/*
 * This file is part of Artifactory.
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
import org.apache.jackrabbit.core.data.DataRecord;
import org.apache.jackrabbit.core.data.DataStoreException;
import org.apache.jackrabbit.core.data.db.DbDataStore;

import javax.jcr.RepositoryException;
import java.io.InputStream;
import java.util.Iterator;

/**
 * A generic db datastore wrapper
 *
 * @author freds
 * @date Jun 23, 2009
 */
public class GenericDbDataStoreWrapper implements GenericDbDataStore {

    private final DbDataStore store;

    public GenericDbDataStoreWrapper(DbDataStore store) {
        this.store = store;
    }

    public DataRecord addRecord(InputStream stream) throws DataStoreException {
        return store.addRecord(stream);
    }

    public int deleteAllOlderThan(long min) throws DataStoreException {
        return store.deleteAllOlderThan(min);
    }

    public Iterator getAllIdentifiers() throws DataStoreException {
        return store.getAllIdentifiers();
    }

    public int getMinRecordLength() {
        return store.getMinRecordLength();
    }

    public void setMinRecordLength(int minRecordLength) {
        store.setMinRecordLength(minRecordLength);
    }

    public DataRecord getRecord(DataIdentifier identifier) throws DataStoreException {
        return store.getRecord(identifier);
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

    public void setDatabaseType(String databaseType) {
        store.setDatabaseType(databaseType);
    }

    public String getDriver() {
        return store.getDriver();
    }

    public void setDriver(String driver) {
        store.setDriver(driver);
    }

    public String getPassword() {
        return store.getPassword();
    }

    public void setPassword(String password) {
        store.setPassword(password);
    }

    public String getUrl() {
        return store.getUrl();
    }

    public void setUrl(String url) {
        store.setUrl(url);
    }

    public String getUser() {
        return store.getUser();
    }

    public void setUser(String user) {
        store.setUser(user);
    }

    public void close() {
        store.close();
    }

    public void clearInUse() {
        store.clearInUse();
    }

    public int getMaxConnections() {
        return store.getMaxConnections();
    }

    public void setMaxConnections(int maxConnections) {
        store.setMaxConnections(maxConnections);
    }

    public GenericConnectionRecoveryManager createNewConnection() throws RepositoryException {
        return new GenericConnectionRecoveryManagerWrapper(store.createNewConnection());
    }

    public long getStorageSize() throws RepositoryException {
        GenericConnectionRecoveryManager crm = createNewConnection();
        return DataStoreHelper.calcStorageSize(crm);
    }

    public boolean getCopyWhenReading() {
        return store.getCopyWhenReading();
    }

    public void setCopyWhenReading(boolean copyWhenReading) {
        store.setCopyWhenReading(copyWhenReading);
    }

    public String getTablePrefix() {
        return store.getTablePrefix();
    }

    public void setTablePrefix(String tablePrefix) {
        store.setTablePrefix(tablePrefix);
    }

}
