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
import org.apache.commons.dbcp.DelegatingConnection;
import org.apache.commons.dbcp.PoolableConnectionFactory;
import org.apache.commons.pool.KeyedObjectPoolFactory;
import org.apache.commons.pool.ObjectPool;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;
import java.sql.Connection;

/**
 * This class inherits from {@link org.apache.commons.dbcp.PoolableConnectionFactory} just to override the
 * {@link org.apache.commons.dbcp.PoolableConnectionFactory#passivateObject(java.lang.Object)} method which calls
 * setAutoCommit(true) unnecessarily every time a connection is returned to the pool (this call goes to the database
 * and also when activated the framework will call setAutoCommit(false)).
 *
 * @author Yossi Shaul
 */
public class ArtifactoryPoolableConnectionFactory extends PoolableConnectionFactory {

    public ArtifactoryPoolableConnectionFactory(ConnectionFactory connFactory,
            ObjectPool pool, KeyedObjectPoolFactory stmtPoolFactory,
            String validationQuery, boolean defaultReadOnly, boolean defaultAutoCommit) {
        super(connFactory, pool, stmtPoolFactory, validationQuery, defaultReadOnly, defaultAutoCommit);
    }


    @Override
    public void passivateObject(Object obj) throws Exception {
        if (obj instanceof Connection) {
            Connection conn = (Connection) obj;
            if (!conn.getAutoCommit() && !conn.isReadOnly()) {
                conn.rollback();
            }
            conn.clearWarnings();
            //if(!conn.getAutoCommit()) {
            //    conn.setAutoCommit(true);
            //}
        }
        if (obj instanceof DelegatingConnection) {
            Method passivateMethod = ReflectionUtils.findMethod(DelegatingConnection.class, "passivate");
            passivateMethod.setAccessible(true);
            ReflectionUtils.invokeMethod(passivateMethod, obj);
        }
    }
}
