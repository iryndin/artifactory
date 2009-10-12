/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.artifactory.schedule;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import org.artifactory.schedule.quartz.QuartzTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * @author yoavl
 */
@Test(sequential = true)
public class SharedTaskServiceTest extends TaskServiceTestBase {
    private static final Logger log = LoggerFactory.getLogger(SharedTaskServiceTest.class);

    protected QuartzTask task;

    @BeforeClass
    public void startTask() throws Exception {
        task = new QuartzTask(DummyQuartzCommand.class, 50);
        taskService.startTask(task);
        LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        lc.getLogger("org.artifactory.schedule.TaskBase").setLevel(Level.TRACE);
        lc.getLogger("org.artifactory.schedule.TaskCallback").setLevel(Level.TRACE);
    }

    @AfterClass
    public void cancelTask() {
        taskService.cancelTask(task.getToken(), true);
    }

    @Test
    public void testPause() throws Exception {
        Thread.sleep(200);
        log.debug("######### PAUSING #########");
        taskService.pauseTask(task.getToken(), true);
        log.debug("######### PAUSED #########");
        Thread.sleep(200);
        log.debug("######### RESUMED #########");
        taskService.resumeTask(task.getToken());
        Thread.sleep(200);
        log.debug("######### STOPPING #########");
        taskService.stopTask(task.getToken(), true);
        log.debug("######### STOPPED #########");
        Thread.sleep(200);
        log.debug("######### RESUMING #########");
        taskService.resumeTask(task.getToken());
        log.debug("######### RESUMED #########");
        Thread.sleep(200);
    }

    @Test(invocationCount = 12, threadPoolSize = 12)
    public void testConcurrentServiceAccess() throws Exception {
        taskService.pauseTask(task.getToken(), true);
        Thread.sleep(200);
        taskService.resumeTask(task.getToken());
        Thread.sleep(200);
        taskService.stopTask(task.getToken(), true);
        Thread.sleep(200);
        taskService.resumeTask(task.getToken());
        Thread.sleep(200);
    }
}