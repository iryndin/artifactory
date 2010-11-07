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

package org.artifactory.schedule;

import org.artifactory.log.LoggerFactory;
import org.artifactory.schedule.quartz.QuartzCommand;
import org.artifactory.schedule.quartz.QuartzTask;
import org.slf4j.Logger;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * @author yoavl
 */
@Test(sequential = true, enabled = false)
public class TaskServiceTest extends TaskServiceTestBase {
    private static final Logger log = LoggerFactory.getLogger(TaskServiceTest.class);

    @BeforeClass
    public void startTask() throws Exception {
        /*LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        lc.getLogger("org.artifactory.schedule.TaskServiceTest").setLevel(Level.INFO);
        lc.getLogger("org.artifactory.schedule").setLevel(Level.DEBUG);
        lc.getLogger("org.artifactory.schedule.TaskBase").setLevel(Level.TRACE);
        lc.getLogger("org.artifactory.schedule.TaskCallback").setLevel(Level.TRACE);*/
    }

    @Test(enabled = false, invocationCount = 5, threadPoolSize = 3)
    public void testServiceSynchronization() throws Exception {
        QuartzTask task1 = new QuartzTask(DummyQuartzCommand.class, 100);
        taskService.startTask(task1);
        taskService.pauseTask(task1.getToken(), true);
        QuartzTask task2 = new QuartzTask(DummyQuartzCommand.class, 100);
        taskService.startTask(task2);
        Thread.sleep(600);
        taskService.stopTask(task1.getToken(), true);
        taskService.stopTask(task2.getToken(), true);
        /*
        InternalRepositoryService irs = EasyMock.createMock(InternalRepositoryService.class);
        EasyMock.replay(irs);

        LocalRepoDescriptor lrd = new LocalRepoDescriptor();
        lrd.setKey("libs");
        JcrRepo jcrRepo = new JcrRepo(irs, lrd);
        ReflectionTestUtils.setField(jcrRepo, "anonAccessEnabled", false);
        RepoPath path = new RepoPathImpl("libs", "jfrog/settings/jfrog-settings-sources.zip");
        EasyMock.expect(authService.canRead(path)).andReturn(false);
        EasyMock.expect(authService.currentUsername()).andReturn("testUser");
        EasyMock.replay(authService);

        StatusHolder holder = jcrRepo.allowsDownload(path);
        Assert.assertTrue(holder.isError(), "User should not have access to src files");
        EasyMock.verify(context, authService, irs);
        */
    }

    @Test(enabled = false, invocationCount = 10)
    public void testMutliResume() throws Exception {
        final CyclicBarrier pauseBarrier1 = new CyclicBarrier(2);
        final CyclicBarrier pauseBarrier2 = new CyclicBarrier(2);
        final TaskBase tsk = new QuartzTask(DummyQuartzCommand.class, 50);
        Callable<String> c1 = new Callable<String>() {
            public String call() throws Exception {
                log.info("........... PAUSING-1");
                taskService.pauseTask(tsk.getToken(), true);
                pauseBarrier1.await();
                pauseBarrier2.await();
                return null;
            }
        };
        Callable<String> c2 = new Callable<String>() {
            public String call() throws Exception {
                pauseBarrier1.await();
                log.info("........... PAUSING-2");
                taskService.pauseTask(tsk.getToken(), true);
                pauseBarrier2.await();
                log.info("........... RESUMING-1");
                boolean resumed = taskService.resumeTask(tsk.getToken());
                if (resumed) {
                    return "Resume barriers are broken - resumed too early.";
                }
                log.info("........... RESUMING-2");
                resumed = taskService.resumeTask(tsk.getToken());
                if (!resumed) {
                    return "Resume barriers are broken - expected resume was not be possible.";
                }
                return null;
            }
        };
        taskService.startTask(tsk);
        ExecutorService executorService = Executors.newCachedThreadPool();
        executorService.submit(c1);
        Future<String> result = executorService.submit(c2);
        String error = result.get();
        if (error != null) {
            Assert.fail(error);
        }
    }

    @Test(enabled = false)
    public void testCancelWhenWaitingOnStateTransition() throws Exception {
        final CyclicBarrier pauseBarrier1 = new CyclicBarrier(2);
        final CyclicBarrier pauseBarrier2 = new CyclicBarrier(2);
        final TaskBase tsk = new QuartzTask(DummyQuartzCommand.class, 50);
        Callable<String> c1 = new Callable<String>() {
            public String call() throws Exception {
                log.info("........... PAUSING-1");
                taskService.pauseTask(tsk.getToken(), true);
                pauseBarrier1.await();
                //Should get here after cancel
                pauseBarrier2.await();
                return null;
            }
        };
        taskService.startTask(tsk);
        ExecutorService executorService = Executors.newCachedThreadPool();
        executorService.submit(c1);
    }

    @Test(enabled = false)
    public void testMultiSingleExecution() throws Exception {
        taskService.cancelAllTasks(true);
        QuartzCommand cmd = new DummyQuartzCommand();
        final QuartzTask tsk1 = new QuartzTask(cmd.getClass(), 0, 0);
        //Make the task run at least 500ms, so that it wont finish before the second conflicting one is scheduled
        tsk1.addAttribute(DummyQuartzCommand.MSECS_TO_RUN, "500");
        tsk1.setSingleton(true);
        taskService.startTask(tsk1);
        log.info("........... STARTED TSK1");
        final TaskBase tsk2 = new QuartzTask(cmd.getClass(), 0, 0);
        tsk2.setSingleton(true);
        try {
            taskService.startTask(tsk2);
            Assert.fail("Should not be able to run 2 singleton tasks concurrently.");
        } catch (IllegalStateException e) {
            //Good - we expected it
        }
    }

    @Test(enabled = false)
    public void testSingleExecutionWithError() throws Exception {
        taskService.cancelAllTasks(true);
        QuartzCommand cmd = new DummyQuartzCommand();
        final QuartzTask tsk1 = new QuartzTask(cmd.getClass(), 0, 0);
        tsk1.setSingleton(true);
        tsk1.addAttribute(DummyQuartzCommand.FAIL, Boolean.TRUE);
        taskService.startTask(tsk1);
        //TODO: [by yl] Find a better way...
        //Let it start
        Thread.sleep(500);
        log.info("........... WAITING FOR TSK1");
        taskService.waitForTaskCompletion(tsk1.getToken());
        TaskBase activeTask = taskService.getInternalActiveTask(tsk1.getToken());
        Assert.assertNull(activeTask);
        final TaskBase tsk2 = new QuartzTask(cmd.getClass(), 0, 0);
        tsk2.setSingleton(true);
        taskService.startTask(tsk2);
    }

    @Test(enabled = false)
    public void testConcurrentStopResumes() throws Exception {
        final CyclicBarrier barrier1 = new CyclicBarrier(2);
        final CyclicBarrier barrier2 = new CyclicBarrier(2);
        final TaskBase tsk = new QuartzTask(DummyQuartzCommand.class, 50);
        Callable<String> c1 = new Callable<String>() {
            public String call() throws Exception {
                barrier1.await();
                log.info("........... PAUSING-1");
                taskService.pauseTask(tsk.getToken(), true);
                barrier2.await();
                return null;
            }
        };
        Callable<String> c2 = new Callable<String>() {
            public String call() throws Exception {
                barrier1.await();
                log.info("........... STOPING-2");
                taskService.stopTask(tsk.getToken(), true);
                barrier2.await();
                return null;
            }
        };
        taskService.startTask(tsk);
        Thread.sleep(200);
        ExecutorService executorService = Executors.newCachedThreadPool();
        executorService.submit(c1);
        executorService.submit(c2);
    }

    @Test(enabled = false)
    public void testDoubleResume() throws Exception {
        QuartzTask task1 = new QuartzTask(DummyQuartzCommand.class, 100);
        taskService.startTask(task1);
        taskService.pauseTask(task1.getToken(), true);
        taskService.stopTask(task1.getToken(), true);
        log.info("........... RESUMING-1");
        taskService.resumeTask(task1.getToken());
        log.info("........... RESUMING-2");
        taskService.resumeTask(task1.getToken());
        log.info("........... RESUMING-3");
        taskService.resumeTask(task1.getToken());
        log.info("........... RESUMING-4");
        taskService.resumeTask(task1.getToken());
        log.info("........... DONE");
    }
}