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

package org.artifactory.storage.db.spring;

import org.apache.commons.dbcp.ConnectionFactory;
import org.apache.commons.dbcp.DriverManagerConnectionFactory;
import org.apache.commons.dbcp.PoolableConnectionFactory;
import org.apache.commons.dbcp.PoolingDataSource;
import org.apache.commons.pool.ObjectPool;
import org.apache.commons.pool.impl.GenericObjectPool;
import org.artifactory.storage.StorageProperties;

import java.sql.Connection;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;

/**
 * A pooling data source wrapper that controls the connection pool(s) and exposed some of its state (used connections,
 * idle etc.)
 *
 * @author Yossi Shaul
 */
public class ArtifactoryDataSource extends PoolingDataSource {

    private final String connectionUrl;
    private final GenericObjectPool genericPool; //for now only used for mbean

    public ArtifactoryDataSource(StorageProperties storageProperties) {
        GenericObjectPool.Config poolConfig = new GenericObjectPool.Config();
        poolConfig.maxActive = storageProperties.getMaxActiveConnections();
        poolConfig.maxIdle = storageProperties.getMaxIdleConnections();
        ObjectPool connectionPool = new GenericObjectPool(null, poolConfig);

        ConnectionFactory connectionFactory = new DriverManagerConnectionFactory(
                connectionUrl = storageProperties.getConnectionUrl(),
                storageProperties.getUsername(), storageProperties.getPassword());

        PoolableConnectionFactory pcf = new ArtifactoryPoolableConnectionFactory(connectionFactory,
                connectionPool, null, null, false, false);
        pcf.setDefaultTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
        setPool(connectionPool);

        genericPool = (GenericObjectPool) _pool;
    }

    public ArtifactoryDataSource(String connectionUrl, GenericObjectPool genericPool) {
        this.connectionUrl = connectionUrl;
        this.genericPool = genericPool;
        setPool(genericPool);
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        throw new SQLFeatureNotSupportedException();
    }

    public void close() throws Exception {
        _pool.close();
    }

    //for now only used for mbean
    public int getActiveConnectionsCount() {
        return _pool.getNumActive();
    }

    public int getIdleConnectionsCount() {
        return _pool.getNumIdle();
    }

    public int getMaxActive() {
        return genericPool.getMaxActive();
    }

    public int getMaxIdle() {
        return genericPool.getMaxIdle();
    }

    public long getMaxWait() {
        return genericPool.getMaxWait();
    }

    public int getMinIdle() {
        return genericPool.getMinIdle();
    }

    public String getUrl() {
        return connectionUrl;
    }
}
