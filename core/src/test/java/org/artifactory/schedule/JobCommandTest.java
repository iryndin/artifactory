/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2011 JFrog Ltd.
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

package org.artifactory.schedule;

import org.artifactory.api.security.SecurityService;
import org.artifactory.api.security.UserInfo;
import org.springframework.test.util.ReflectionTestUtils;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * @author freds
 */
@Test(sequential = true)
public class JobCommandTest extends TaskServiceTestBase {

    @Test
    public void testCommandToStop() throws Exception {
        ArtifactoryHomeTaskTestStub homeStub = getOrCreateArtifactoryHomeStub();
        TaskBase task1 = createRunAndPauseDummyA();
        ArtifactoryHomeTaskTestStub.TaskTestData task1Data = homeStub.getTaskData(task1.getToken());

        taskService.resumeTask(task1.getToken());
        Thread.sleep(500);
        taskService.pauseTask(task1.getToken(), true);
        task1Data.assertNbs(1, 0);
        Assert.assertTrue(task1Data.taskStoppedOnExecute.isEmpty());
        Assert.assertTrue(task1Data.taskStoppedAfterExecute.isEmpty());
        Assert.assertEquals(task1Data.userNameOnExecute, SecurityService.USER_SYSTEM);

        // Run while the other waiting
        TaskBase task2 = createManualDummyB(task1.getToken());
        ArtifactoryHomeTaskTestStub.TaskTestData task2Data = homeStub.getTaskData(task2.getToken());
        Assert.assertTrue(taskService.resumeTask(task2.getToken()));
        Thread.sleep(300);
        task2Data.assertNbs(1, 0);
        Assert.assertEquals(task2Data.taskStoppedAfterExecute.size(), 0);

        taskService.stopTask(task1.getToken(), true);
        task2Data.assertNbs(1, 0);
        task1Data.assertNbs(1, 0);
        taskService.cancelTask(task1.getToken(), true);
        Assert.assertNull(taskService.getInternalActiveTask(task1.getToken()));
        Assert.assertNull(taskService.getInternalActiveTask(task2.getToken()));
    }

    @Test
    public void testSingletonAndManual() throws Exception {
        ArtifactoryHomeTaskTestStub homeStub = getOrCreateArtifactoryHomeStub();
        TaskBase task1 = createRunAndPauseDummyA();
        try {
            createRunAndPauseDummyA();
            Assert.fail("Should get an IllegalStateException trying to start 2 singleton tasks");
        } catch (IllegalStateException e) {
        }
        ArtifactoryHomeTaskTestStub.TaskTestData task1Data = homeStub.getTaskData(task1.getToken());
        taskService.resumeTask(task1.getToken());
        Assert.assertTrue(task1.isRunning());
        try {
            TaskBase failedTask = TaskUtils.createManualTask(DummyQuartzCommandA.class, 0L);
            taskService.startTask(failedTask, false);
            Assert.fail(
                    "Should get an IllegalStateException trying to start a manual task while the singleton running");
        } catch (IllegalStateException e) {
        }
        Thread.sleep(500);
        task1Data.assertNbs(1, 0);
        TaskBase manualTask = TaskUtils.createManualTask(DummyQuartzCommandA.class, 0L);
        taskService.startTask(manualTask, true);
        Assert.assertTrue(manualTask.isRunning());
        Assert.assertFalse(task1.isRunning());
        TaskBase.TaskState task1State = (TaskBase.TaskState) ReflectionTestUtils.getField(task1, "state");
        Assert.assertEquals(task1State, TaskBase.TaskState.STOPPED);
        taskService.pauseTask(manualTask.getToken(), true);
        try {
            TaskBase failedTask = TaskUtils.createManualTask(DummyQuartzCommandA.class, 0L);
            taskService.startTask(failedTask, false);
            Assert.fail("Should get an IllegalStateException trying to start 2 manual tasks for a singleton");
        } catch (IllegalStateException e) {
        }
        Assert.assertFalse(manualTask.isSingleton());
        Assert.assertTrue(manualTask.isManuallyActivated());
        ArtifactoryHomeTaskTestStub.TaskTestData manualTaskData = homeStub.getTaskData(manualTask.getToken());
        manualTaskData.assertPause(1, 1);
        Assert.assertEquals(manualTaskData.taskStoppedOnExecute.size(), 1);
        Assert.assertEquals(manualTaskData.taskStoppedOnExecute.get(0), task1.getToken());
        Assert.assertNull(manualTaskData.taskStoppedAfterExecute);
        Assert.assertEquals(manualTaskData.userNameOnExecute, UserInfo.ANONYMOUS);
        Assert.assertTrue(taskService.resumeTask(manualTask.getToken()));
        Assert.assertTrue(taskService.waitForTaskCompletion(manualTask.getToken()));
        Thread.sleep(50);
        manualTaskData.assertNbs(1, 0);
        Assert.assertEquals(manualTaskData.taskStoppedAfterExecute.size(), 0);
        task1State = (TaskBase.TaskState) ReflectionTestUtils.getField(task1, "state");
        Assert.assertEquals(task1State, TaskBase.TaskState.SCHEDULED);
        taskService.cancelTask(task1.getToken(), true);
        Assert.assertNull(taskService.getInternalActiveTask(task1.getToken()));
        Assert.assertNull(taskService.getInternalActiveTask(manualTask.getToken()));
    }

    private TaskBase createManualDummyB(String stoppedToken) {
        ArtifactoryHomeTaskTestStub homeStub = getOrCreateArtifactoryHomeStub();
        TaskBase task2 = TaskUtils.createManualTask(DummyQuartzCommandB.class, 0L);
        task2.addAttribute(DummyQuartzCommand.MSECS_TO_RUN, "200");
        taskService.startTask(task2, true);
        ArtifactoryHomeTaskTestStub.TaskTestData taskData = homeStub.getTaskData(task2.getToken());
        Assert.assertTrue(task2.isRunning());
        taskService.pauseTask(task2.getToken(), true);
        taskData.assertPause(1, 1);
        Assert.assertFalse(task2.isRunning());
        Assert.assertFalse(task2.isSingleton());
        Assert.assertTrue(task2.isManuallyActivated());
        Assert.assertEquals(taskData.taskStoppedOnExecute.size(), 1);
        Assert.assertEquals(taskData.taskStoppedOnExecute.get(0), stoppedToken);
        Assert.assertNull(taskData.taskStoppedAfterExecute);
        Assert.assertEquals(taskData.userNameOnExecute, UserInfo.ANONYMOUS);
        return task2;
    }

    private TaskBase createRunAndPauseDummyA() throws Exception {
        ArtifactoryHomeTaskTestStub homeStub = getOrCreateArtifactoryHomeStub();
        TaskBase task1 = TaskUtils.createRepeatingTask(DummyQuartzCommandA.class, 1000, 100);
        taskService.startTask(task1, false);
        taskService.pauseTask(task1.getToken(), false);
        ArtifactoryHomeTaskTestStub.TaskTestData taskData = homeStub.getTaskData(task1.getToken());
        taskData.assertNbs(0, 0);
        Assert.assertTrue(task1.isSingleton());
        Assert.assertFalse(task1.isManuallyActivated());
        Assert.assertTrue(taskService.resumeTask(task1.getToken()));
        // Wait to start
        Thread.sleep(200);
        try {
            Assert.assertTrue(task1.isRunning());
            taskService.pauseTask(task1.getToken(), false);
            Assert.fail("Should get cannot stop running task exception");
        } catch (IllegalStateException e) {
        }
        taskService.pauseTask(task1.getToken(), true);
        taskData.assertPause(1, 1);
        Assert.assertTrue(taskData.taskStoppedOnExecute.isEmpty());
        Assert.assertNull(taskData.taskStoppedAfterExecute);
        Assert.assertEquals(taskData.userNameOnExecute, SecurityService.USER_SYSTEM);
        return task1;
    }

    @Test
    public void testStopStrategy() throws Exception {

    }
}