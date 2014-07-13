/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2012 JFrog Ltd.
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

package org.artifactory.storage.db.fs.itest.dao;

import org.artifactory.storage.db.fs.dao.StatsDao;
import org.artifactory.storage.db.fs.entity.Stat;
import org.artifactory.storage.db.itest.DbBaseTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.sql.SQLException;

import static org.testng.Assert.*;

/**
 * Tests {@link org.artifactory.storage.db.fs.dao.StatsDao}.
 *
 * @author Yossi Shaul
 */
public class StatsDaoTest extends DbBaseTest {

    @Autowired
    private StatsDao statsDao;

    @BeforeClass
    public void setup() {
        importSql("/sql/nodes.sql");
    }

    public void getFileStats() throws SQLException {
        Stat stat = statsDao.getStats(6);
        assertNotNull(stat);
        assertEquals(stat.getNodeId(), 6L);
        assertEquals(stat.getDownloadCount(), 15);
        assertEquals(stat.getLastDownloaded(), 1340283207850L);
        assertEquals(stat.getLastDownloadedBy(), "yossis");
    }

    public void getFileStatsWithNoDownloads() throws SQLException {
        assertNull(statsDao.getStats(11));
    }

    public void hasStatsFileWithStats() throws SQLException {
        assertTrue(statsDao.hasStats(6));
    }

    public void hasStatsFileWithoutStats() throws SQLException {
        assertFalse(statsDao.hasStats(11));
    }

    public void hasStatsFolder() throws SQLException {
        assertFalse(statsDao.hasStats(2), "Folders don't have stats");
    }

    public void hasStatsNonExistentId() throws SQLException {
        assertFalse(statsDao.hasStats(2375437583L));
    }

    public void createStatsFileWithoutStats() throws SQLException {
        long lastDownloaded = System.currentTimeMillis();
        int updateCount = statsDao.createStats(new Stat(12, 3, lastDownloaded, "yoyo"));
        assertEquals(updateCount, 1);
        Stat stats = statsDao.getStats(12);
        assertNotNull(stats);
        assertEquals(stats.getNodeId(), 12L);
        assertEquals(stats.getDownloadCount(), 3);
        assertEquals(stats.getLastDownloaded(), lastDownloaded);
        assertEquals(stats.getLastDownloadedBy(), "yoyo");
    }

    @Test(dependsOnMethods = "createStatsFileWithoutStats", expectedExceptions = SQLException.class)
    public void createStatsFileWithStats() throws SQLException {
        statsDao.createStats(new Stat(12, 5, System.currentTimeMillis(), "lolo"));
    }

    @Test(dependsOnMethods = "getFileStats")
    public void updateStatsFileWithStats() throws SQLException {
        long time = System.currentTimeMillis();
        int updateCount = statsDao.updateStats(new Stat(6, 23, time, "yoyo"));

        assertEquals(updateCount, 1);
        Stat stat = statsDao.getStats(6);
        assertNotNull(stat);
        assertEquals(stat.getNodeId(), 6L);
        assertEquals(stat.getDownloadCount(), 23);
        assertEquals(stat.getLastDownloaded(), time);
        assertEquals(stat.getLastDownloadedBy(), "yoyo");
    }

    public void updateStatsFileWithoutStats() throws SQLException {
        int updateCount = statsDao.updateStats(new Stat(11, 23, System.currentTimeMillis(), "yoyo"));
        assertEquals(updateCount, 0);
        assertNull(statsDao.getStats(11));
    }

    @Test(dependsOnMethods = "createStatsFileWithStats")
    public void deleteStatsFileWithStats() throws SQLException {
        assertEquals(statsDao.deleteStats(12), 1);
    }

    public void deleteStatsFileWithoutStats() throws SQLException {
        assertEquals(statsDao.deleteStats(13), 0);
    }

    public void deleteStatsNonExistingNode() throws SQLException {
        assertEquals(statsDao.deleteStats(343434), 0);
    }
}
