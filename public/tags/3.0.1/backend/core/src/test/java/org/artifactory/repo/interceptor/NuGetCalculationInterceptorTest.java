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

import org.apache.commons.lang.StringUtils;
import org.artifactory.addon.AddonsManager;
import org.artifactory.addon.NuGetAddon;
import org.artifactory.addon.PropertiesAddon;
import org.artifactory.addon.nuget.NuGetProperties;
import org.artifactory.api.common.MultiStatusHolder;
import org.artifactory.api.repo.RepositoryService;
import org.artifactory.common.MutableStatusHolder;
import org.artifactory.descriptor.repo.LocalRepoDescriptor;
import org.artifactory.md.Properties;
import org.artifactory.model.xstream.fs.PropertiesImpl;
import org.artifactory.repo.RepoPath;
import org.artifactory.repo.RepoPathFactory;
import org.artifactory.sapi.fs.VfsFile;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.easymock.EasyMock.*;

/**
 * @author Noam Y. Tenne
 */
@Test
public class NuGetCalculationInterceptorTest {

    private NuGetCalculationInterceptor nuGetCalculationInterceptor = new NuGetCalculationInterceptor();
    private NuGetAddon nuGetAddon;

    @BeforeClass
    public void setUp() {
    }

    @BeforeMethod
    public void beforeMethod() {
        nuGetCalculationInterceptor.addonsManager = createMock(AddonsManager.class);
        nuGetAddon = createMock(NuGetAddon.class);
        expect(nuGetCalculationInterceptor.addonsManager.addonByType(NuGetAddon.class))
                .andReturn(nuGetAddon).anyTimes();
    }

    @Test
    public void testNotNuGetFile() {
        VfsFile vfsItemMock = createAndGetFsItemMock(RepoPathFactory.create("bob", "bob.jar"));
        replay(vfsItemMock);

        nuGetCalculationInterceptor.afterCopy(null, vfsItemMock, null, null);
        nuGetCalculationInterceptor.afterCreate(vfsItemMock, null);
        nuGetCalculationInterceptor.afterMove(null, vfsItemMock, null, null);
        nuGetCalculationInterceptor.beforeDelete(vfsItemMock, null);

        verify(vfsItemMock);
    }

    @Test
    public void testNotNuGetRepo() {
        RepositoryService repoServiceMock = createAndSetMockRepo(false, "bob");

        VfsFile vfsItemMock = createAndGetFsItemMock(RepoPathFactory.create("bob", "bob.nupkg"));
        replay(vfsItemMock, repoServiceMock);

        nuGetCalculationInterceptor.afterCopy(null, vfsItemMock, null, null);
        nuGetCalculationInterceptor.afterCreate(vfsItemMock, null);
        nuGetCalculationInterceptor.afterMove(null, vfsItemMock, null, null);
        nuGetCalculationInterceptor.beforeDelete(vfsItemMock, null);

        verify(vfsItemMock, repoServiceMock);
    }

    @Test
    public void testAfterCreate() {
        RepositoryService repoServiceMock = createAndSetMockRepo(true, "bob");
        VfsFile vfsItemMock = createAndGetFsItemMock(RepoPathFactory.create("bob", "bob.nupkg"));
        nuGetAddon.extractNuPkgInfo(eq(vfsItemMock), isA(MutableStatusHolder.class));
        expectLastCall();
        replay(vfsItemMock, repoServiceMock, nuGetCalculationInterceptor.addonsManager, nuGetAddon);
        nuGetCalculationInterceptor.afterCreate(vfsItemMock, new MultiStatusHolder());
        verify(vfsItemMock, repoServiceMock, nuGetCalculationInterceptor.addonsManager, nuGetAddon);
    }

    @Test
    public void testAfterMoveOrCopyNoIdProperty() {
        RepositoryService repoServiceMock = createAndSetMockRepo(true, "bob");
        RepoPath itemRepoPath = RepoPathFactory.create("bob", "bob.nupkg");
        VfsFile vfsItemMock = createAndGetFsItemMock(itemRepoPath);
        PropertiesAddon propertiesAddon = createAndSetMockProperties(itemRepoPath, null, false);

        replay(vfsItemMock, repoServiceMock, propertiesAddon, nuGetCalculationInterceptor.addonsManager);
        nuGetCalculationInterceptor.afterCopy(null, vfsItemMock, null, null);
        nuGetCalculationInterceptor.afterMove(null, vfsItemMock, null, null);
        verify(vfsItemMock, repoServiceMock, propertiesAddon, nuGetCalculationInterceptor.addonsManager);
    }

    @Test
    public void testAfterCopy() {
        RepositoryService repoServiceMock = createAndSetMockRepo(true, "bob");
        RepoPath itemRepoPath = RepoPathFactory.create("bob", "bob.nupkg");
        VfsFile vfsItemMock = createAndGetFsItemMock(itemRepoPath);
        expect(vfsItemMock.getRepoKey()).andReturn(itemRepoPath.getRepoKey());
        PropertiesAddon propertiesAddon = createAndSetMockProperties(itemRepoPath, "bobsId", false);

        nuGetAddon.requestAsyncLatestNuPkgVersionUpdate(itemRepoPath.getRepoKey(), "bobsId");
        expectLastCall();

        replay(vfsItemMock, repoServiceMock, propertiesAddon, nuGetCalculationInterceptor.addonsManager);
        nuGetCalculationInterceptor.afterCopy(null, vfsItemMock, null, null);
        verify(vfsItemMock, repoServiceMock, propertiesAddon, nuGetCalculationInterceptor.addonsManager);
    }

    @Test
    public void testAfterMoveNotLatestVersion() {
        RepositoryService repoServiceMock = createAndSetMockRepo(true, "bob");
        RepoPath targetRepoPath = RepoPathFactory.create("bob", "bob.nupkg");
        VfsFile targetVfsFile = createAndGetFsItemMock(targetRepoPath);
        expect(targetVfsFile.getRepoKey()).andReturn(targetRepoPath.getRepoKey());
        PropertiesAddon propertiesAddon = createAndSetMockProperties(targetRepoPath, "bobsId", false);

        nuGetAddon.requestAsyncLatestNuPkgVersionUpdate(targetRepoPath.getRepoKey(), "bobsId");
        expectLastCall();

        replay(targetVfsFile, repoServiceMock, propertiesAddon, nuGetCalculationInterceptor.addonsManager);
        nuGetCalculationInterceptor.afterMove(targetVfsFile, targetVfsFile, null, null);
        verify(targetVfsFile, repoServiceMock, propertiesAddon, nuGetCalculationInterceptor.addonsManager);
    }

    @Test
    public void testAfterMoveLatestVersion() {
        RepositoryService repoServiceMock = createAndSetMockRepo(true, "bob");
        RepoPath targetRepoPath = RepoPathFactory.create("bob", "bob.nupkg");
        VfsFile targetVfsFile = createAndGetFsItemMock(targetRepoPath);
        expect(targetVfsFile.getRepoKey()).andReturn(targetRepoPath.getRepoKey()).times(2);
        PropertiesAddon propertiesAddon = createAndSetMockProperties(targetRepoPath, "bobsId", true);

        nuGetAddon.requestAsyncLatestNuPkgVersionUpdate(targetRepoPath.getRepoKey(), "bobsId");
        expectLastCall().times(2);

        replay(targetVfsFile, repoServiceMock, propertiesAddon, nuGetCalculationInterceptor.addonsManager);
        nuGetCalculationInterceptor.afterMove(targetVfsFile, targetVfsFile, null, null);
        verify(targetVfsFile, repoServiceMock, propertiesAddon, nuGetCalculationInterceptor.addonsManager);
    }

    @Test
    public void testAfterMoveNoNuGetProperties() {
        RepositoryService repoServiceMock = createAndSetMockRepo(true, "bob");
        RepoPath targetRepoPath = RepoPathFactory.create("bob", "bob.nupkg");
        VfsFile targetVfsFile = createAndGetFsItemMock(targetRepoPath);
        PropertiesAddon propertiesAddon = createAndSetMockProperties(targetRepoPath, null, false);

        MultiStatusHolder statusHolder = new MultiStatusHolder();
        nuGetAddon.extractNuPkgInfo(targetVfsFile, statusHolder);
        expectLastCall();

        replay(targetVfsFile, repoServiceMock, propertiesAddon, nuGetCalculationInterceptor.addonsManager);
        nuGetCalculationInterceptor.afterMove(targetVfsFile, targetVfsFile, statusHolder, null);
        verify(targetVfsFile, repoServiceMock, propertiesAddon, nuGetCalculationInterceptor.addonsManager);
    }

    @Test
    public void testBeforeDeleteNotLatest() {
        RepositoryService repoServiceMock = createAndSetMockRepo(true, "bob");
        RepoPath itemRepoPath = RepoPathFactory.create("bob", "bob.nupkg");
        VfsFile vfsItemMock = createAndGetFsItemMock(itemRepoPath);
        PropertiesAddon propertiesAddon = createAndSetMockProperties(itemRepoPath, null, false);

        replay(vfsItemMock, repoServiceMock, propertiesAddon, nuGetCalculationInterceptor.addonsManager);
        nuGetCalculationInterceptor.beforeDelete(vfsItemMock, null);
        verify(vfsItemMock, repoServiceMock, propertiesAddon, nuGetCalculationInterceptor.addonsManager);
    }

    @Test
    public void testBeforeDeleteNoPackageId() {
        RepositoryService repoServiceMock = createAndSetMockRepo(true, "bob");
        RepoPath itemRepoPath = RepoPathFactory.create("bob", "bob.nupkg");
        VfsFile vfsItemMock = createAndGetFsItemMock(itemRepoPath);
        PropertiesAddon propertiesAddon = createAndSetMockProperties(itemRepoPath, null, true);

        replay(vfsItemMock, repoServiceMock, propertiesAddon, nuGetCalculationInterceptor.addonsManager);
        nuGetCalculationInterceptor.beforeDelete(vfsItemMock, null);
        verify(vfsItemMock, repoServiceMock, propertiesAddon, nuGetCalculationInterceptor.addonsManager);
    }

    @Test
    public void testBeforeDelete() {
        RepositoryService repoServiceMock = createAndSetMockRepo(true, "bob");
        RepoPath itemRepoPath = RepoPathFactory.create("bob", "bob.nupkg");
        VfsFile vfsItemMock = createAndGetFsItemMock(itemRepoPath);
        expect(vfsItemMock.getRepoKey()).andReturn(itemRepoPath.getRepoKey());
        PropertiesAddon propertiesAddon = createAndSetMockProperties(itemRepoPath, "bobsId", true);

        nuGetAddon.requestAsyncLatestNuPkgVersionUpdate(itemRepoPath.getRepoKey(), "bobsId");
        expectLastCall();

        replay(vfsItemMock, repoServiceMock, propertiesAddon, nuGetCalculationInterceptor.addonsManager, nuGetAddon);
        nuGetCalculationInterceptor.beforeDelete(vfsItemMock, null);
        verify(vfsItemMock, repoServiceMock, propertiesAddon, nuGetCalculationInterceptor.addonsManager, nuGetAddon);
    }

    @Test
    public void testAfterImport() throws Exception {
        RepositoryService repoServiceMock = createAndSetMockRepo(true, "bob");
        RepoPath itemRepoPath = RepoPathFactory.create("bob", "bob.nupkg");
        VfsFile vfsItemMock = createAndGetFsItemMock(itemRepoPath);
        MultiStatusHolder statusHolder = new MultiStatusHolder();

        nuGetAddon.extractNuPkgInfo(vfsItemMock, statusHolder);
        expectLastCall();

        replay(vfsItemMock, repoServiceMock, nuGetCalculationInterceptor.addonsManager, nuGetAddon);
        nuGetCalculationInterceptor.afterImport(vfsItemMock, statusHolder);
        verify(vfsItemMock, repoServiceMock, nuGetCalculationInterceptor.addonsManager, nuGetAddon);
    }

    private VfsFile createAndGetFsItemMock(RepoPath repoPath) {
        VfsFile vfsItemMock = createMock(VfsFile.class);
        expect(vfsItemMock.isFile()).andReturn(true).anyTimes();
        expect(vfsItemMock.getRepoPath()).andReturn(repoPath).anyTimes();
        return vfsItemMock;
    }

    private RepositoryService createAndSetMockRepo(boolean nuGetSupported, String repoKey) {
        RepositoryService repoServiceMock = createMock(RepositoryService.class);
        LocalRepoDescriptor localRepoDescriptor = new LocalRepoDescriptor();
        localRepoDescriptor.setEnableNuGetSupport(nuGetSupported);
        expect(repoServiceMock.repoDescriptorByKey(repoKey)).andReturn(localRepoDescriptor).anyTimes();
        nuGetCalculationInterceptor.repositoryService = repoServiceMock;
        return repoServiceMock;
    }

    private PropertiesAddon createAndSetMockProperties(RepoPath repoPath, String nugetId, boolean isLatestVersion) {
        PropertiesAddon propertiesAddon = createMock(PropertiesAddon.class);
        Properties toReturn = new PropertiesImpl();
        if (!StringUtils.isBlank(nugetId)) {
            toReturn.put(NuGetProperties.Id.nodePropertyName(), nugetId);
        }
        if (isLatestVersion) {
            toReturn.put(NuGetProperties.IsLatestVersion.nodePropertyName(), Boolean.toString(isLatestVersion));
        }
        expect(propertiesAddon.getProperties(repoPath)).andReturn(toReturn).anyTimes();
        expect(nuGetCalculationInterceptor.addonsManager.addonByType(PropertiesAddon.class))
                .andReturn(propertiesAddon).anyTimes();
        return propertiesAddon;
    }
}
