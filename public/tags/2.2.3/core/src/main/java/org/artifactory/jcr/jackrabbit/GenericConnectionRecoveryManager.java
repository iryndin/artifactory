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

package org.artifactory.jcr.jackrabbit;

import javax.jcr.RepositoryException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * @author freds
 * @date Jun 23, 2009
 */
public interface GenericConnectionRecoveryManager {
    /**
     * Gets the database connection that is managed. If the connection has been closed, and autoReconnect==true then an
     * attempt is made to reestablish the connection.
     *
     * @return the database connection that is managed
     * @throws SQLException        on error
     * @throws RepositoryException if the database driver could not be loaded
     */
    Connection getConnection() throws RepositoryException, SQLException;

    /**
     * Executes the given SQL statement with the specified parameters.
     *
     * @param sql    statement to execute
     * @param params parameters to set
     * @return the <code>Statement</code> object that had been executed
     * @throws SQLException        if an error occurs
     * @throws RepositoryException if the database driver could not be loaded
     */
    PreparedStatement executeStmt(String sql, Object[] params) throws RepositoryException, SQLException;

    /**
     * Closes all resources held by this {@link org.artifactory.jcr.jackrabbit.ArtifactoryConnectionRecoveryManager}. An
     * ongoing transaction is discarded.
     */
    void close();
}
