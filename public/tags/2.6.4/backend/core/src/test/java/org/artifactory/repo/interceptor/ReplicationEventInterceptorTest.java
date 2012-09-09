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

package org.artifactory.repo.interceptor;

import org.artifactory.addon.AddonsManager;
import org.artifactory.addon.replication.ReplicationAddon;
import org.artifactory.repo.RepoPath;
import org.artifactory.repo.RepoPathFactory;
import org.artifactory.sapi.fs.VfsItem;
import org.easymock.IExpectationSetters;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.lang.reflect.Field;

import static org.easymock.EasyMock.*;

/**
 * @author Noam Y. Tenne
 */
public class ReplicationEventInterceptorTest {

    private ReplicationEventInterceptor interceptor = new ReplicationEventInterceptor();
    private AddonsManager addonsManager = createMock(AddonsManager.class);
    private ReplicationAddon replicationAddon = createMock(ReplicationAddon.class);
    VfsItem sourceMock = createMock(VfsItem.class);
    VfsItem targetMock = createMock(VfsItem.class);
    private RepoPath sourceRepoPath = RepoPathFactory.create("momo:popo");
    private RepoPath targetRepoPath = RepoPathFactory.create("moo:boo");

    @BeforeClass
    public void setUp() throws Exception {
        Field addonsManagerField = interceptor.getClass().getDeclaredField("addonsManager");
        addonsManagerField.setAccessible(true);
        addonsManagerField.set(interceptor, addonsManager);

        expect(addonsManager.addonByType(ReplicationAddon.class)).andReturn(replicationAddon).anyTimes();
        replay(addonsManager);
    }

    @BeforeMethod
    public void resetAddon() throws Exception {
        reset(replicationAddon, sourceMock, targetMock);
        expect(sourceMock.getRepoPath()).andReturn(sourceRepoPath);
        expect(targetMock.getRepoPath()).andReturn(targetRepoPath);
    }

    @Test
    public void testAfterCreateFile() throws Exception {
        prepareTargetAsDeployedFile();
        executeAndVerifyAfterCreateWithTargetMock();
    }

    @Test
    public void testAfterCreateFolder() throws Exception {
        prepareTargetAsCreatedFolder();
        executeAndVerifyAfterCreateWithTargetMock();
    }

    @Test
    public void testAfterDelete() throws Exception {
        prepareSourceAsDeleted();
        replay(sourceMock, replicationAddon);
        interceptor.afterDelete(sourceMock, null);
        verify(replicationAddon, sourceMock);
    }

    @Test
    public void testAfterMoveFile() throws Exception {
        prepareTargetAsDeployedFile();
        prepareSourceAsDeleted();
        executeAndVerifyAfterMove();
    }

    @Test
    public void testAfterMoveFolder() throws Exception {
        prepareTargetAsCreatedFolder();
        prepareSourceAsDeleted();
        executeAndVerifyAfterMove();
    }

    @Test
    public void testAfterCopyFile() throws Exception {
        prepareTargetAsDeployedFile();
        executeAndVerifyAfterCopy();
    }

    @Test
    public void testAfterCopyFolder() throws Exception {
        prepareTargetAsCreatedFolder();
        executeAndVerifyAfterCopy();
    }

    private void prepareTargetAsDeployedFile() {
        prepareTargetAsFile();
        replicationAddon.offerLocalReplicationDeploymentEvent(targetRepoPath);
        expectLastCall();
    }

    private IExpectationSetters<Boolean> prepareTargetAsFile() {
        return expect(targetMock.isFile()).andReturn(true);
    }

    private void prepareTargetAsCreatedFolder() {
        prepareTargetAsFolder();
        replicationAddon.offerLocalReplicationMkDirEvent(targetRepoPath);
        expectLastCall();
    }

    private void prepareTargetAsFolder() {
        expect(targetMock.isFile()).andReturn(false);
    }

    private void prepareSourceAsDeleted() {
        replicationAddon.offerLocalReplicationDeleteEvent(sourceRepoPath);
        expectLastCall();
    }

    private void executeAndVerifyAfterCreateWithTargetMock() {
        replay(targetMock, replicationAddon);
        interceptor.afterCreate(targetMock, null);
        verify(replicationAddon, targetMock);
    }

    private void executeAndVerifyAfterMove() {
        replay(sourceMock, targetMock, replicationAddon);
        interceptor.afterMove(sourceMock, targetMock, null, null);
        verify(replicationAddon, sourceMock, targetMock);
    }

    private void executeAndVerifyAfterCopy() {
        replay(targetMock, replicationAddon);
        interceptor.afterCopy(null, targetMock, null, null);
        verify(replicationAddon, targetMock);
    }
}
