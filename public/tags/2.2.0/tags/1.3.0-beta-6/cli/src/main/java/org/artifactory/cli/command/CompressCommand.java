package org.artifactory.cli.command;

import org.apache.jackrabbit.core.RepositoryImpl;
import org.apache.jackrabbit.core.config.FileSystemConfig;
import org.apache.jackrabbit.core.config.PersistenceManagerConfig;
import org.apache.jackrabbit.core.config.RepositoryConfig;
import org.apache.jackrabbit.core.config.WorkspaceConfig;
import org.apache.jackrabbit.core.data.db.DbDataStore;
import org.apache.jackrabbit.core.persistence.bundle.DerbyPersistenceManager;
import org.apache.jackrabbit.core.persistence.bundle.util.ConnectionRecoveryManager;
import org.artifactory.cli.common.BaseCommand;
import org.artifactory.cli.common.Command;
import org.artifactory.cli.main.CliOption;
import org.artifactory.cli.main.CommandDefinition;
import org.artifactory.common.ArtifactoryHome;
import org.artifactory.config.JcrConfResourceLoader;
import org.artifactory.update.jcr.JcrRepositoryForExport;

import javax.jcr.RepositoryException;
import java.io.IOException;
import java.lang.reflect.Field;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Properties;

/**
 * The "Compress" command class. If artifactory is using a Derby database, this command will call
 * the compress table procedure. This command is available to Derby only.
 *
 * @author Noam Tenne
 */
public class CompressCommand extends BaseCommand implements Command {
    private final String COMPRESS_COMMAND = "CALL SYSCS_UTIL.SYSCS_COMPRESS_TABLE(?, ?, ?)";
    private final String PREFIX_PROPERTY_NAME = "schemaObjectPrefix";

    /**
     * Constructor
     */
    public CompressCommand() {
        super(CommandDefinition.compress, CliOption.url, CliOption.username, CliOption.password);
    }

    /**
     * Executes the command
     */
    public void execute() throws Exception {
        compress();
    }

    /**
     * Prints the command usage
     */
    public void usage() {
        defaultUsage();
    }

    /**
     * Calls the compress command on the workspace and datastore tables
     *
     * @throws IOException
     * @throws NoSuchMethodException
     */
    private void compress() throws IOException, NoSuchMethodException {
        ArtifactoryHome.setReadOnly(true);
        findAndSetArtifactoryHome();
        ArtifactoryHome.create();
        JcrRepositoryForExport repositoryForExport = new JcrRepositoryForExport();
        JcrConfResourceLoader confResourceLoader = new JcrConfResourceLoader("repo.xml");
        repositoryForExport.setRepoXml(confResourceLoader);
        RepositoryImpl repositoryImpl =
                ((RepositoryImpl) repositoryForExport.createJcrRepository());
        try {
            compressWorkspace(repositoryImpl);
            compressDataStore(repositoryImpl);
        } catch (NoSuchFieldException e) {
            throw new IllegalArgumentException(e.getMessage());
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    /**
     * Compresses the datastore tables
     *
     * @param repositoryImpl The repository object of artifactory
     * @throws RepositoryException
     * @throws SQLException
     */
    private void compressDataStore(RepositoryImpl repositoryImpl)
            throws RepositoryException, SQLException {
        DbDataStore dataStore = ((DbDataStore) repositoryImpl.getDataStore());
        if (canCompress(dataStore.getDatabaseType())) {
            ConnectionRecoveryManager crm = dataStore.createNewConnection();
            Connection connection = crm.getConnection();
            CallableStatement cs = connection.prepareCall
                    (COMPRESS_COMMAND);
            cs.setString(1, "APP");
            cs.setString(2, "DATASTORE");
            cs.setShort(3, (short) 1);
            cs.execute();
            connection.commit();
        }
    }

    /**
     * Compresses the workspace tables
     *
     * @param repositoryImpl The repository object of artifactory
     * @throws NoSuchFieldException
     * @throws IllegalAccessException
     * @throws RepositoryException
     * @throws SQLException
     */
    private void compressWorkspace(RepositoryImpl repositoryImpl)
            throws NoSuchFieldException, IllegalAccessException, RepositoryException, SQLException {
        HashMap wsInfos = getWsInfos(repositoryImpl);
        RepositoryConfig repoConfig = repositoryImpl.getConfig();
        FileSystemConfig fileSystemConfig = repoConfig.getFileSystemConfig();
        String fsSchemaPrefix = getProperty(fileSystemConfig.getParameters(), PREFIX_PROPERTY_NAME);
        fsSchemaPrefix = fsSchemaPrefix.toUpperCase();
        for (Object workspaceInfo : wsInfos.values()) {
            PersistenceManagerConfig persistenceConfig = getPersistenceManagerConf(workspaceInfo);
            String pmSchemaPrefix =
                    getProperty(persistenceConfig.getParameters(), PREFIX_PROPERTY_NAME);
            pmSchemaPrefix = pmSchemaPrefix.toUpperCase();
            Connection connection = getWsConnection(workspaceInfo);
            DatabaseMetaData dbMetaData = connection.getMetaData();
            if (canCompress(dbMetaData.getDatabaseProductName())) {
                ResultSet rs = dbMetaData.getTables(null, null, null, null);
                while (rs.next()) {
                    String currentSchemaName = rs.getString("TABLE_SCHEM").toUpperCase();
                    String currentTableName = rs.getString("TABLE_NAME").toUpperCase();
                    if ((currentTableName.startsWith(fsSchemaPrefix)) ||
                            (currentTableName.startsWith(pmSchemaPrefix))) {
                        CallableStatement cs = connection.prepareCall
                                (COMPRESS_COMMAND);
                        cs.setString(1, currentSchemaName);
                        cs.setString(2, currentTableName);
                        cs.setShort(3, (short) 1);
                        cs.execute();
                    }
                }
                connection.commit();
            }
        }
    }

    /**
     * Checks the database product name, to make sure the one being called upon is Derby
     *
     * @param productIdentifier The database type (Derby, Oracle, mySQL)
     * @return
     */
    private boolean canCompress(String productIdentifier) {
        productIdentifier = productIdentifier.toLowerCase();
        if (!productIdentifier.contains("derby")) {
            throw new UnsupportedOperationException(
                    "The 'compress' command is available only for Apache Derby databases");
        }
        return true;
    }

    /**
     * Returns a hashmap with all the databases WorkspaceInfo objects
     *
     * @param repositoryImpl Jackrabbit repository object
     * @return HashMap - Map of WorkspaceInfo objects
     * @throws NoSuchFieldException
     * @throws IllegalAccessException
     */
    private HashMap getWsInfos(RepositoryImpl repositoryImpl)
            throws NoSuchFieldException, IllegalAccessException {
        Field mapField = repositoryImpl.getClass().getDeclaredField("wspInfos");
        mapField.setAccessible(true);
        HashMap wspInfos = (HashMap) mapField.get(repositoryImpl);
        return wspInfos;
    }

    /**
     * Returns a jdbc connection object via a WorkspaceInfo object
     *
     * @param workspaceInfo Workspaceinfo object
     * @return Connection - JDBC Connection
     * @throws NoSuchFieldException
     * @throws IllegalAccessException
     * @throws SQLException
     * @throws RepositoryException
     */
    private Connection getWsConnection(Object workspaceInfo)
            throws NoSuchFieldException, IllegalAccessException, SQLException, RepositoryException {
        Field persistenceManagerField = workspaceInfo.getClass().getDeclaredField("persistMgr");
        persistenceManagerField.setAccessible(true);
        DerbyPersistenceManager persistenceManager =
                ((DerbyPersistenceManager) persistenceManagerField.get(workspaceInfo));
        Class clazz = persistenceManager.getClass().getSuperclass();
        Field connectionField = clazz.getDeclaredField("connectionManager");
        connectionField.setAccessible(true);
        ConnectionRecoveryManager crm =
                (ConnectionRecoveryManager) connectionField.get(persistenceManager);
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
    private PersistenceManagerConfig getPersistenceManagerConf(Object workspaceInfo)
            throws NoSuchFieldException, IllegalAccessException {
        Field configField = workspaceInfo.getClass().getDeclaredField("config");
        configField.setAccessible(true);
        WorkspaceConfig workspaceConfig = (WorkspaceConfig) configField.get(workspaceInfo);
        PersistenceManagerConfig persistenceConfig =
                workspaceConfig.getPersistenceManagerConfig();
        return persistenceConfig;
    }

    /**
     * Returns a property from a Properties object
     *
     * @param properties   A properties object
     * @param propertyName Name of property
     * @return String - property value
     */
    private String getProperty(Properties properties, String propertyName) {
        String property = properties.getProperty(propertyName);
        if (property == null) {
            throw new IllegalArgumentException("Property: " + propertyName + " was not found!");
        }
        return property;
    }
}
