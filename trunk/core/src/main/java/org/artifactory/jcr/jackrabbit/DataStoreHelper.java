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

import org.artifactory.log.LoggerFactory;
import org.slf4j.Logger;

import javax.jcr.RepositoryException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @author Yoav Landman
 * @date Jun 23, 2009
 */
abstract class DataStoreHelper {
    private static final Logger log = LoggerFactory.getLogger(DataStoreHelper.class);

    static final String getDbStoreSize = "select SUM(LENGTH) from DATASTORE";

    static long calcStorageSize(GenericConnectionRecoveryManager crm) throws RepositoryException {
        long totalSize = 0L;
        try {
            PreparedStatement prep = crm.executeStmt(getDbStoreSize, new Object[0]);
            ResultSet rs = prep.getResultSet();
            if (rs.next()) {
                totalSize = rs.getLong(1);
            }
            log.debug("Found total size of {} bytes.", totalSize);
        } catch (SQLException e) {
            throw new RepositoryException(e);
        } finally {
            crm.close();
        }
        return totalSize;
    }
}