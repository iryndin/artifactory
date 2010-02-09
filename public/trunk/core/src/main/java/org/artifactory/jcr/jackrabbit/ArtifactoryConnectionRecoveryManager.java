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
 * /org/apache/jackrabbit/core/data/AbstractDataRecord.class
 */

package org.artifactory.jcr.jackrabbit;

import org.apache.jackrabbit.core.persistence.bundle.util.ConnectionFactory;
import org.artifactory.log.LoggerFactory;
import org.slf4j.Logger;

import javax.jcr.RepositoryException;
import java.io.InputStream;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;

/**
 * This class provides methods to get a database connection and to execute SQL statements. It also contains reconnection
 * logic. If the connection has been closed with the {@link #close()} method, then  a call to any public method except
 * for {@link #setAutoReconnect(boolean)} will try to reestablish the connection, but only if the
 * <code>autoReconnect</code> equals <code>true</code>.
 * <p/>
 * The reconnection attempt can either be blocking or non-blocking, which is configured during construction. In the
 * latter case a fixed number of reconnection attempts is made. When the reconnection failed an SQLException is thrown.
 * <p/>
 * The methods of this class that execute SQL statements automatically call {@link #close()} when they encounter an
 * SQLException.
 */
public class ArtifactoryConnectionRecoveryManager implements GenericConnectionRecoveryManager {

    /**
     * The default logger.
     */
    private static Logger log = LoggerFactory.getLogger(ArtifactoryConnectionRecoveryManager.class);

    /**
     * The database driver.
     */
    private final String driver;

    /**
     * The database URL.
     */
    private final String url;

    /**
     * The database user.
     */
    private final String user;

    /**
     * The database password.
     */
    private final String password;

    /**
     * The database connection that is managed by this {@link ArtifactoryConnectionRecoveryManager}.
     */
    private Connection connection;

    /**
     * An internal flag governing whether an automatic reconnect should be attempted after a SQLException had been
     * encountered in {@link #executeStmt(String, Object[])}.
     */
    private boolean autoReconnect = true;

    /**
     * Indicates whether the reconnection function should block until the connection is up again.
     */
    private final boolean block;

    /**
     * Time to sleep in ms before a reconnect is attempted.
     */
    private static final int SLEEP_BEFORE_RECONNECT = 500;

    /**
     * Number of reconnection attempts per method call. Only used if <code>block == false</code>.
     */
    public static final int TRIALS = 20;

    /**
     * The map of prepared statements (key: SQL stmt, value: prepared stmt).
     */
    private Map<String, PreparedStatement> preparedStatements = new HashMap<String, PreparedStatement>();

    /**
     * Indicates whether the managed connection is open or closed.
     */
    private boolean isClosed;

    /**
     * Creates a {@link ArtifactoryConnectionRecoveryManager} and establishes a database Connection using the driver,
     * user, password and url arguments.
     * <p/>
     * By default, the connection is in auto-commit mode, and this manager tries to reconnect if the connection is
     * lost.
     *
     * @param block    whether this class should block until the connection can be recovered
     * @param driver   the driver to use for the connection
     * @param url      the url to use for the connection
     * @param user     the user to use for the connection
     * @param password the password to use for the connection
     * @throws javax.jcr.RepositoryException if the database driver could not be loaded
     */
    public ArtifactoryConnectionRecoveryManager(boolean block, String driver, String url, String user, String password)
            throws RepositoryException {
        this.block = block;
        this.driver = driver;
        this.url = url;
        this.user = user;
        this.password = password;
        try {
            setupConnection();
            isClosed = false;
        } catch (SQLException e) {
            logException("could not setup connection", e);
            close();
        }
    }

    /**
     * Gets the database connection that is managed. If the connection has been closed, and autoReconnect==true then an
     * attempt is made to reestablish the connection.
     *
     * @return the database connection that is managed
     * @throws java.sql.SQLException         on error
     * @throws javax.jcr.RepositoryException if the database driver could not be loaded
     */
    public synchronized Connection getConnection() throws SQLException, RepositoryException {
        if (isClosed) {
            if (autoReconnect) {
                reestablishConnection();
            } else {
                throw new SQLException("connection has been closed and autoReconnect == false");
            }
        }
        return connection;
    }

    /**
     * Starts a transaction. I.e., the auto-commit is set to false, and the manager does not try to reconnect if the
     * connection is lost. This method call should be followed by a call to <code>endTransaction</code>.
     *
     * @throws java.sql.SQLException on error
     */
    public synchronized void setAutoReconnect(boolean autoReconnect) {
        this.autoReconnect = autoReconnect;
    }

    /**
     * Executes the given SQL statement with the specified parameters.
     *
     * @param sql    statement to execute
     * @param params parameters to set
     * @return the <code>Statement</code> object that had been executed
     * @throws java.sql.SQLException         if an error occurs
     * @throws javax.jcr.RepositoryException if the database driver could not be loaded
     */
    public PreparedStatement executeStmt(String sql, Object[] params) throws SQLException, RepositoryException {
        return executeStmt(sql, params, false, 0);
    }

    /**
     * Executes the given SQL statement with the specified parameters.
     *
     * @param sql                 statement to execute
     * @param params              parameters to set
     * @param returnGeneratedKeys if the statement should return auto generated keys
     * @param maxRows             the maximum number of rows to return (0 for all rows)
     * @return the <code>Statement</code> object that had been executed
     * @throws java.sql.SQLException         if an error occurs
     * @throws javax.jcr.RepositoryException if the database driver could not be loaded
     */
    public synchronized PreparedStatement executeStmt(String sql, Object[] params, boolean returnGeneratedKeys,
                                                      int maxRows) throws SQLException, RepositoryException {
        int trials = 2;
        SQLException lastException;
        // Check if one param is a stream, then don't do retries to get immediate exception on save
        // The stream need to be reset between retries or part of the stream will be saved in DB!!!
        for (int i = 0; params != null && i < params.length; i++) {
            Object p = params[i];
            if ((p instanceof StreamWrapper) || (p instanceof InputStream)) {
                trials = 1;
                break;
            }
        }

        do {
            trials--;
            try {
                return executeStmtInternal(sql, params, returnGeneratedKeys, maxRows);
            } catch (SQLException e) {
                lastException = e;
            }
        } while (autoReconnect && (block || trials > 0));
        throw lastException;
    }

    /**
     * Executes the given SQL statement with the specified parameters.
     *
     * @param sql                 statement to execute
     * @param params              parameters to set
     * @param returnGeneratedKeys if the statement should return auto generated keys
     * @param maxRows             the maximum number of rows to return (0 for all rows)
     * @return the <code>Statement</code> object that had been executed
     * @throws java.sql.SQLException         if an error occurs
     * @throws javax.jcr.RepositoryException if the database driver could not be loaded
     */
    private PreparedStatement executeStmtInternal(String sql, Object[] params, boolean returnGeneratedKeys, int maxRows)
            throws SQLException, RepositoryException {
        try {
            String key = sql;
            if (returnGeneratedKeys) {
                key += " RETURN_GENERATED_KEYS";
            }
            PreparedStatement stmt = preparedStatements.get(key);
            if (stmt == null) {
                if (returnGeneratedKeys) {
                    stmt = getConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
                } else {
                    stmt = getConnection().prepareStatement(sql);
                }
                preparedStatements.put(key, stmt);
            }
            stmt.setMaxRows(maxRows);
            return executeStmtInternal(params, stmt);
        } catch (SQLException e) {
            logException("could not execute statement", e);
            close();
            throw e;
        }
    }

    /**
     * Closes all resources held by this {@link ArtifactoryConnectionRecoveryManager}. An ongoing transaction is
     * discarded.
     */
    public synchronized void close() {
        preparedStatements.clear();
        try {
            if (connection != null) {
                if (!connection.getAutoCommit()) {
                    connection.rollback();
                }
                connection.close();
            }
        } catch (SQLException e) {
            logException("failed to close connection", e);
        }
        connection = null;
        isClosed = true;
    }

    /**
     * Creates the database connection.
     *
     * @throws java.sql.SQLException         on error
     * @throws javax.jcr.RepositoryException if the database driver could not be loaded
     */
    private void setupConnection() throws SQLException, RepositoryException {
        try {
            connection = ConnectionFactory.getConnection(driver, url, user, password);
        } catch (SQLException e) {
            log.warn("Could not connect; driver: " + driver + " url: " + url + " user: " + user + " error: " +
                    e.toString(), e);
            throw e;
        }
        // JCR-1013: Setter may fail unnecessarily on a managed connection
        if (!connection.getAutoCommit()) {
            connection.setAutoCommit(true);
        }
        try {
            DatabaseMetaData meta = connection.getMetaData();
            log.debug("Database: " + meta.getDatabaseProductName() + " / " + meta.getDatabaseProductVersion());
            log.debug("Driver: " + meta.getDriverName() + " / " + meta.getDriverVersion());
        } catch (SQLException e) {
            log.warn("Can not retrieve database and driver name / version", e);
        }
    }

    /**
     * @param params the parameters for the <code>stmt</code> parameter
     * @param stmt   the statement to execute
     * @return the executed Statement
     * @throws java.sql.SQLException on error
     */
    private PreparedStatement executeStmtInternal(Object[] params, PreparedStatement stmt) throws SQLException {
        for (int i = 0; params != null && i < params.length; i++) {
            Object p = params[i];
            if (p instanceof StreamWrapper) {
                StreamWrapper wrapper = (StreamWrapper) p;
                stmt.setBinaryStream(i + 1, wrapper.stream, (int) wrapper.size);
            } else if (p instanceof InputStream) {
                InputStream stream = (InputStream) p;
                stmt.setBinaryStream(i + 1, stream, -1);
            } else {
                stmt.setObject(i + 1, p);
            }
        }
        stmt.execute();
        resetStatement(stmt);
        return stmt;
    }

    /**
     * Re-establishes the database connection.
     *
     * @throws java.sql.SQLException         if reconnecting failed
     * @throws javax.jcr.RepositoryException
     */
    private void reestablishConnection() throws SQLException, RepositoryException {

        long trials = TRIALS;
        SQLException exception = null;

        // Close the connection (might already have been done)
        close();

        if (block) {
            log.warn("blocking until database connection is up again...");
        }

        // Try to reconnect
        while (trials-- >= 0 || block) {

            // Reset the last caught exception
            exception = null;

            // Sleep for a while to give database a chance
            // to restart before a reconnect is attempted.
            try {
                Thread.sleep(SLEEP_BEFORE_RECONNECT);
            } catch (InterruptedException ignore) {
            }

            // now try to re-establish connection
            try {
                setupConnection();
                isClosed = false;
                break;
            } catch (SQLException e) {
                exception = e;
                close();
            }
        }

        // Rethrow last caught exception (if this is not null, then
        // we know that reconnecting failed and close has been called.
        if (exception != null) {
            throw exception;
        } else if (block) {
            log.warn("database connection is up again!");
        }
    }

    /**
     * Resets the given <code>PreparedStatement</code> by clearing the parameters and warnings contained.
     *
     * @param stmt The <code>PreparedStatement</code> to reset. If <code>null</code> this method does nothing.
     */
    private void resetStatement(PreparedStatement stmt) {
        if (stmt != null) {
            try {
                stmt.clearParameters();
                stmt.clearWarnings();
            } catch (SQLException se) {
                logException("Failed resetting PreparedStatement", se);
            }
        }
    }

    /**
     * Logs an sql exception.
     *
     * @param message the message
     * @param se      the exception
     */
    private void logException(String message, SQLException se) {
        message = message == null ? "" : message;
        log.error(message + ", reason: " + se.getMessage() + ", state/code: "
                + se.getSQLState() + "/" + se.getErrorCode());
        log.debug("   dump:", se);
    }

    /**
     * A wrapper for a binary stream that includes the size of the stream.
     */
    public static class StreamWrapper {

        private final InputStream stream;
        private final long size;

        /**
         * Creates a wrapper for the given InputStream that can safely be passed as a parameter to the
         * <code>executeStmt</code> methods in the {@link ArtifactoryConnectionRecoveryManager} class.
         *
         * @param in   the InputStream to wrap
         * @param size the size of the input stream
         */
        public StreamWrapper(InputStream in, long size) {
            this.stream = in;
            this.size = size;
        }
    }

    public void closeSilently(ResultSet rs) {
        if (rs != null) {
            try {
                rs.close();
            } catch (SQLException e) {
                // ignore
            }
        }
    }
}