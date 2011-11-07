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

package org.artifactory.repo.replication;

import org.apache.commons.io.output.NullWriter;
import org.artifactory.addon.AddonsManager;
import org.artifactory.addon.ReplicationAddon;
import org.artifactory.addon.replication.RemoteReplicationSettings;
import org.artifactory.addon.replication.RemoteReplicationSettingsBuilder;
import org.artifactory.api.common.MultiStatusHolder;
import org.artifactory.api.context.ArtifactoryContextThreadBinder;
import org.artifactory.api.security.SecurityService;
import org.artifactory.descriptor.replication.RemoteReplicationDescriptor;
import org.easymock.EasyMock;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;

/**
 * @author Noam Y. Tenne
 */
public class RemoteReplicationJobTest extends ReplicationJobTestBase {

    private static final RemoteReplicationJob job = new RemoteReplicationJob();
    private RemoteReplicationDescriptor replicationDescriptor;
    private RemoteReplicationSettings replicationSettings;

    @BeforeClass
    public void setUp() throws Exception {
        ArtifactoryContextThreadBinder.bind(artifactoryContext);

        replicationDescriptor = new RemoteReplicationDescriptor();
        replicationDescriptor.setRepoKey("key");
        replicationDescriptor.setEnabled(true);
        replicationDescriptor.setPathPrefix("path");
        replicationDescriptor.setSyncDeletes(true);
        replicationDescriptor.setSyncProperties(true);
        replicationDescriptor.setSocketTimeoutMillis(111111);

        replicationSettings = new RemoteReplicationSettingsBuilder(replicationDescriptor.getRepoPath(),
                new NullWriter())
                .deleteExisting(replicationDescriptor.isSyncDeletes())
                .includeProperties(replicationDescriptor.isSyncProperties())
                .timeout(replicationDescriptor.getSocketTimeoutMillis())
                .build();
    }

    @Test
    public void testContextNotReady() throws Exception {
        EasyMock.expect(artifactoryContext.isReady()).andReturn(false);
        replayMocks();
        job.onExecute(null);
        verifyMocks();
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void testNullDescriptor() throws Exception {
        EasyMock.expect(artifactoryContext.isReady()).andReturn(true);
        EasyMock.expect(executionContext.getJobDetail()).andReturn(jobDetail);
        EasyMock.expect(jobDetail.getJobDataMap()).andReturn(jobDataMap);
        EasyMock.expect(jobDataMap.get(ReplicationAddon.DESCRIPTOR)).andReturn(null);
        replayMocks();
        job.onExecute(null);
        verifyMocks();
    }

    @Test
    public void testExecutionException() throws Exception {
        EasyMock.expect(artifactoryContext.isReady()).andReturn(true);
        EasyMock.expect(executionContext.getJobDetail()).andReturn(jobDetail);
        EasyMock.expect(jobDetail.getJobDataMap()).andReturn(jobDataMap);
        EasyMock.expect(jobDataMap.get(ReplicationAddon.DESCRIPTOR)).andReturn(replicationDescriptor);
        EasyMock.expect(artifactoryContext.beanForType(SecurityService.class)).andReturn(securityService);
        securityService.authenticateAsSystem();
        EasyMock.expectLastCall();
        EasyMock.expect(artifactoryContext.beanForType(AddonsManager.class)).andReturn(addonsManager);
        EasyMock.expect(addonsManager.addonByType(ReplicationAddon.class)).andReturn(replicationAddon);
        EasyMock.expect(replicationAddon.performRemoteReplication(replicationSettings))
                .andThrow(new IOException());
        securityService.nullifyContext();
        EasyMock.expectLastCall();
        replayMocks();
        job.onExecute(executionContext);
        verifyMocks();
    }

    @Test
    public void testExecution() throws Exception {
        EasyMock.expect(artifactoryContext.isReady()).andReturn(true);
        EasyMock.expect(executionContext.getJobDetail()).andReturn(jobDetail);
        EasyMock.expect(jobDetail.getJobDataMap()).andReturn(jobDataMap);
        EasyMock.expect(jobDataMap.get(ReplicationAddon.DESCRIPTOR)).andReturn(replicationDescriptor);
        EasyMock.expect(artifactoryContext.beanForType(SecurityService.class)).andReturn(securityService);
        securityService.authenticateAsSystem();
        EasyMock.expectLastCall();
        EasyMock.expect(artifactoryContext.beanForType(AddonsManager.class)).andReturn(addonsManager);
        EasyMock.expect(addonsManager.addonByType(ReplicationAddon.class)).andReturn(replicationAddon);
        EasyMock.expect(replicationAddon.performRemoteReplication(replicationSettings))
                .andReturn(new MultiStatusHolder());
        securityService.nullifyContext();
        EasyMock.expectLastCall();
        replayMocks();
        job.onExecute(executionContext);
        verifyMocks();
    }

}
