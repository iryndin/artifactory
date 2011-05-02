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

import com.google.common.collect.Maps;
import org.artifactory.api.config.CentralConfigService;
import org.artifactory.descriptor.config.CentralConfigDescriptor;
import org.artifactory.descriptor.repo.LocalRepoDescriptor;
import org.artifactory.descriptor.repo.RemoteRepoDescriptor;
import org.artifactory.descriptor.repo.VirtualRepoDescriptor;
import org.artifactory.spring.InternalArtifactoryContext;
import org.artifactory.test.mock.MockUtils;
import org.easymock.EasyMock;
import org.quartz.Scheduler;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * @author yoavl
 */
@Test(enabled = false)
public class TaskServiceTestBase {

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
                .andReturn(Maps.<String, LocalRepoDescriptor>newLinkedHashMap()).anyTimes();
        EasyMock.expect(
                ccd.getRemoteRepositoriesMap())
                .andReturn(Maps.<String, RemoteRepoDescriptor>newLinkedHashMap()).anyTimes();
        EasyMock.expect(
                ccd.getVirtualRepositoriesMap())
                .andReturn(Maps.<String, VirtualRepoDescriptor>newLinkedHashMap()).anyTimes();
        EasyMock.expect(cc.getDescriptor()).andReturn(ccd).anyTimes();

        //Put the task service into the context
        taskService = new TaskServiceImpl();
        taskService.onContextReady();
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
    }
}