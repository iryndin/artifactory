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
import org.artifactory.addon.YumAddon;
import org.artifactory.descriptor.repo.LocalRepoDescriptor;
import org.artifactory.model.common.RepoPathImpl;
import org.artifactory.repo.service.InternalRepositoryService;
import org.artifactory.sapi.fs.VfsFile;
import org.easymock.EasyMock;
import org.testng.annotations.Test;

/**
 * @author Noam Y. Tenne
 */
public class YumCalculationInterceptorTest {

    @Test
    public void testNoneRpmFile() throws Exception {
        YumCalculationInterceptor yumCalculationInterceptor = new YumCalculationInterceptor();

        VfsFile vfsFileMock = EasyMock.createMock(VfsFile.class);
        VfsFile copySourceMock = EasyMock.createMock(VfsFile.class);
        VfsFile copyTargetMock = EasyMock.createMock(VfsFile.class);
        VfsFile moveSourceMock = EasyMock.createMock(VfsFile.class);
        VfsFile moveTargetMock = EasyMock.createMock(VfsFile.class);
        LocalRepoDescriptor descriptorMock = EasyMock.createMock(LocalRepoDescriptor.class);
        InternalRepositoryService repoServiceMock = EasyMock.createMock(InternalRepositoryService.class);
        yumCalculationInterceptor.repositoryService = repoServiceMock;

        EasyMock.expect(vfsFileMock.getRepoPath()).andReturn(new RepoPathImpl("repoKey", "momo.zip")).times(2);
        EasyMock.expect(copyTargetMock.getRepoPath()).andReturn(new RepoPathImpl("repoKey", "momo.zip")).times(1);
        EasyMock.expect(moveSourceMock.getRepoPath()).andReturn(new RepoPathImpl("repoKey", "momo.zip")).times(1);
        EasyMock.expect(moveTargetMock.getRepoPath()).andReturn(new RepoPathImpl("repoKey", "momo.zip")).times(1);
        EasyMock.expect(descriptorMock.isCalculateYumMetadata()).andReturn(true).anyTimes();
        EasyMock.expect(descriptorMock.getYumGroupFileNames()).andReturn("").anyTimes();
        EasyMock.expect(repoServiceMock.localRepoDescriptorByKey("repoKey")).andReturn(descriptorMock).anyTimes();
        EasyMock.replay(vfsFileMock, copyTargetMock, moveSourceMock, moveTargetMock, descriptorMock, repoServiceMock);
        yumCalculationInterceptor.afterCreate(vfsFileMock, null);
        yumCalculationInterceptor.afterDelete(vfsFileMock, null);
        yumCalculationInterceptor.afterCopy(copySourceMock, copyTargetMock, null, null);
        yumCalculationInterceptor.afterMove(moveSourceMock, moveTargetMock, null, null);
        EasyMock.verify(vfsFileMock, copyTargetMock, moveSourceMock, moveTargetMock, descriptorMock, repoServiceMock);
        EasyMock.reset(vfsFileMock, copyTargetMock, moveSourceMock, moveTargetMock, descriptorMock, repoServiceMock);

        EasyMock.expect(vfsFileMock.getRepoPath()).andReturn(new RepoPathImpl("repoKey", "momo.jar")).times(2);
        EasyMock.expect(copyTargetMock.getRepoPath()).andReturn(new RepoPathImpl("repoKey", "momo.jar")).times(1);
        EasyMock.expect(moveSourceMock.getRepoPath()).andReturn(new RepoPathImpl("repoKey", "momo.jar")).times(1);
        EasyMock.expect(moveTargetMock.getRepoPath()).andReturn(new RepoPathImpl("repoKey", "momo.jar")).times(1);
        EasyMock.expect(descriptorMock.isCalculateYumMetadata()).andReturn(true).anyTimes();
        EasyMock.expect(descriptorMock.getYumGroupFileNames()).andReturn("").anyTimes();
        EasyMock.expect(repoServiceMock.localRepoDescriptorByKey("repoKey")).andReturn(descriptorMock).anyTimes();
        EasyMock.replay(vfsFileMock, copyTargetMock, moveSourceMock, moveTargetMock, descriptorMock, repoServiceMock);
        yumCalculationInterceptor.afterCreate(vfsFileMock, null);
        yumCalculationInterceptor.afterDelete(vfsFileMock, null);
        yumCalculationInterceptor.afterCopy(copySourceMock, copyTargetMock, null, null);
        yumCalculationInterceptor.afterMove(moveSourceMock, moveTargetMock, null, null);
        EasyMock.verify(vfsFileMock, copyTargetMock, moveSourceMock, moveTargetMock, descriptorMock, repoServiceMock);
        EasyMock.reset(vfsFileMock, copyTargetMock, moveSourceMock, moveTargetMock, descriptorMock, repoServiceMock);

        EasyMock.expect(vfsFileMock.getRepoPath()).andReturn(new RepoPathImpl("repoKey", "momo.pom")).times(2);
        EasyMock.expect(copyTargetMock.getRepoPath()).andReturn(new RepoPathImpl("repoKey", "momo.pom")).times(1);
        EasyMock.expect(moveSourceMock.getRepoPath()).andReturn(new RepoPathImpl("repoKey", "momo.pom")).times(1);
        EasyMock.expect(moveTargetMock.getRepoPath()).andReturn(new RepoPathImpl("repoKey", "momo.pom")).times(1);
        EasyMock.expect(descriptorMock.isCalculateYumMetadata()).andReturn(true).anyTimes();
        EasyMock.expect(descriptorMock.getYumGroupFileNames()).andReturn("").anyTimes();
        EasyMock.expect(repoServiceMock.localRepoDescriptorByKey("repoKey")).andReturn(descriptorMock).anyTimes();
        EasyMock.replay(vfsFileMock, copyTargetMock, moveSourceMock, moveTargetMock, descriptorMock, repoServiceMock);
        yumCalculationInterceptor.afterCreate(vfsFileMock, null);
        yumCalculationInterceptor.afterDelete(vfsFileMock, null);
        yumCalculationInterceptor.afterCopy(copySourceMock, copyTargetMock, null, null);
        yumCalculationInterceptor.afterMove(moveSourceMock, moveTargetMock, null, null);
        EasyMock.verify(vfsFileMock, copyTargetMock, moveSourceMock, moveTargetMock, descriptorMock, repoServiceMock);
        EasyMock.reset(vfsFileMock, copyTargetMock, moveSourceMock, moveTargetMock, descriptorMock, repoServiceMock);

        EasyMock.expect(vfsFileMock.getRepoPath()).andReturn(new RepoPathImpl("repoKey", "momo.xml")).times(2);
        EasyMock.expect(copyTargetMock.getRepoPath()).andReturn(new RepoPathImpl("repoKey", "momo.xml")).times(1);
        EasyMock.expect(moveSourceMock.getRepoPath()).andReturn(new RepoPathImpl("repoKey", "momo.xml")).times(1);
        EasyMock.expect(moveTargetMock.getRepoPath()).andReturn(new RepoPathImpl("repoKey", "momo.xml")).times(1);
        EasyMock.expect(descriptorMock.isCalculateYumMetadata()).andReturn(true).anyTimes();
        EasyMock.expect(descriptorMock.getYumGroupFileNames()).andReturn("").anyTimes();
        EasyMock.expect(repoServiceMock.localRepoDescriptorByKey("repoKey")).andReturn(descriptorMock).anyTimes();
        EasyMock.replay(vfsFileMock, copyTargetMock, moveSourceMock, moveTargetMock, descriptorMock, repoServiceMock);
        yumCalculationInterceptor.afterCreate(vfsFileMock, null);
        yumCalculationInterceptor.afterDelete(vfsFileMock, null);
        yumCalculationInterceptor.afterCopy(copySourceMock, copyTargetMock, null, null);
        yumCalculationInterceptor.afterMove(moveSourceMock, moveTargetMock, null, null);
        EasyMock.verify(vfsFileMock, copyTargetMock, moveSourceMock, moveTargetMock, descriptorMock, repoServiceMock);
        EasyMock.reset(vfsFileMock, copyTargetMock, moveSourceMock, moveTargetMock, descriptorMock, repoServiceMock);
    }

    @Test
    public void testRpmGroupFile() throws Exception {
        YumCalculationInterceptor yumCalculationInterceptor = new YumCalculationInterceptor();

        VfsFile vfsFileMock = EasyMock.createMock(VfsFile.class);
        VfsFile copySourceMock = EasyMock.createMock(VfsFile.class);
        VfsFile copyTargetMock = EasyMock.createMock(VfsFile.class);
        VfsFile moveSourceMock = EasyMock.createMock(VfsFile.class);
        VfsFile moveTargetMock = EasyMock.createMock(VfsFile.class);
        YumAddon yumAddonMock = EasyMock.createMock(YumAddon.class);
        AddonsManager addonsManagerMock = EasyMock.createMock(AddonsManager.class);
        yumCalculationInterceptor.addonsManager = addonsManagerMock;
        LocalRepoDescriptor descriptorMock = EasyMock.createMock(LocalRepoDescriptor.class);
        InternalRepositoryService repoServiceMock = EasyMock.createMock(InternalRepositoryService.class);
        yumCalculationInterceptor.repositoryService = repoServiceMock;

        EasyMock.expect(vfsFileMock.getRepoPath()).andReturn(
                new RepoPathImpl("bubu-local", "repodata/comps.xml")).times(2);
        EasyMock.expect(copyTargetMock.getRepoPath()).andReturn(
                new RepoPathImpl("bubu-local", "repodata/comps.xml")).times(1);
        EasyMock.expect(moveSourceMock.getRepoPath()).andReturn(
                new RepoPathImpl("bubu-local", "repodata/comps.xml")).times(1);
        EasyMock.expect(moveTargetMock.getRepoPath()).andReturn(
                new RepoPathImpl("bubu-local", "repodata/comps.xml")).times(1);
        EasyMock.expect(descriptorMock.isCalculateYumMetadata()).andReturn(true).anyTimes();
        EasyMock.expect(descriptorMock.getYumRootDepth()).andReturn(0).anyTimes();
        EasyMock.expect(descriptorMock.getYumGroupFileNames()).andReturn("comps.xml").anyTimes();
        EasyMock.expect(repoServiceMock.localRepoDescriptorByKey("bubu-local")).andReturn(descriptorMock).anyTimes();

        yumAddonMock.requestAsyncRepositoryYumMetadataCalculation(new RepoPathImpl("bubu-local", ""));
        EasyMock.expectLastCall().times(5);
        EasyMock.expect(addonsManagerMock.addonByType(YumAddon.class)).andReturn(yumAddonMock).times(5);

        EasyMock.replay(vfsFileMock, copyTargetMock, moveSourceMock, moveTargetMock, descriptorMock, repoServiceMock,
                yumAddonMock, addonsManagerMock);
        yumCalculationInterceptor.afterCreate(vfsFileMock, null);
        yumCalculationInterceptor.afterDelete(vfsFileMock, null);
        yumCalculationInterceptor.afterCopy(copySourceMock, copyTargetMock, null, null);
        yumCalculationInterceptor.afterMove(moveSourceMock, moveTargetMock, null, null);
        EasyMock.verify(vfsFileMock, copyTargetMock, moveSourceMock, moveTargetMock, descriptorMock, repoServiceMock,
                yumAddonMock, addonsManagerMock);
        EasyMock.reset(vfsFileMock, copyTargetMock, moveSourceMock, moveTargetMock, descriptorMock, repoServiceMock,
                yumAddonMock, addonsManagerMock);
    }

    @Test
    public void testNoneCalculatingRepo() throws Exception {
        YumCalculationInterceptor yumCalculationInterceptor = new YumCalculationInterceptor();

        LocalRepoDescriptor descriptorMock = EasyMock.createMock(LocalRepoDescriptor.class);
        EasyMock.expect(descriptorMock.isCalculateYumMetadata()).andReturn(false).times(5);

        InternalRepositoryService repoServiceMock = EasyMock.createMock(InternalRepositoryService.class);
        EasyMock.expect(repoServiceMock.localRepoDescriptorByKey("momo-local")).andReturn(descriptorMock).times(5);
        yumCalculationInterceptor.repositoryService = repoServiceMock;

        VfsFile vfsFileMock = EasyMock.createMock(VfsFile.class);
        VfsFile copySourceMock = EasyMock.createMock(VfsFile.class);
        VfsFile copyTargetMock = EasyMock.createMock(VfsFile.class);
        VfsFile moveSourceMock = EasyMock.createMock(VfsFile.class);
        VfsFile moveTargetMock = EasyMock.createMock(VfsFile.class);
        EasyMock.expect(vfsFileMock.getRepoPath()).andReturn(new RepoPathImpl("momo-local", "momo.rpm")).times(2);
        EasyMock.expect(copyTargetMock.getRepoPath()).andReturn(new RepoPathImpl("momo-local", "momo.rpm")).times(1);
        EasyMock.expect(moveSourceMock.getRepoPath()).andReturn(new RepoPathImpl("momo-local", "momo.rpm")).times(1);
        EasyMock.expect(moveTargetMock.getRepoPath()).andReturn(new RepoPathImpl("momo-local", "momo.rpm")).times(1);

        EasyMock.replay(vfsFileMock, copyTargetMock, moveSourceMock, moveTargetMock, descriptorMock, repoServiceMock);

        yumCalculationInterceptor.afterCreate(vfsFileMock, null);
        yumCalculationInterceptor.afterDelete(vfsFileMock, null);
        yumCalculationInterceptor.afterCopy(copySourceMock, copyTargetMock, null, null);
        yumCalculationInterceptor.afterMove(moveSourceMock, moveTargetMock, null, null);

        EasyMock.verify(vfsFileMock, copyTargetMock, moveSourceMock, moveTargetMock, descriptorMock, repoServiceMock);
    }

    @Test
    public void testCalculatingRepo() throws Exception {
        testCalculatingRepo("", 0, "", true);
        testCalculatingRepo("a/", 0, "", true);
        testCalculatingRepo("a/b/", 0, "", true);

        testCalculatingRepo("", 0, "", true);
        testCalculatingRepo("a/", 1, "a", true);
        testCalculatingRepo("a/b/", 2, "a/b", true);

        testCalculatingRepo("a/", 2, null, false);
        testCalculatingRepo("a/b/", 3, null, false);
    }

    private void testCalculatingRepo(String rpmSubDir, int calculationRootDepth, String calculationSubSir,
            boolean expectDeploymentWithinRange) throws Exception {
        YumCalculationInterceptor yumCalculationInterceptor = new YumCalculationInterceptor();

        LocalRepoDescriptor descriptorMock = EasyMock.createMock(LocalRepoDescriptor.class);
        EasyMock.expect(descriptorMock.getYumRootDepth()).andReturn(calculationRootDepth).times(5);
        EasyMock.expect(descriptorMock.isCalculateYumMetadata()).andReturn(true).times(5);

        InternalRepositoryService repoServiceMock = EasyMock.createMock(InternalRepositoryService.class);
        EasyMock.expect(repoServiceMock.localRepoDescriptorByKey("momo-local")).andReturn(descriptorMock).times(5);
        yumCalculationInterceptor.repositoryService = repoServiceMock;

        YumAddon yumAddonMock = EasyMock.createMock(YumAddon.class);
        AddonsManager addonsManagerMock = EasyMock.createMock(AddonsManager.class);
        if (expectDeploymentWithinRange) {
            yumAddonMock.requestAsyncRepositoryYumMetadataCalculation(
                    new RepoPathImpl("momo-local", calculationSubSir));
            EasyMock.expectLastCall().times(5);
            EasyMock.expect(addonsManagerMock.addonByType(YumAddon.class)).andReturn(yumAddonMock).times(5);
            yumCalculationInterceptor.addonsManager = addonsManagerMock;
        }

        VfsFile vfsFileMock = EasyMock.createMock(VfsFile.class);
        VfsFile copySourceMock = EasyMock.createMock(VfsFile.class);
        VfsFile copyTargetMock = EasyMock.createMock(VfsFile.class);
        VfsFile moveSourceMock = EasyMock.createMock(VfsFile.class);
        VfsFile moveTargetMock = EasyMock.createMock(VfsFile.class);

        EasyMock.expect(vfsFileMock.getRepoPath()).andReturn(new RepoPathImpl("momo-local", rpmSubDir + "momo.rpm"))
                .times(2);
        EasyMock.expect(copyTargetMock.getRepoPath()).andReturn(new RepoPathImpl("momo-local", rpmSubDir + "momo.rpm"))
                .times(1);
        EasyMock.expect(moveSourceMock.getRepoPath()).andReturn(new RepoPathImpl("momo-local", rpmSubDir + "momo.rpm"))
                .times(1);
        EasyMock.expect(moveTargetMock.getRepoPath()).andReturn(new RepoPathImpl("momo-local", rpmSubDir + "momo.rpm"))
                .times(1);

        EasyMock.replay(vfsFileMock, copyTargetMock, moveSourceMock, moveTargetMock, descriptorMock, repoServiceMock,
                yumAddonMock, addonsManagerMock);

        yumCalculationInterceptor.afterCreate(vfsFileMock, null);
        yumCalculationInterceptor.afterDelete(vfsFileMock, null);
        yumCalculationInterceptor.afterCopy(copySourceMock, copyTargetMock, null, null);
        yumCalculationInterceptor.afterMove(moveSourceMock, moveTargetMock, null, null);

        EasyMock.verify(vfsFileMock, copyTargetMock, moveSourceMock, moveTargetMock, descriptorMock, repoServiceMock,
                yumAddonMock, addonsManagerMock);
    }
}
