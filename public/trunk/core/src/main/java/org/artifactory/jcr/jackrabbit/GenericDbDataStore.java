/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2010 JFrog Ltd.
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
 *
 * Based on: /org/apache/jackrabbit/jackrabbit-core/1.6.0/jackrabbit-core-1.6.0.jar!
 * /org/apache/jackrabbit/core/data/DataStore.class
 */

package org.artifactory.jcr.jackrabbit;

import org.apache.jackrabbit.core.data.DataStore;

import javax.jcr.RepositoryException;

/**
 * @author freds
 * @date Mar 12, 2009
 */
public interface GenericDbDataStore extends DataStore {
    /**
     * Get the database type (if set).
     *
     * @return the database type
     */
    String getDatabaseType();

    /**
     * Create a new connection.
     *
     * @return the new connection
     */
    GenericConnectionRecoveryManager createNewConnection() throws RepositoryException;

    /**
     * Returns the total size in bytes of all stored objects
     *
     * @return
     */
    long getStorageSize() throws RepositoryException;
}
