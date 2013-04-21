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

package org.artifactory.storage.db.fs.itest.service;

import org.artifactory.model.common.RepoPathImpl;
import org.artifactory.repo.RepoPath;
import org.artifactory.storage.db.itest.DbBaseTest;
import org.artifactory.storage.fs.service.TasksService;
import org.springframework.beans.factory.annotation.Autowired;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.Set;

import static org.testng.Assert.*;

/**
 * Tests the {@link org.artifactory.storage.fs.service.TasksService}
 *
 * @author Yossi Shaul
 */
@Test
public class TasksServiceImplTest extends DbBaseTest {

    @Autowired
    private TasksService tasksService;

    @BeforeClass
    public void setup() {
        importSql("/sql/nodes-for-service.sql");
    }

    public void getIndexTasks() {
        Set<RepoPath> checksumsToIndex = tasksService.getIndexTasks();
        assertNotNull(checksumsToIndex);
        assertEquals(checksumsToIndex.size(), 2);
        assertTrue(checksumsToIndex.contains(new RepoPathImpl("repo1:ant/ant/1.5/ant-1.5.jar")));
        assertTrue(checksumsToIndex.contains(new RepoPathImpl("reponone:ant/ant/1.5/ant-1.5.jar")));
    }

    public void hasIndexTask() {
        assertTrue(tasksService.hasIndexTask(new RepoPathImpl("repo1:ant/ant/1.5/ant-1.5.jar")));
    }

    @Test(dependsOnMethods = "getIndexTasks")
    public void addIndexTask() {
        RepoPathImpl repoPath = new RepoPathImpl("repo2:test");
        assertFalse(tasksService.hasIndexTask(repoPath));
        tasksService.addIndexTask(repoPath);
        assertTrue(tasksService.hasIndexTask(repoPath));
    }

    @Test(dependsOnMethods = "addIndexTask")
    public void removeIndexTask() {
        assertTrue(tasksService.removeIndexTask(new RepoPathImpl("repo2:test")));
        assertFalse(tasksService.hasIndexTask(new RepoPathImpl("repo2:test")));
    }

    @Test(dependsOnMethods = "hasIndexTask")
    public void removeIndexTaskByRepoPath() {
        assertTrue(tasksService.removeIndexTask(new RepoPathImpl("repo1:ant/ant/1.5/ant-1.5.jar")));
    }

    public void removeIndexTaskByRepoPathNotExist() {
        assertFalse(tasksService.removeIndexTask(new RepoPathImpl("nosuch:path.txt")));
    }
}
