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

import org.apache.jackrabbit.core.persistence.bundle.util.ConnectionRecoveryManager;

import javax.jcr.RepositoryException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * @author freds
 * @date Jun 23, 2009
 */
public class GenericConnectionRecoveryManagerWrapper implements GenericConnectionRecoveryManager {
    private final ConnectionRecoveryManager connection;

    public GenericConnectionRecoveryManagerWrapper(ConnectionRecoveryManager connection) {
        this.connection = connection;
    }

    public Connection getConnection() throws RepositoryException, SQLException {
        return connection.getConnection();
    }

    public PreparedStatement executeStmt(String records, Object[] objects) throws RepositoryException, SQLException {
        return connection.executeStmt(records, objects);
    }

    public void close() {
        connection.close();
    }
}
