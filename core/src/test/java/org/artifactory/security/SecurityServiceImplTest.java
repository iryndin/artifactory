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

package org.artifactory.security;

import org.artifactory.api.repo.RepoPathImpl;
import org.artifactory.api.security.AceInfo;
import org.artifactory.api.security.AclInfo;
import org.artifactory.api.security.ArtifactoryPermission;
import org.artifactory.api.security.GroupInfo;
import org.artifactory.api.security.PermissionTargetInfo;
import org.artifactory.api.security.UserInfo;
import org.artifactory.api.security.UserInfoBuilder;
import org.artifactory.config.InternalCentralConfigService;
import org.artifactory.descriptor.config.CentralConfigDescriptor;
import org.artifactory.descriptor.security.SecurityDescriptor;
import org.artifactory.repo.LocalRepo;
import org.artifactory.repo.RepoPath;
import org.artifactory.repo.service.InternalRepositoryService;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.test.util.ReflectionTestUtils;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.easymock.EasyMock.*;
import static org.testng.Assert.*;

/**
 * SecurityServiceImpl unit tests. TODO: simplify the tests
 *
 * @author Yossi Shaul
 */
@Test
public class SecurityServiceImplTest {
    private SecurityContextImpl securityContext;
    private SecurityServiceImpl service;
    private List<Acl> testAcls;
    private InternalAclManager internalAclManagerMock;
    private String userAndGroupSharedName;
    private List<PermissionTarget> permissionTargets;
    private InternalRepositoryService repositoryServiceMock;
    private LocalRepo localRepoMock;
    private LocalRepo cacheRepoMock;
    private InternalCentralConfigService centralConfigServiceMock;

    @BeforeClass
    public void initArtifactoryRoles() {
        testAcls = createTestAcls();
        internalAclManagerMock = createMock(InternalAclManager.class);
        repositoryServiceMock = createMock(InternalRepositoryService.class);
        centralConfigServiceMock = createMock(InternalCentralConfigService.class);
        localRepoMock = createLocalRepoMock();
        cacheRepoMock = createCacheRepoMock();
    }

    @BeforeMethod
    public void setUp() {
        // create new security context
        securityContext = new SecurityContextImpl();
        SecurityContextHolder.setContext(securityContext);

        // new service instance
        service = new SecurityServiceImpl();
        // set the aclManager mock on the security service
        ReflectionTestUtils.setField(service, "internalAclManager", internalAclManagerMock);
        ReflectionTestUtils.setField(service, "repositoryService", repositoryServiceMock);
        ReflectionTestUtils.setField(service, "centralConfig", centralConfigServiceMock);

        // reset mocks
        reset(internalAclManagerMock, repositoryServiceMock, centralConfigServiceMock);
    }

    public void isAdminOnAdminUser() {
        Authentication authentication = setAdminAuthentication();

        boolean admin = service.isAdmin();
        assertTrue(admin, "The user in test is admin");
        // un-authenticate
        authentication.setAuthenticated(false);
        admin = service.isAdmin();
        assertFalse(admin, "Unauthenticated token");
    }

    public void isAdminOnSimpleUser() {
        setSimpleUserAuthentication();

        boolean admin = service.isAdmin();
        assertFalse(admin, "The user in test is not an admin");
    }

    @Test(dependsOnMethods = "isAdminOnAdminUser")
    public void spidermanCanDoAnything() {
        setAdminAuthentication();
        assertFalse(service.isAnonymous());// sanity
        assertTrue(service.isAdmin());// sanity

        RepoPath path = new RepoPathImpl("someRepo", "blabla");
        boolean canRead = service.canRead(path);
        assertTrue(canRead);
        boolean canDeploy = service.canDeploy(path);
        assertTrue(canDeploy);
    }

    @Test
    public void userReadAndDeployPermissions() {
        Authentication authentication = setSimpleUserAuthentication();

        RepoPath securedPath = new RepoPathImpl("securedRepo", "blabla");
        expect(repositoryServiceMock.localOrCachedRepositoryByKey(securedPath.getRepoKey()))
                .andReturn(localRepoMock).anyTimes();
        replay(repositoryServiceMock);

        // cannot read the specified path
        expectGetAllAclsCall(authentication);
        replay(internalAclManagerMock);
        boolean canRead = service.canRead(securedPath);
        assertFalse(canRead, "User should not have permissions for this path");
        verify(internalAclManagerMock);
        reset(internalAclManagerMock);

        // cannot deploy to the specified path
        expectGetAllAclsCall(authentication);
        replay(internalAclManagerMock);

        boolean canDeploy = service.canDeploy(securedPath);
        assertFalse(canDeploy, "User should not have permissions for this path");
        verify(internalAclManagerMock, repositoryServiceMock);
        reset(internalAclManagerMock, repositoryServiceMock);

        RepoPath allowedReadPath = new RepoPathImpl("testRepo1", "blabla");
        expect(repositoryServiceMock.localOrCachedRepositoryByKey(allowedReadPath.getRepoKey()))
                .andReturn(localRepoMock).anyTimes();
        replay(repositoryServiceMock);

        // can read the specified path
        expectGetAllAclsCall(authentication);
        replay(internalAclManagerMock);
        canRead = service.canRead(allowedReadPath);
        assertTrue(canRead, "User should have read permissions for this path");
        verify(internalAclManagerMock);
        reset(internalAclManagerMock);

        // cannot deploy to the specified path
        expectGetAllAclsCall(authentication);
        replay(internalAclManagerMock);
        canDeploy = service.canDeploy(allowedReadPath);
        assertFalse(canDeploy, "User should not have permissions for this path");
        verify(internalAclManagerMock, repositoryServiceMock);
        reset(internalAclManagerMock);

        // cannot admin the specified path
        expectGetAllAclsCall(authentication);
        replay(internalAclManagerMock);
        boolean canAdmin = service.canAdmin(allowedReadPath);
        assertFalse(canAdmin, "User should not have permissions for this path");
        verify(internalAclManagerMock);
    }

    @Test
    public void adminRolePermissions() {
        // user with admin role on permission target 'target1'
        Authentication authentication = setSimpleUserAuthentication("yossis");

        RepoPath allowedReadPath = new RepoPathImpl("testRepo1", "blabla");

        // can read the specified path
        expectGetAllAclsCall(authentication);
        replay(internalAclManagerMock);
        boolean canRead = service.canRead(allowedReadPath);
        assertTrue(canRead, "User should have permissions for this path");
        verify(internalAclManagerMock);
        reset(internalAclManagerMock);

        // can deploy to the specified path
        expectGetAllAclsCall(authentication);
        replay(internalAclManagerMock);
        boolean canDeploy = service.canDeploy(allowedReadPath);
        assertTrue(canDeploy, "User should have permissions for this path");
        verify(internalAclManagerMock);
        reset(internalAclManagerMock);

        // can admin the specified path
        expectGetAllAclsCall(authentication);
        replay(internalAclManagerMock);
        boolean canAdmin = service.canAdmin(allowedReadPath);
        assertTrue(canAdmin, "User should have permissions for this path");
        verify(internalAclManagerMock);
        reset(internalAclManagerMock);

        // can admin
        Acl testRepo1Acl = testAcls.get(0);
        PermissionTarget target = testRepo1Acl.getPermissionTarget();
        expect(internalAclManagerMock.findAclById(target)).andReturn(testRepo1Acl);
        replay(internalAclManagerMock);
        boolean canAdminTarget = service.canAdmin(target.getDescriptor());
        assertTrue(canAdminTarget, "User should have admin permissions for this target");
        verify(internalAclManagerMock);
    }

    @Test
    public void groupPermissions() {
        Authentication authentication = setSimpleUserAuthentication("userwithnopermissions");

        RepoPath allowedReadPath = new RepoPathImpl("testRepo1", "**");
        expect(repositoryServiceMock.localOrCachedRepositoryByKey(allowedReadPath.getRepoKey()))
                .andReturn(localRepoMock).anyTimes();
        replay(repositoryServiceMock);

        // cannot deploy to the specified path
        expectGetAllAclsCall(authentication);
        replay(internalAclManagerMock);
        boolean canDeploy = service.canDeploy(allowedReadPath);
        assertFalse(canDeploy, "User should have permissions for this path");
        verify(internalAclManagerMock);
        reset(internalAclManagerMock);

        // add the user to a group with permissions and expext permission garnted
        setSimpleUserAuthentication("userwithnopermissions", "deployGroup");
        expectGetAllAclsCallWithAnyArray();
        replay(internalAclManagerMock);
        canDeploy = service.canDeploy(allowedReadPath);
        assertTrue(canDeploy, "User in a group with permissions for this path");
        verify(internalAclManagerMock, repositoryServiceMock);
        reset(internalAclManagerMock, repositoryServiceMock);
    }

    @Test
    public void userWithPermissionsToAGroupWithTheSameName() {
        setSimpleUserAuthentication(userAndGroupSharedName, userAndGroupSharedName);

        RepoPath testRepo1Path = new RepoPathImpl("testRepo1", "**");
        expectGetAllAclsCallWithAnyArray();
        replay(internalAclManagerMock);
        boolean canRead = service.canRead(testRepo1Path);
        assertTrue(canRead, "User should have permissions for this path");
        verify(internalAclManagerMock);
        reset(internalAclManagerMock);

        RepoPath testRepo2Path = new RepoPathImpl("testRepo2", "**");
        expectGetAllAclsCallWithAnyArray();
        replay(internalAclManagerMock);
        canRead = service.canRead(testRepo2Path);
        assertTrue(canRead, "User belongs to a group with permissions to the path");
        verify(internalAclManagerMock);
        reset(internalAclManagerMock);
    }

    @Test
    public void userWithPermissionsToANonUniqueGroupName() {
        // here we test that a user that belongs to a group which has
        // the same name of a nother user will only get the group permissions
        // and not the user with the same name permissions
        setSimpleUserAuthentication("auser", userAndGroupSharedName);

        RepoPath testRepo1Path = new RepoPathImpl("testRepo1", "**");
        expectGetAllAclsCallWithAnyArray();
        expect(repositoryServiceMock.localOrCachedRepositoryByKey("testRepo1"))
                .andReturn(localRepoMock).anyTimes();
        replay(internalAclManagerMock, repositoryServiceMock);
        boolean canRead = service.canRead(testRepo1Path);
        assertFalse(canRead, "User should not have permissions for this path");
        verify(internalAclManagerMock, repositoryServiceMock);
        reset(internalAclManagerMock, repositoryServiceMock);

        RepoPath testRepo2Path = new RepoPathImpl("testRepo2", "**");
        expectGetAllAclsCallWithAnyArray();
        expect(repositoryServiceMock.localOrCachedRepositoryByKey("testRepo2"))
                .andReturn(localRepoMock).anyTimes();
        replay(internalAclManagerMock, repositoryServiceMock);
        canRead = service.canRead(testRepo2Path);
        assertTrue(canRead, "User belongs to a group with permissions to the path");
        verify(internalAclManagerMock, repositoryServiceMock);
    }

    @Test
    public void hasPermissionPassingUserInfo() {
        SimpleUser user = createNonAdminUser("yossis");
        UserInfo userInfo = user.getDescriptor();

        RepoPath testRepo1Path = new RepoPathImpl("testRepo1", "any/path");

        expectGetAllAclsCallWithAnyArray();
        replay(internalAclManagerMock);
        boolean canRead = service.canRead(userInfo, testRepo1Path);
        assertTrue(canRead, "User should have permissions for this path");
        verify(internalAclManagerMock);
        reset(internalAclManagerMock);

        expectGetAllAclsCallWithAnyArray();
        replay(internalAclManagerMock);
        boolean canDeploy = service.canDeploy(userInfo, testRepo1Path);
        assertTrue(canDeploy, "User should have permissions for this path");
        verify(internalAclManagerMock);
        reset(internalAclManagerMock);

        expect(repositoryServiceMock.localOrCachedRepositoryByKey("testRepo1")).andReturn(localRepoMock).anyTimes();
        expectGetAllAclsCallWithAnyArray();
        replay(internalAclManagerMock, repositoryServiceMock);
        boolean canDelete = service.canDelete(userInfo, testRepo1Path);
        assertFalse(canDelete, "User should not have permissions for this path");
        verify(internalAclManagerMock, repositoryServiceMock);
        reset(internalAclManagerMock);

        expectGetAllAclsCallWithAnyArray();
        replay(internalAclManagerMock);
        boolean canAdmin = service.canAdmin(userInfo, testRepo1Path);
        assertTrue(canAdmin, "User should have permissions for this path");
        verify(internalAclManagerMock);
        reset(internalAclManagerMock, repositoryServiceMock);

        RepoPath testRepo2Path = new RepoPathImpl("testRepo2", "**");

        expect(repositoryServiceMock.localOrCachedRepositoryByKey("testRepo2")).andReturn(localRepoMock).anyTimes();
        expectGetAllAclsCallWithAnyArray();
        replay(internalAclManagerMock, repositoryServiceMock);
        canRead = service.canRead(userInfo, testRepo2Path);
        assertFalse(canRead, "User should not have permissions for this path");
        verify(internalAclManagerMock);
        reset(internalAclManagerMock, repositoryServiceMock);

        SecurityDescriptor securityDescriptor = new SecurityDescriptor();
        securityDescriptor.setAnonAccessEnabled(false);

        CentralConfigDescriptor configDescriptor = createMock(CentralConfigDescriptor.class);
        expect(configDescriptor.getSecurity()).andReturn(securityDescriptor).anyTimes();
        replay(configDescriptor);
        expect(centralConfigServiceMock.getDescriptor()).andReturn(configDescriptor).anyTimes();
        replay(centralConfigServiceMock);

        SimpleUser anon = createNonAdminUser(UserInfo.ANONYMOUS);
        UserInfo anonUserInfo = anon.getDescriptor();

        RepoPath testMultiRepo = new RepoPathImpl("multi1", "**");
        expect(repositoryServiceMock.localOrCachedRepositoryByKey("multi1")).andReturn(cacheRepoMock).anyTimes();
        expectGetAllAclsCallWithAnyArray();
        replay(repositoryServiceMock);

        canRead = service.canRead(anonUserInfo, testMultiRepo);
        assertFalse(canRead, "Anonymous user should have permissions for this path");
        verify(configDescriptor, centralConfigServiceMock);
    }

    @Test
    public void hasPermissionForGroupInfo() {
        GroupInfo groupInfo = new GroupInfo("deployGroup");

        RepoPath testRepo1Path = new RepoPathImpl("testRepo1", "any/path");
        expect(repositoryServiceMock.localOrCachedRepositoryByKey("testRepo1"))
                .andReturn(localRepoMock).anyTimes();
        replay(repositoryServiceMock);

        expectGetAllAclsCallWithAnyArray();
        replay(internalAclManagerMock);
        boolean canRead = service.canRead(groupInfo, testRepo1Path);
        assertFalse(canRead, "Group should not have permissions for this path");
        verify(internalAclManagerMock);
        reset(internalAclManagerMock);

        expectGetAllAclsCallWithAnyArray();
        replay(internalAclManagerMock);
        boolean canDeploy = service.canDeploy(groupInfo, testRepo1Path);
        assertTrue(canDeploy, "Group should have permissions for this path");
        verify(internalAclManagerMock);
        reset(internalAclManagerMock);

        expectGetAllAclsCallWithAnyArray();
        replay(internalAclManagerMock);
        boolean canDelete = service.canDelete(groupInfo, testRepo1Path);
        assertFalse(canDelete, "Group should not have permissions for this path");
        verify(internalAclManagerMock);
        reset(internalAclManagerMock);

        expectGetAllAclsCallWithAnyArray();
        replay(internalAclManagerMock);
        boolean canAdmin = service.canAdmin(groupInfo, testRepo1Path);
        assertFalse(canAdmin, "Group should not have permissions for this path");
        verify(internalAclManagerMock);
        reset(internalAclManagerMock, repositoryServiceMock);

        RepoPath testRepo2Path = new RepoPathImpl("testRepo2", "some/path");

        expect(repositoryServiceMock.localOrCachedRepositoryByKey("testRepo2"))
                .andReturn(localRepoMock).anyTimes();
        replay(repositoryServiceMock);
        expectGetAllAclsCallWithAnyArray();
        replay(internalAclManagerMock);
        canRead = service.canRead(groupInfo, testRepo2Path);
        assertFalse(canRead, "Group should not have permissions for this path");
        verify(internalAclManagerMock, repositoryServiceMock);
        reset(internalAclManagerMock, repositoryServiceMock);

        GroupInfo anyRepoGroupRead = new GroupInfo("anyRepoReadersGroup");

        RepoPath somePath = new RepoPathImpl("blabla", "some/path");

        expect(repositoryServiceMock.localOrCachedRepositoryByKey("blabla")).andReturn(localRepoMock).anyTimes();
        expectGetAllAclsCallWithAnyArray();
        replay(internalAclManagerMock, repositoryServiceMock);
        canRead = service.canRead(anyRepoGroupRead, somePath);
        assertTrue(canRead, "Group should have permissions for this path");
        verify(internalAclManagerMock, repositoryServiceMock);
        reset(internalAclManagerMock);

        expectGetAllAclsCallWithAnyArray();
        replay(internalAclManagerMock);
        canDeploy = service.canDeploy(anyRepoGroupRead, somePath);
        assertFalse(canDeploy, "Group should not have permissions for this path");
        verify(internalAclManagerMock);
        reset(internalAclManagerMock, repositoryServiceMock);

        GroupInfo multiRepoGroupRead = new GroupInfo("multiRepoReadersGroup");

        RepoPath multiPath = new RepoPathImpl("multi1", "some/path");
        expect(repositoryServiceMock.localOrCachedRepositoryByKey("multi1")).andReturn(localRepoMock).anyTimes();
        expect(repositoryServiceMock.localOrCachedRepositoryByKey("multi2")).andReturn(localRepoMock).anyTimes();
        expectGetAllAclsCallWithAnyArray();
        replay(internalAclManagerMock, repositoryServiceMock);
        canRead = service.canRead(multiRepoGroupRead, multiPath);
        assertTrue(canRead, "Group should have permissions for this path");
        verify(internalAclManagerMock, repositoryServiceMock);
        reset(internalAclManagerMock);

        RepoPath multiPath2 = new RepoPathImpl("multi2", "some/path");
        expectGetAllAclsCallWithAnyArray();
        replay(internalAclManagerMock);
        canRead = service.canRead(multiRepoGroupRead, multiPath2);
        assertTrue(canRead, "Group should have permissions for this path");
        verify(internalAclManagerMock, repositoryServiceMock);
        reset(internalAclManagerMock);

        expectGetAllAclsCallWithAnyArray();
        replay(internalAclManagerMock);
        canDeploy = service.canDeploy(multiRepoGroupRead, multiPath);
        assertFalse(canDeploy, "Group should not have permissions for this path");
        verify(internalAclManagerMock, repositoryServiceMock);
    }

    @Test
    public void getAllPermissionTargetsForAdminUser() {
        setAdminAuthentication();

        expect(internalAclManagerMock.getAllPermissionTargets()).andReturn(permissionTargets);
        //expect(aclManagerMock.getAllAcls()).andReturn(testAcls);
        //expectGetAllAclsCall(authentication);
        replay(internalAclManagerMock);
        List<PermissionTargetInfo> permissionTargets = service.getPermissionTargets(ArtifactoryPermission.ADMIN);
        assertEquals(permissionTargets.size(), permissionTargets.size());
        verify(internalAclManagerMock);
    }

    @Test
    public void getAllPermissionTargetsForUserWithNoPermission() {
        setSimpleUserAuthentication("noadminpermissionsuser");

        expectAclScan();

        List<PermissionTargetInfo> permissionTargets = service.getPermissionTargets(ArtifactoryPermission.ADMIN);
        assertEquals(permissionTargets.size(), 0);

        verify(internalAclManagerMock);
    }

    @Test(enabled = false)
    public void getDeployPermissionTargetsForUserWithNoPermission() {
        setSimpleUserAuthentication("user");

        expectAclScan();

        List<PermissionTargetInfo> targets = service.getPermissionTargets(ArtifactoryPermission.DEPLOY);
        assertEquals(targets.size(), 0);

        verify(internalAclManagerMock);
    }

    @Test
    public void getDeployPermissionTargetsForUserWithDeployPermission() {
        setSimpleUserAuthentication("yossis");

        expectAclScan();

        List<PermissionTargetInfo> targets = service.getPermissionTargets(ArtifactoryPermission.DEPLOY);
        assertEquals(targets.size(), 1, "Expecting one deploy permission");

        verify(internalAclManagerMock);
    }

    @Test
    public void userPasswordMatches() {
        setSimpleUserAuthentication("user");

        assertTrue(service.userPasswordMatches("password"));
        assertFalse(service.userPasswordMatches(""));
        assertFalse(service.userPasswordMatches("Password"));
        assertFalse(service.userPasswordMatches("blabla"));
    }

    public void userReadAndDeployPermissionsOnAnyRemote() {
        Authentication authentication = setSimpleUserAuthentication();

        // can read the specified path
        RepoPath securedPath = new RepoPathImpl("repo1-cache", "blabla");
        expect(repositoryServiceMock.localOrCachedRepositoryByKey(securedPath.getRepoKey()))
                .andReturn(cacheRepoMock).anyTimes();
        ArtifactorySid[] sids = {new ArtifactorySid(authentication.getName())};
        expect(internalAclManagerMock.getAllAcls(aryEq(sids))).andReturn(createAnyRemotelAcl());

        verifyAnyRemoteOrAnyLocal(authentication, securedPath);
    }

    public void userReadAndDeployPermissionsOnAnyLocal() {
        Authentication authentication = setSimpleUserAuthentication();

        // can read the specified path
        RepoPath securedPath = new RepoPathImpl("local-repo", "mama");
        expect(repositoryServiceMock.localOrCachedRepositoryByKey(securedPath.getRepoKey()))
                .andReturn(localRepoMock).anyTimes();
        ArtifactorySid[] sids = {new ArtifactorySid(authentication.getName())};
        expect(internalAclManagerMock.getAllAcls(aryEq(sids))).andReturn(createAnyLocalAcl());

        verifyAnyRemoteOrAnyLocal(authentication, securedPath);
    }

    private void verifyAnyRemoteOrAnyLocal(Authentication authentication, RepoPath securedPath) {
        replay(internalAclManagerMock, repositoryServiceMock);
        boolean canRead = service.canRead(securedPath);
        assertTrue(canRead, "User should have permissions for this path");
        verify(internalAclManagerMock);
        reset(internalAclManagerMock);

        // can deploy to the specified path
        expectGetAllAclsCall(authentication);
        replay(internalAclManagerMock);

        boolean canDeploy = service.canDeploy(securedPath);
        assertFalse(canDeploy, "User should have permissions for this path");
        verify(internalAclManagerMock, repositoryServiceMock);
        reset(internalAclManagerMock, repositoryServiceMock);

        RepoPath allowedReadPath = new RepoPathImpl("testRepo1", "blabla");
        expect(repositoryServiceMock.localOrCachedRepositoryByKey(allowedReadPath.getRepoKey()))
                .andReturn(localRepoMock).anyTimes();
        replay(repositoryServiceMock);

        // can read the specified path
        expectGetAllAclsCall(authentication);
        replay(internalAclManagerMock);
        canRead = service.canRead(allowedReadPath);
        assertTrue(canRead, "User should have read permissions for this path");
        verify(internalAclManagerMock);
        reset(internalAclManagerMock);

        // cannot deploy to the specified path
        expectGetAllAclsCall(authentication);
        replay(internalAclManagerMock);
        canDeploy = service.canDeploy(allowedReadPath);
        assertFalse(canDeploy, "User should not have permissions for this path");
        verify(internalAclManagerMock, repositoryServiceMock);
        reset(internalAclManagerMock);

        // cannot admin the specified path
        expectGetAllAclsCall(authentication);
        replay(internalAclManagerMock);
        boolean canAdmin = service.canAdmin(allowedReadPath);
        assertFalse(canAdmin, "User should not have permissions for this path");
        verify(internalAclManagerMock);
    }

    private void expectAclScan() {
        expect(internalAclManagerMock.getAllPermissionTargets()).andReturn(permissionTargets).anyTimes();
        expect(internalAclManagerMock.findAclById(permissionTargets.get(0))).andReturn(testAcls.get(0));
        expect(internalAclManagerMock.findAclById(permissionTargets.get(1))).andReturn(testAcls.get(1));
        expect(internalAclManagerMock.findAclById(permissionTargets.get(2))).andReturn(testAcls.get(2));
        expect(internalAclManagerMock.findAclById(permissionTargets.get(3))).andReturn(testAcls.get(3));
        replay(internalAclManagerMock);
    }

    private void expectGetAllAclsCall(Authentication authentication) {
        ArtifactorySid[] sids = {new ArtifactorySid(authentication.getName())};
        expect(internalAclManagerMock.getAllAcls(aryEq(sids))).andReturn(testAcls);
    }

    private void expectGetAllAclsCallWithAnyArray() {
        expect(internalAclManagerMock.getAllAcls(new ArtifactorySid[]{(ArtifactorySid) anyObject()}))
                .andReturn(testAcls);
    }

    private List<Acl> createTestAcls() {
        userAndGroupSharedName = "usergroup";
        PermissionTargetInfo pmi = new PermissionTargetInfo("target1", Arrays.asList("testRepo1"));
        // yossis has all the permissions (when all permissions are checked)
        AceInfo adminAce = new AceInfo("yossis", false, ArtifactoryPermission.ADMIN.getMask());
        adminAce.setDeploy(true);
        adminAce.setRead(true);
        AceInfo readerAce = new AceInfo("user", false, ArtifactoryPermission.READ.getMask());
        AceInfo userGroupAce =
                new AceInfo(userAndGroupSharedName, false, ArtifactoryPermission.READ.getMask());
        AceInfo deployerGroupAce =
                new AceInfo("deployGroup", true, ArtifactoryPermission.DEPLOY.getMask());
        Set<AceInfo> aces = new HashSet<AceInfo>(
                Arrays.asList(adminAce, readerAce, userGroupAce, deployerGroupAce));
        Acl aclInfo = new Acl(new AclInfo(pmi, aces, "me"));

        PermissionTargetInfo pmi2 = new PermissionTargetInfo("target2", Arrays.asList("testRepo2"));
        AceInfo target2GroupAce = new AceInfo(userAndGroupSharedName, true,
                ArtifactoryPermission.READ.getMask());
        Set<AceInfo> target2Aces = new HashSet<AceInfo>(Arrays.asList(target2GroupAce));
        Acl aclInfo2 = new Acl(new AclInfo(pmi2, target2Aces, "me"));

        // acl for any repository with read permissions to group
        PermissionTargetInfo anyTarget = new PermissionTargetInfo("anyRepoTarget",
                Arrays.asList(PermissionTargetInfo.ANY_REPO));
        AceInfo readerGroupAce =
                new AceInfo("anyRepoReadersGroup", true, ArtifactoryPermission.READ.getMask());
        Set<AceInfo> anyTargetAces = new HashSet<AceInfo>(Arrays.asList(readerGroupAce));
        Acl anyTargetAcl = new Acl(new AclInfo(anyTarget, anyTargetAces, "me"));

        // acl with multiple repo keys with read permissions to group and anonymous
        PermissionTargetInfo multiReposTarget = new PermissionTargetInfo("anyRepoTarget",
                Arrays.asList("multi1", "multi2"));
        AceInfo multiReaderGroupAce =
                new AceInfo("multiRepoReadersGroup", true, ArtifactoryPermission.READ.getMask());
        AceInfo multiReaderAnonAce =
                new AceInfo(UserInfo.ANONYMOUS, false, ArtifactoryPermission.READ.getMask());
        Set<AceInfo> multiTargetAces = new HashSet<AceInfo>(Arrays.asList(multiReaderGroupAce, multiReaderAnonAce));
        Acl multiReposAcl = new Acl(new AclInfo(multiReposTarget, multiTargetAces, "me"));

        List<Acl> acls = Arrays.asList(aclInfo, aclInfo2, anyTargetAcl, multiReposAcl);
        permissionTargets = Arrays.asList(
                aclInfo.getPermissionTarget(), aclInfo2.getPermissionTarget(),
                anyTargetAcl.getPermissionTarget(), multiReposAcl.getPermissionTarget());
        return acls;
    }

    private List<Acl> createAnyRemotelAcl() {
        PermissionTargetInfo pmi = new PermissionTargetInfo(
                "target1", Arrays.asList(PermissionTargetInfo.ANY_REMOTE_REPO));
        AceInfo readerAndDeployer = new AceInfo("user", false,
                ArtifactoryPermission.READ.getMask() | ArtifactoryPermission.DEPLOY.getMask());
        Set<AceInfo> aces = new HashSet<AceInfo>(Arrays.asList(readerAndDeployer));
        Acl aclInfo = new Acl(new AclInfo(pmi, aces, "me"));

        return Arrays.asList(aclInfo);
    }

    private List<Acl> createAnyLocalAcl() {
        PermissionTargetInfo pmi = new PermissionTargetInfo(
                "target1", Arrays.asList(PermissionTargetInfo.ANY_LOCAL_REPO));
        AceInfo readerAndDeployer = new AceInfo("user", false,
                ArtifactoryPermission.READ.getMask() | ArtifactoryPermission.DEPLOY.getMask());
        Set<AceInfo> aces = new HashSet<AceInfo>(Arrays.asList(readerAndDeployer));
        Acl aclInfo = new Acl(new AclInfo(pmi, aces, "me"));

        return Arrays.asList(aclInfo);
    }

    private Authentication setAdminAuthentication() {
        SimpleUser adminUser = createAdminUser();
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                adminUser, null, SimpleUser.ADMIN_GAS);
        securityContext.setAuthentication(authenticationToken);
        return authenticationToken;
    }

    private Authentication setSimpleUserAuthentication() {
        return setSimpleUserAuthentication("user");
    }

    private Authentication setSimpleUserAuthentication(String username, String... groups) {
        SimpleUser simpleUser = createNonAdminUser(username, groups);
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                simpleUser, "password", SimpleUser.USER_GAS);
        securityContext.setAuthentication(authenticationToken);
        return authenticationToken;
    }

    private static SimpleUser createNonAdminUser(String username, String... groups) {
        UserInfo userInfo = new UserInfoBuilder(username).updatableProfile(true)
                .internalGroups(new HashSet<String>(Arrays.asList(groups))).build();
        return new SimpleUser(userInfo);
    }

    private static SimpleUser createAdminUser() {
        UserInfo userInfo = new UserInfoBuilder("spiderman").admin(true).updatableProfile(true).build();
        return new SimpleUser(userInfo);
    }

    private static LocalRepo createLocalRepoMock() {
        LocalRepo localRepo = createMock(LocalRepo.class);
        expect(localRepo.isLocal()).andReturn(true).anyTimes();
        expect(localRepo.isCache()).andReturn(false).anyTimes();
        expect(localRepo.isAnonAccessEnabled()).andReturn(true).anyTimes();
        replay(localRepo);
        return localRepo;
    }

    private static LocalRepo createCacheRepoMock() {
        LocalRepo localRepo = createMock(LocalRepo.class);
        expect(localRepo.isLocal()).andReturn(true).anyTimes();
        expect(localRepo.isCache()).andReturn(true).anyTimes();
        expect(localRepo.isAnonAccessEnabled()).andReturn(false).anyTimes();
        replay(localRepo);
        return localRepo;
    }

}
