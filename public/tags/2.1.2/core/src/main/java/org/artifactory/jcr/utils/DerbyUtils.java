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

package org.artifactory.jcr.utils;

import org.apache.jackrabbit.core.RepositoryImpl;
import org.apache.jackrabbit.core.config.PersistenceManagerConfig;
import org.apache.jackrabbit.core.config.RepositoryConfig;
import org.apache.jackrabbit.core.config.WorkspaceConfig;
import org.apache.jackrabbit.core.fs.FileSystem;
import org.apache.jackrabbit.core.fs.db.DatabaseFileSystem;
import org.apache.jackrabbit.core.persistence.bundle.DerbyPersistenceManager;
import org.apache.jackrabbit.core.persistence.bundle.util.ConnectionRecoveryManager;
import org.artifactory.api.common.StatusHolder;
import org.artifactory.jcr.JcrService;
import org.artifactory.jcr.JcrSession;
import org.artifactory.jcr.jackrabbit.GenericConnectionRecoveryManager;
import org.artifactory.jcr.jackrabbit.GenericDbDataStore;
import org.artifactory.log.LoggerFactory;
import org.artifactory.spring.InternalContextHelper;
import org.slf4j.Logger;

import java.lang.reflect.Field;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
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

    public static void compress(StatusHolder holder) {
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
            log.error("Could not determine if the datastore is of type Derby DB", e, log);
            return false;
        } finally {
            usession.logout();
        }
    }


    /**
     * Compresses the datastore tables (holding the blobs) TODO: Move this metohd to the DataStore implementation
     */
    private static void compressDataStore(RepositoryImpl repositoryImpl, StatusHolder holder) throws Exception {
        log.info("Compressing datasource...");
        GenericDbDataStore dataStore = JcrUtils.getDataStore(repositoryImpl);
        if (dataStore == null || !isDerbyDb(dataStore.getDatabaseType())) {
            holder.setWarning("Cannot compress datastore: not using Derby DB.", log);
            return;
        }
        GenericConnectionRecoveryManager crm = dataStore.createNewConnection();
        try {
            Connection connection = crm.getConnection();
            connection.setAutoCommit(true);
            final String schemaName = "APP";
            final String tableName = "DATASTORE";
            executeCall(connection, CALL_COMPRESS, schemaName, tableName, 1);
            executeCall(connection, CALL_COMPRESS_INPLACE, schemaName, tableName, 3);
            log.debug("Datastore compressed successfully");
        } finally {
            crm.close();
        }
    }

    /**
     * Compresses the workspace tables
     */
    private static void compressWorkspace(RepositoryImpl repositoryImpl, StatusHolder holder) throws Exception {
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
            Connection connection = getWsConnection(workspaceInfo);
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
        GenericDbDataStore dbDataStore = JcrUtils.getDataStore(repositoryImpl);
        boolean derby = dbDataStore != null && isDerbyDb(dbDataStore.getDatabaseType());
        if (derby) {
            return true;
        }
        //Check the pms
        Map wsInfos = getWsInfos(repositoryImpl);
        for (Object workspaceInfoName : wsInfos.keySet()) {
            Object workspaceInfo = wsInfos.get(workspaceInfoName);
            Connection connection = getWsConnection(workspaceInfo);
            DatabaseMetaData dbMetaData = connection.getMetaData();
            if (isDerbyDb(dbMetaData.getDatabaseProductName())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isDerbyDb(String productIdentifier) {
        return productIdentifier != null && productIdentifier.toLowerCase().contains("derby");
    }

    /**
     * Returns a hashmap with all the databases WorkspaceInfo objects
     *
     * @param repositoryImpl Jackrabbit repository object
     * @return HashMap - Map of WorkspaceInfo objects
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
    private static Connection getWsConnection(Object workspaceInfo) throws Exception {
        Field persistenceManagerField = workspaceInfo.getClass().getDeclaredField("persistMgr");
        persistenceManagerField.setAccessible(true);
        DerbyPersistenceManager persistenceManager =
                ((DerbyPersistenceManager) persistenceManagerField.get(workspaceInfo));
        Class clazz = persistenceManager.getClass().getSuperclass();
        Field connectionField = clazz.getDeclaredField("connectionManager");
        connectionField.setAccessible(true);
        ConnectionRecoveryManager crm = (ConnectionRecoveryManager) connectionField.get(persistenceManager);
        Connection connection = crm.getConnection();
        return connection;
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
}