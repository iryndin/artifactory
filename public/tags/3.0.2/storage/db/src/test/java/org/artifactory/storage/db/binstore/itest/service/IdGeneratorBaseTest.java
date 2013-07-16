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

package org.artifactory.storage.db.binstore.itest.service;

import org.artifactory.storage.db.itest.DbBaseTest;
import org.artifactory.storage.db.util.IdGenerator;
import org.artifactory.storage.db.util.JdbcHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author mamo
 */
public abstract class IdGeneratorBaseTest extends DbBaseTest {
    @Autowired
    protected IdGenerator idGenerator;
    @Autowired
    protected JdbcHelper jdbcHelper;

    protected long getCurrentInMemoryId() {
        IdGenerator idGenerator = (IdGenerator) ReflectionTestUtils.getField(dbService, "idGenerator");
        return ((AtomicLong) ReflectionTestUtils.getField(idGenerator, "currentIndex")).get();
    }

    protected long getMaxReservedIndex() {
        IdGenerator idGenerator = (IdGenerator) ReflectionTestUtils.getField(dbService, "idGenerator");
        return (Long) ReflectionTestUtils.getField(idGenerator, "maxReservedIndex");
    }

    protected long getCurrentInTableId() {
        try (ResultSet rs = jdbcHelper.executeSelect(
                "SELECT current_id FROM unique_ids WHERE index_type = ?", IdGenerator.INDEX_TYPE_GENERAL)) {
            rs.next();
            return rs.getLong(1);
        } catch (SQLException e) {
            throw new RuntimeException("Could not get current_id from unique_ids", e);
        }
    }
}
