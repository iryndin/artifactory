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

import org.apache.tomcat.jdbc.pool.DataSource;
import org.apache.tomcat.jdbc.pool.PoolProperties;
import org.artifactory.common.ConstantValues;
import org.artifactory.storage.StorageProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;

/**
 * A pooling data source based on tomcat-jdbc library.
 *
 * @author Yossi Shaul
 */
public class ArtifactoryTomcatDataSource extends DataSource implements ArtifactoryDataSource {
    private static final Logger log = LoggerFactory.getLogger(ArtifactoryTomcatDataSource.class);
    public ArtifactoryTomcatDataSource(StorageProperties s) {
        // see org.apache.tomcat.jdbc.pool.DataSourceFactory.parsePoolProperties()
        PoolProperties p = new PoolProperties();
        p.setUrl(s.getConnectionUrl());
        p.setDriverClassName(s.getDriverClass());
        p.setUsername(s.getUsername());
        p.setPassword(s.getPassword());

        p.setDefaultAutoCommit(false);
        p.setDefaultTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);

        p.setInitialSize(s.getIntProperty("initialSize", 1));
        p.setMaxAge(s.getIntProperty("maxAge", 0));
        p.setMaxActive(s.getMaxActiveConnections());
        p.setMaxWait(s.getIntProperty("maxWait", (int) TimeUnit.SECONDS.toMillis(120)));
        p.setMaxIdle(s.getMaxIdleConnections());
        p.setMinIdle(s.getIntProperty("minIdle", 1));
        p.setMinEvictableIdleTimeMillis(
                s.getIntProperty("minEvictableIdleTimeMillis", 300000));
        p.setTimeBetweenEvictionRunsMillis(
                s.getIntProperty("timeBetweenEvictionRunsMillis", 30000));
        p.setInitSQL(s.getProperty("initSQL", null));

        // validation query for all kind of tests (connect, borrow etc.)
        p.setValidationQuery(s.getProperty("validationQuery", getDefaultValidationQuery(s)));
        p.setValidationQueryTimeout(s.getIntProperty("validationQueryTimeout", 30));
        p.setValidationInterval(s.getLongProperty("validationInterval", 30000));
        p.setTestOnBorrow(s.getBooleanProperty("testOnBorrow", true));
        p.setTestWhileIdle(s.getBooleanProperty("testWhileIdle", false));
        p.setTestOnReturn(s.getBooleanProperty("testOnReturn", false));
        p.setTestOnConnect(s.getBooleanProperty("testOnConnect", false));

        p.setRemoveAbandoned(s.getBooleanProperty("removeAbandoned", false));
        p.setRemoveAbandonedTimeout(s.getIntProperty("removeAbandonedTimeout", 600));
        p.setSuspectTimeout(s.getIntProperty("suspectTimeout", 600));
        p.setLogAbandoned(s.getBooleanProperty("logAbandoned", false));
        p.setLogValidationErrors(s.getBooleanProperty("logValidationErrors", false));

        p.setJmxEnabled(s.getBooleanProperty("jmxEnabled", true));

        // only applicable if auto commit is false. has high performance penalty and only protects bugs in the code
        p.setRollbackOnReturn(s.getBooleanProperty("rollbackOnReturn", false));
        p.setCommitOnReturn(s.getBooleanProperty("commitOnReturn", false));

        p.setIgnoreExceptionOnPreLoad(s.getBooleanProperty("ignoreExceptionOnPreLoad", false));

        //p.setJdbcInterceptors(s.getProperty("jdbcInterceptors", "ConnectionState;StatementFinalizer"));
        p.setJdbcInterceptors(s.getProperty("jdbcInterceptors", null));

        p.setDefaultCatalog(s.getProperty("defaultCatalog", null));

        setPoolProperties(p);
    }

    @Override
    public int getActiveConnectionsCount() {
        return super.getActive();
    }

    @Override
    public int getIdleConnectionsCount() {
        return super.getIdle();
    }

    @Override
    public void close() {
        close(true);    // close all connections, including active ones
    }

    public static DataSource createUniqueIdDataSource(StorageProperties s) {
        // see org.apache.tomcat.jdbc.pool.DataSourceFactory.parsePoolProperties()
        PoolProperties p = new PoolProperties();
        p.setUrl(s.getConnectionUrl());
        p.setDriverClassName(s.getDriverClass());
        p.setUsername(s.getUsername());
        p.setPassword(s.getPassword());

        // auto commit is true for the unique id generator
        p.setDefaultAutoCommit(true);
        p.setDefaultTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);

        // only one connection is required for the id generator
        p.setInitialSize(0);
        p.setMinIdle(0);
        p.setMaxIdle(1);
        p.setMaxActive(1);
        // Make sure old idle connections are sweep and tested
        p.setTestWhileIdle(true);
        p.setTestOnBorrow(true);
        p.setTestWhileIdle(true);
        p.setRemoveAbandoned(true);
        p.setRemoveAbandonedTimeout((int) ConstantValues.locksTimeoutSecs.getLong()/2);
        p.setSuspectTimeout((int) ConstantValues.locksTimeoutSecs.getLong()/2);
        p.setLogAbandoned(true);
        p.setLogValidationErrors(true);

        // Timeout default to make sure new connection is created
        long timeoutInMillis = TimeUnit.SECONDS.toMillis(ConstantValues.locksTimeoutSecs.getLong());
        p.setMaxAge(timeoutInMillis);
        p.setMaxWait((int) timeoutInMillis);
        // Defaults values are good
        //p.setMinEvictableIdleTimeMillis(60000);
        //p.setTimeBetweenEvictionRunsMillis(5000);

        // Pool sweeper critical here since connection rarely used
        if (!p.isPoolSweeperEnabled()) {
            log.error("ID Generator pool connection should sweep idled connections");
        }

        // validation query for all kind of tests (connect, borrow etc.)
        p.setInitSQL(s.getProperty("initSQL", null));
        p.setValidationQuery(s.getProperty("validationQuery", getDefaultValidationQuery(s)));
        p.setValidationQueryTimeout(s.getIntProperty("validationQueryTimeout", 30));
        p.setValidationInterval(s.getLongProperty("validationInterval", 30000));

        p.setJmxEnabled(false);

        p.setIgnoreExceptionOnPreLoad(s.getBooleanProperty("ignoreExceptionOnPreLoad", false));

        //p.setJdbcInterceptors(s.getProperty("jdbcInterceptors", "ConnectionState;StatementFinalizer"));
        p.setJdbcInterceptors(s.getProperty("jdbcInterceptors", null));

        p.setDefaultCatalog(s.getProperty("defaultCatalog", null));

        return new DataSource(p);
    }

    private static String getDefaultValidationQuery(StorageProperties s) {
        switch (s.getDbType()) {
            case DERBY:
                return "values(1)";
            case MYSQL:
                return "/* ping */"; // special MySQL lightweight ping query
            case ORACLE:
                return "SELECT 1 FROM DUAL";
            default:
                return "SELECT 1";
        }
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        log.debug("Acquiring connection from pool");
        return super.getConnection(username, password);
    }

    public Connection getConnection() throws SQLException {
        log.debug("Acquiring connection from pool");
        Connection connection = super.getConnection();
        if (Connection.TRANSACTION_READ_COMMITTED != connection.getTransactionIsolation()) {
            connection.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
        }
        return connection;
    }
}
