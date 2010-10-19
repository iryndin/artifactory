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
 */

package org.artifactory.jcr.utils;

import org.apache.jackrabbit.core.RepositoryImpl;
import org.apache.jackrabbit.core.config.PersistenceManagerConfig;
import org.apache.jackrabbit.core.config.RepositoryConfig;
import org.apache.jackrabbit.core.config.WorkspaceConfig;
import org.apache.jackrabbit.core.fs.FileSystem;
import org.apache.jackrabbit.core.fs.db.DatabaseFileSystem;
import org.apache.jackrabbit.core.persistence.bundle.ConnectionRecoveryManager;
import org.apache.jackrabbit.core.persistence.pool.DerbyPersistenceManager;
import org.apache.jackrabbit.core.util.db.ArtifactoryConnectionHelper;
import org.apache.jackrabbit.core.util.db.ConnectionHelper;
import org.artifactory.api.common.BasicStatusHolder;
import org.artifactory.jcr.JcrService;
import org.artifactory.jcr.JcrSession;
import org.artifactory.jcr.jackrabbit.ExtendedDbDataStore;
import org.artifactory.log.LoggerFactory;
import org.artifactory.spring.InternalContextHelper;
import org.slf4j.Logger;

import javax.jcr.RepositoryException;
import java.lang.reflect.Field;
import java.sql.*;
import java.util.Map;
import java.util.Properties;

/**
 * @author yoavl
 */
public abstract class DerbyUtils {
    private static final Logger log = LoggerFactory.getLogger(DerbyUtils.class);

    /**
     * http://db.apache.org/derby/docs/10.2/ref/rrefaltertablecompress.html
     */
    private static final String CALL_COMPRESS = "CALL SYSCS_UTIL.SYSCS_COMPRESS_TABLE(?, ?, ?)";

    /**
     * http://db.apache.org/derby/docs/10.2/ref/rrefproceduresinplacecompress.html
     */
    private static final String CALL_COMPRESS_INPLACE = "CALL SYSCS_UTIL.SYSCS_INPLACE_COMPRESS_TABLE(?, ?, ?, ?, ?)";

    private static final String PREFIX_PROPERTY_NAME = "schemaObjectPrefix";

    public static void compress(BasicStatusHolder holder) {
        JcrSession usession = getUnmanagedSession();
        try {
            RepositoryImpl repository = (RepositoryImpl) usession.getRepository();
            compressWorkspace(repository, holder);
            compressDataStore(repository, holder);
        } catch (Exception e) {
            holder.setError("Could not compress storage", e, log);
        } finally {
            usession.logout();
        }
    }

    public static boolean isDerbyUsed() {
        JcrSession usession = getUnmanagedSession();
        try {
            RepositoryImpl repository = (RepositoryImpl) usession.getRepository();
            return isDerbyDatastoreOrPms(repository);
        } catch (Exception e) {
            log.warn("Could not determine if the datastore is of type Derby DB.", e);
            return false;
        } finally {
            usession.logout();
        }
    }

    /**
     * Compresses the datastore tables (holding the blobs) TODO: Move this method to the DataStore implementation
     */
    private static void compressDataStore(RepositoryImpl repositoryImpl, BasicStatusHolder holder) throws Exception {
        log.info("Compressing datasource...");
        ExtendedDbDataStore dataStore = JcrUtils.getDataStore(repositoryImpl);

        if (dataStore == null || !isDerbyDb(dataStore.getDatabaseType())) {
            holder.setWarning("Cannot compress datastore: not using Derby DB.", log);
            return;
        }
        ArtifactoryConnectionHelper conHelper = dataStore.getConnectionHelper();
        Connection con = null;
        try {
            con = conHelper.takeConnection();
            con.setAutoCommit(true);

            DatabaseMetaData dbMetadata = con.getMetaData();
            ResultSet rs = dbMetadata.getTables(null, null, null, null);

            final String tableName = (dataStore.getDataStoreTablePrefix() + dataStore.getDataStoreTableName()).
                    toUpperCase();

            while (rs.next()) {
                String currentSchemaName = rs.getString("TABLE_SCHEM").toUpperCase();
                String currentTableName = rs.getString("TABLE_NAME").toUpperCase();
                if (currentTableName.equals(tableName)) {
                    executeCall(con, CALL_COMPRESS, currentSchemaName, currentTableName, 1);
                    executeCall(con, CALL_COMPRESS_INPLACE, currentSchemaName, currentTableName, 3);
                    log.debug("Datastore compressed successfully");
                }
            }
        } finally {
            conHelper.putConnection(con);
        }
    }

    /**
     * Compresses the workspace tables
     */
    private static void compressWorkspace(RepositoryImpl repositoryImpl, BasicStatusHolder holder) throws Exception {
        log.info("Compressing workspace...");
        RepositoryConfig repoConfig = repositoryImpl.getConfig();
        FileSystem fileSystem = repoConfig.getFileSystem();
        if (!(fileSystem instanceof DatabaseFileSystem)) {
            holder.setWarning("Repository itself not using a database file system!", log);
        }
        DatabaseFileSystem dbfs = (DatabaseFileSystem) fileSystem;
        String fsSchemaPrefix = dbfs.getSchemaObjectPrefix();
        fsSchemaPrefix = fsSchemaPrefix.toUpperCase();
        Map wsInfos = getWsInfos(repositoryImpl);
        for (Object workspaceInfoName : wsInfos.keySet()) {
            Object workspaceInfo = wsInfos.get(workspaceInfoName);
            PersistenceManagerConfig persistenceConfig = getPersistenceManagerConf(workspaceInfo);
            String pmSchemaPrefix = getProperty(persistenceConfig.getParameters(), PREFIX_PROPERTY_NAME);
            pmSchemaPrefix = pmSchemaPrefix.toUpperCase();

            ConnectionWrapper connectionWrapper = null;
            try {
                connectionWrapper = getWsConnection(workspaceInfo);
                if (connectionWrapper == null) {
                    holder.setWarning("Cannot compress workspace " + workspaceInfoName +
                            ": unable to retrieve connection.", log);
                    continue;
                }

                Connection connection = connectionWrapper.getConnection();
                DatabaseMetaData dbMetaData = connection.getMetaData();
                if (!isDerbyDb(dbMetaData.getDatabaseProductName())) {
                    holder.setWarning("Cannot compress workspace " + workspaceInfoName + ": not using Derby DB.", log);
                    continue;
                }
                ResultSet rs = dbMetaData.getTables(null, null, null, null);
                while (rs.next()) {
                    String currentSchemaName = rs.getString("TABLE_SCHEM").toUpperCase();
                    String currentTableName = rs.getString("TABLE_NAME").toUpperCase();
                    if ((currentTableName.startsWith(fsSchemaPrefix)) ||
                            (currentTableName.startsWith(pmSchemaPrefix))) {
                        executeCall(connection, CALL_COMPRESS, currentSchemaName, currentTableName, 1);
                        executeCall(connection, CALL_COMPRESS_INPLACE, currentSchemaName, currentTableName, 3);
                    }
                }
                connection.commit();
            } finally {
                if (connectionWrapper != null) {
                    connectionWrapper.close();
                }
            }
        }
        log.debug("Workspace compressed successfully");
    }

    private static JcrSession getUnmanagedSession() {
        JcrService jcrService = InternalContextHelper.get().getJcrService();
        JcrSession usession = jcrService.getUnmanagedSession();
        return usession;
    }

    /**
     * Executes the given call (does not commit)
     *
     * @param connection  Database connection
     * @param command     Command to execute
     * @param schemaName  Name of selected schema
     * @param tableName   Name of selected table
     * @param paramLength Length of short params needed for the command
     * @throws SQLException
     */
    private static void executeCall(Connection connection, String command, String schemaName, String tableName,
                                    int paramLength) throws SQLException {
        CallableStatement cs = connection.prepareCall(command);
        cs.setString(1, schemaName);
        cs.setString(2, tableName);
        paramLength += 3;
        for (int i = 3; i < paramLength; i++) {
            cs.setShort(i, (short) 1);
        }
        cs.execute();
    }

    /**
     * Checks the database product name, to make sure the one being called upon is Derby
     *
     * @return true if Derby datastore
     */
    private static boolean isDerbyDatastoreOrPms(RepositoryImpl repositoryImpl) throws Exception {
        //First check the datastore
        ExtendedDbDataStore dbDataStore = JcrUtils.getDataStore(repositoryImpl);
        boolean derby = dbDataStore != null && isDerbyDb(dbDataStore.getDatabaseType());
        if (derby) {
            return true;
        }
        //Check the pms
        Map wsInfos = getWsInfos(repositoryImpl);
        for (Object workspaceInfoName : wsInfos.keySet()) {
            Object workspaceInfo = wsInfos.get(workspaceInfoName);
            if ((workspaceInfo instanceof DerbyPersistenceManager) ||
                    (workspaceInfo instanceof org.apache.jackrabbit.core.persistence.bundle.DerbyPersistenceManager)) {
                ConnectionWrapper connectionWrapper = null;
                try {
                    connectionWrapper = getWsConnection(workspaceInfo);
                    if (connectionWrapper == null) {
                        return false;
                    }
                    Connection connection = connectionWrapper.getConnection();
                    DatabaseMetaData dbMetaData = connection.getMetaData();
                    if (isDerbyDb(dbMetaData.getDatabaseProductName())) {
                        return true;
                    }
                } finally {
                    if (connectionWrapper != null) {
                        connectionWrapper.close();
                    }
                }
            }
        }
        return false;
    }

    private static boolean isDerbyDb(String productIdentifier) {
        return productIdentifier != null && productIdentifier.toLowerCase().contains("derby");
    }

    /**
     * Returns a map with all the databases WorkspaceInfo objects
     *
     * @param repositoryImpl Jackrabbit repository object
     * @return A map of WorkspaceInfo objects
     * @throws NoSuchFieldException
     * @throws IllegalAccessException
     */
    private static Map getWsInfos(RepositoryImpl repositoryImpl) throws Exception {
        Field mapField = repositoryImpl.getClass().getDeclaredField("wspInfos");
        mapField.setAccessible(true);
        Map wspInfos = (Map) mapField.get(repositoryImpl);
        return wspInfos;
    }

    /**
     * Returns a jdbc connection object via a WorkspaceInfo object
     *
     * @param workspaceInfo Workspaceinfo object
     * @return Connection - JDBC Connection
     */
    private static ConnectionWrapper getWsConnection(Object workspaceInfo) throws Exception {
        Field persistenceManagerField = workspaceInfo.getClass().getDeclaredField("persistMgr");
        persistenceManagerField.setAccessible(true);

        Object persistenceManager = persistenceManagerField.get(workspaceInfo);
        Class clazz = persistenceManager.getClass().getSuperclass();

        ConnectionWrapper connectionWrapper = null;
        if (persistenceManager instanceof DerbyPersistenceManager) {

            Field connectionField = clazz.getDeclaredField("conHelper");
            connectionField.setAccessible(true);
            ConnectionHelper ch = (ConnectionHelper) connectionField.get(persistenceManager);
            ArtifactoryConnectionHelper helper = new ArtifactoryConnectionHelper(ch);
            connectionWrapper = ConnectionWrapper.getInstance(helper);
        } else if (persistenceManager instanceof
                org.apache.jackrabbit.core.persistence.bundle.DerbyPersistenceManager) {

            Field connectionField = clazz.getDeclaredField("connectionManager");
            connectionField.setAccessible(true);
            ConnectionRecoveryManager crm = (ConnectionRecoveryManager) connectionField.get(persistenceManager);
            connectionWrapper = ConnectionWrapper.getInstance(crm);
        }

        return connectionWrapper;
    }

    /**
     * Returns a Persistence manager configuration object via a WorkspaceInfo object
     *
     * @param workspaceInfo Workspaceinfo object
     * @return PersistenceManagerConfig - Persistence manager configuration
     * @throws NoSuchFieldException
     * @throws IllegalAccessException
     */
    private static PersistenceManagerConfig getPersistenceManagerConf(Object workspaceInfo)
            throws NoSuchFieldException, IllegalAccessException {
        Field configField = workspaceInfo.getClass().getDeclaredField("config");
        configField.setAccessible(true);
        WorkspaceConfig workspaceConfig = (WorkspaceConfig) configField.get(workspaceInfo);
        PersistenceManagerConfig persistenceConfig = workspaceConfig.getPersistenceManagerConfig();
        return persistenceConfig;
    }

    /**
     * Returns a property from a Properties object
     *
     * @param properties   A properties object
     * @param propertyName Name of property
     * @return String - property value
     */
    private static String getProperty(Properties properties, String propertyName) {
        String property = properties.getProperty(propertyName);
        if (property == null) {
            throw new IllegalArgumentException("Property: " + propertyName + " was not found!");
        }
        return property;
    }

    private static class ConnectionWrapper {

        private Connection connection;
        private ArtifactoryConnectionHelper artifactoryConnectionHelper;

        private ConnectionWrapper(ArtifactoryConnectionHelper artifactoryConnectionHelper) {
            this.connection = artifactoryConnectionHelper.takeConnection();
            this.artifactoryConnectionHelper = artifactoryConnectionHelper;
        }

        private ConnectionWrapper(ConnectionRecoveryManager connectionRecoveryManager)
                throws RepositoryException, SQLException {
            this.connection = connectionRecoveryManager.getConnection();
        }

        private static ConnectionWrapper getInstance(ArtifactoryConnectionHelper artifactoryConnectionHelper) {
            return new ConnectionWrapper(artifactoryConnectionHelper);
        }

        private static ConnectionWrapper getInstance(ConnectionRecoveryManager connectionRecoveryManager)
                throws RepositoryException, SQLException {
            return new ConnectionWrapper(connectionRecoveryManager);
        }

        private Connection getConnection() {
            return connection;
        }

        private void close() {
            if (artifactoryConnectionHelper != null) {
                artifactoryConnectionHelper.putConnection(connection);
            }
        }
    }
}