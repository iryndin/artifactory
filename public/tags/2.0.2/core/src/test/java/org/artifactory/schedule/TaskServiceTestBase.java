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

import org.apache.commons.collections15.map.LinkedMap;
import org.artifactory.api.cache.CacheService;
import org.artifactory.api.config.CentralConfigService;
import org.artifactory.cache.CacheServiceImpl;
import org.artifactory.descriptor.config.CentralConfigDescriptor;
import org.artifactory.descriptor.repo.LocalRepoDescriptor;
import org.artifactory.descriptor.repo.RemoteRepoDescriptor;
import org.artifactory.spring.InternalArtifactoryContext;
import org.artifactory.test.mock.MockUtils;
import org.easymock.EasyMock;
import org.quartz.Scheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * @author yoavl
 */
@Test
public class TaskServiceTestBase {
    @SuppressWarnings({"UnusedDeclaration"})
    private static final Logger log = LoggerFactory.getLogger(TaskServiceTestBase.class);

    protected TaskServiceImpl taskService;

    @BeforeClass
    public void setupTaskService() throws Exception {
        /*LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        lc.getLogger("org.artifactory.schedule").setLevel(Level.DEBUG);*/

        InternalArtifactoryContext context = MockUtils.getThreadBoundedMockContext();

        //Create a config mock
        CentralConfigService cc = EasyMock.createMock(CentralConfigService.class);
        EasyMock.expect(context.getCentralConfig()).andReturn(cc).anyTimes();
        CentralConfigDescriptor ccd = EasyMock.createMock(CentralConfigDescriptor.class);
        EasyMock.expect(
                ccd.getLocalRepositoriesMap())
                .andReturn(new LinkedMap<String, LocalRepoDescriptor>()).anyTimes();
        EasyMock.expect(
                ccd.getRemoteRepositoriesMap())
                .andReturn(new LinkedMap<String, RemoteRepoDescriptor>()).anyTimes();
        EasyMock.expect(cc.getDescriptor()).andReturn(ccd).anyTimes();

        //Put the cache service into the context
        CacheServiceImpl cacheService = new CacheServiceImpl();
        EasyMock.expect(context.beanForType(CacheService.class)).andReturn(cacheService).anyTimes();

        //Put the task service into the context
        taskService = new TaskServiceImpl();
        EasyMock.expect(context.getTaskService()).andReturn(taskService).anyTimes();

        //Put the scheduler into the context
        ArtifactorySchedulerFactoryBean schedulerFactory = new ArtifactorySchedulerFactoryBean();
        schedulerFactory.setTaskExecutor(new CachedThreadPoolTaskExecutor());
        schedulerFactory.afterPropertiesSet();
        Scheduler scheduler = (Scheduler) schedulerFactory.getObject();
        EasyMock.expect(context.beanForType(Scheduler.class)).andReturn(scheduler).anyTimes();
        schedulerFactory.setApplicationContext(context);

        //Charge the mocks
        EasyMock.replay(context, cc, ccd);

        //Init
        cacheService.init();
    }
}