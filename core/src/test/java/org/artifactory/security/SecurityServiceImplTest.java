package org.artifactory.security;

import org.artifactory.api.repo.RepoPath;
import org.artifactory.api.security.AceInfo;
import org.artifactory.api.security.AclInfo;
import org.artifactory.api.security.ArtifactoryPermisssion;
import org.artifactory.api.security.GroupInfo;
import org.artifactory.api.security.PermissionTargetInfo;
import org.artifactory.api.security.UserInfo;
import static org.easymock.EasyMock.*;
import org.springframework.security.Authentication;
import org.springframework.security.context.SecurityContextHolder;
import org.springframework.security.context.SecurityContextImpl;
import org.springframework.security.providers.TestingAuthenticationToken;
import org.springframework.test.util.ReflectionTestUtils;
import static org.testng.Assert.*;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * SecurityServiceImpl unit tests.
 * TODO: simplify the tests
 *
 * @author Yossi Shaul
 */
@Test
public class SecurityServiceImplTest {
    private SecurityContextImpl securityContext;
    private SecurityServiceImpl service;
    private List<Acl> testAcls;
    private AclManager aclManagerMock;
    private String userAndGroupSharedName;
    private List<PermissionTarget> permissionTargets;

    @BeforeClass
    public void initArtifactoryRoles() {
        testAcls = createTestAcls();
        aclManagerMock = createMock(AclManager.class);
    }

    @BeforeMethod
    public void setUp() {
        // create new security context
        securityContext = new SecurityContextImpl();
        SecurityContextHolder.setContext(securityContext);

        // new service instance
        service = new SecurityServiceImpl();
        // set the aclManager mock on the security service
        ReflectionTestUtils.setField(service, "aclManager", aclManagerMock);

        // reset mock
        reset(aclManagerMock);
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

        RepoPath path = new RepoPath("smoeRepo", "blabla");

        boolean canRead = service.canRead(path);
        assertTrue(canRead);
        boolean canDeploy = service.canDeploy(path);
        assertTrue(canDeploy);
    }

    @Test
    public void userReadAndDeployPermissions() {
        Authentication authentication = setSimpleUserAuthentication();

        RepoPath securedPath = new RepoPath("securedRepo", "blabla");

        // cannot read the specified path
        expectGetAllAclsCall(authentication);
        replay(aclManagerMock);
        boolean canRead = service.canRead(securedPath);
        assertFalse(canRead, "User should not have permissions for this path");
        verify(aclManagerMock);
        reset(aclManagerMock);

        // cannot deploy to the specified path
        expectGetAllAclsCall(authentication);
        replay(aclManagerMock);
        boolean canDeploy = service.canDeploy(securedPath);
        assertFalse(canDeploy, "User should not have permissions for this path");
        verify(aclManagerMock);
        reset(aclManagerMock);

        RepoPath allowedReadPath = new RepoPath("testRepo1", "blabla");

        // can read the specified path
        expectGetAllAclsCall(authentication);
        replay(aclManagerMock);
        canRead = service.canRead(allowedReadPath);
        assertTrue(canRead, "User should have read permissions for this path");
        verify(aclManagerMock);
        reset(aclManagerMock);

        // cannot deploy to the specified path
        expectGetAllAclsCall(authentication);
        replay(aclManagerMock);
        canDeploy = service.canDeploy(allowedReadPath);
        assertFalse(canDeploy, "User should not have permissions for this path");
        verify(aclManagerMock);
        reset(aclManagerMock);

        // cannot admin the specified path
        expectGetAllAclsCall(authentication);
        replay(aclManagerMock);
        boolean canAdmin = service.canAdmin(allowedReadPath);
        assertFalse(canAdmin, "User should not have permissions for this path");
        verify(aclManagerMock);
        reset(aclManagerMock);
    }

    @Test
    public void adminRolePermissions() {
        // user with admin role on permission target 'target1'
        Authentication authentication = setSimpleUserAuthentication("yossis");

        RepoPath allowedReadPath = new RepoPath("testRepo1", "blabla");

        // can read the specified path
        expectGetAllAclsCall(authentication);
        replay(aclManagerMock);
        boolean canRead = service.canRead(allowedReadPath);
        assertTrue(canRead, "User should have permissions for this path");
        verify(aclManagerMock);
        reset(aclManagerMock);

        // can deploy to the specified path
        expectGetAllAclsCall(authentication);
        replay(aclManagerMock);
        boolean canDeploy = service.canDeploy(allowedReadPath);
        assertTrue(canDeploy, "User should have permissions for this path");
        verify(aclManagerMock);
        reset(aclManagerMock);

        // can admin the specified path
        expectGetAllAclsCall(authentication);
        replay(aclManagerMock);
        boolean canAdmin = service.canAdmin(allowedReadPath);
        assertTrue(canDeploy, "User should have permissions for this path");
        verify(aclManagerMock);
        reset(aclManagerMock);

        // can admin
        Acl testRepo1Acl = testAcls.get(0);
        PermissionTarget target = testRepo1Acl.getPermissionTarget();
        expect(aclManagerMock.findAclById(target)).andReturn(testRepo1Acl);
        replay(aclManagerMock);
        boolean canAdminTarget = service.canAdmin(target.getDescriptor());
        assertTrue(canAdminTarget, "User should have admin permissions for this target");
        verify(aclManagerMock);
    }

    @Test
    public void groupPermissions() {
        Authentication authentication = setSimpleUserAuthentication("userwithnopermissions");

        RepoPath allowedReadPath = new RepoPath("testRepo1", "**");

        // cannot deploy to the specified path
        expectGetAllAclsCall(authentication);
        replay(aclManagerMock);
        boolean canDeploy = service.canDeploy(allowedReadPath);
        assertFalse(canDeploy, "User should have permissions for this path");
        verify(aclManagerMock);
        reset(aclManagerMock);

        // add the user to a group with permissions and expext permission garnted
        setSimpleUserAuthentication("userwithnopermissions", "deployGroup");
        expectGetAllAclsCallWithAnyArray();
        replay(aclManagerMock);
        canDeploy = service.canDeploy(allowedReadPath);
        assertTrue(canDeploy, "User in a group with permissions for this path");
        verify(aclManagerMock);
        reset(aclManagerMock);
    }

    @Test
    public void userWithPermissionsToAGroupWithTheSameName() {
        setSimpleUserAuthentication(userAndGroupSharedName, userAndGroupSharedName);

        RepoPath testRepo1Path = new RepoPath("testRepo1", "**");
        expectGetAllAclsCallWithAnyArray();
        replay(aclManagerMock);
        boolean canRead = service.canRead(testRepo1Path);
        assertTrue(canRead, "User should have permissions for this path");
        verify(aclManagerMock);
        reset(aclManagerMock);

        RepoPath testRepo2Path = new RepoPath("testRepo2", "**");
        expectGetAllAclsCallWithAnyArray();
        replay(aclManagerMock);
        canRead = service.canRead(testRepo2Path);
        assertTrue(canRead, "User belongs to a group with permissions to the path");
        verify(aclManagerMock);
        reset(aclManagerMock);
    }

    @Test
    public void userWithPermissionsToANonUniqueGroupName() {
        // here we test that a user that belongs to a group which has
        // the same name of a nother user will only get the group permissions
        // and not the user with the same name permissions
        setSimpleUserAuthentication("auser", userAndGroupSharedName);

        RepoPath testRepo1Path = new RepoPath("testRepo1", "**");
        expectGetAllAclsCallWithAnyArray();
        replay(aclManagerMock);
        boolean canRead = service.canRead(testRepo1Path);
        assertFalse(canRead, "User should not have permissions for this path");
        verify(aclManagerMock);
        reset(aclManagerMock);

        RepoPath testRepo2Path = new RepoPath("testRepo2", "**");
        expectGetAllAclsCallWithAnyArray();
        replay(aclManagerMock);
        canRead = service.canRead(testRepo2Path);
        assertTrue(canRead, "User belongs to a group with permissions to the path");
        verify(aclManagerMock);
        reset(aclManagerMock);
    }

    @Test
    public void hasPermissionPassingUserInfo() {
        SimpleUser user = createNonAdminUser("yossis");
        UserInfo userInfo = user.getDescriptor();

        RepoPath testRepo1Path = new RepoPath("testRepo1", "any/path");

        expectGetAllAclsCallWithAnyArray();
        replay(aclManagerMock);
        boolean canRead = service.canRead(userInfo, testRepo1Path);
        assertTrue(canRead, "User should have permissions for this path");
        verify(aclManagerMock);
        reset(aclManagerMock);

        expectGetAllAclsCallWithAnyArray();
        replay(aclManagerMock);
        boolean canDeploy = service.canDeploy(userInfo, testRepo1Path);
        assertTrue(canDeploy, "User should have permissions for this path");
        verify(aclManagerMock);
        reset(aclManagerMock);

        expectGetAllAclsCallWithAnyArray();
        replay(aclManagerMock);
        boolean canDelete = service.canDelete(userInfo, testRepo1Path);
        assertFalse(canDelete, "User should not have permissions for this path");
        verify(aclManagerMock);
        reset(aclManagerMock);

        expectGetAllAclsCallWithAnyArray();
        replay(aclManagerMock);
        boolean canAdmin = service.canAdmin(userInfo, testRepo1Path);
        assertTrue(canAdmin, "User should have permissions for this path");
        verify(aclManagerMock);
        reset(aclManagerMock);

        RepoPath testRepo2Path = new RepoPath("testRepo2", "**");

        expectGetAllAclsCallWithAnyArray();
        replay(aclManagerMock);
        canRead = service.canRead(userInfo, testRepo2Path);
        assertFalse(canRead, "User should not have permissions for this path");
        verify(aclManagerMock);
        reset(aclManagerMock);
    }

    @Test
    public void hasPermissionForGroupInfo() {
        GroupInfo groupInfo = new GroupInfo("deployGroup");

        RepoPath testRepo1Path = new RepoPath("testRepo1", "any/path");

        expectGetAllAclsCallWithAnyArray();
        replay(aclManagerMock);
        boolean canRead = service.canRead(groupInfo, testRepo1Path);
        assertFalse(canRead, "Group should not have permissions for this path");
        verify(aclManagerMock);
        reset(aclManagerMock);

        expectGetAllAclsCallWithAnyArray();
        replay(aclManagerMock);
        boolean canDeploy = service.canDeploy(groupInfo, testRepo1Path);
        assertTrue(canDeploy, "Group should have permissions for this path");
        verify(aclManagerMock);
        reset(aclManagerMock);

        expectGetAllAclsCallWithAnyArray();
        replay(aclManagerMock);
        boolean canDelete = service.canDelete(groupInfo, testRepo1Path);
        assertFalse(canDelete, "Group should not have permissions for this path");
        verify(aclManagerMock);
        reset(aclManagerMock);

        expectGetAllAclsCallWithAnyArray();
        replay(aclManagerMock);
        boolean canAdmin = service.canAdmin(groupInfo, testRepo1Path);
        assertFalse(canAdmin, "Group should not have permissions for this path");
        verify(aclManagerMock);
        reset(aclManagerMock);

        RepoPath testRepo2Path = new RepoPath("testRepo2", "some/path");
        expectGetAllAclsCallWithAnyArray();
        replay(aclManagerMock);
        canRead = service.canRead(groupInfo, testRepo2Path);
        assertFalse(canRead, "Group should not have permissions for this path");
        verify(aclManagerMock);
        reset(aclManagerMock);

        GroupInfo anyRepoGroupRead = new GroupInfo("anyRepoReadersGroup");

        RepoPath somePath = new RepoPath("blabla", "some/path");
        expectGetAllAclsCallWithAnyArray();
        replay(aclManagerMock);
        canRead = service.canRead(anyRepoGroupRead, somePath);
        assertTrue(canRead, "Group should have permissions for this path");
        verify(aclManagerMock);
        reset(aclManagerMock);

        expectGetAllAclsCallWithAnyArray();
        replay(aclManagerMock);
        canDeploy = service.canDeploy(anyRepoGroupRead, somePath);
        assertFalse(canDeploy, "Group should not have permissions for this path");
        verify(aclManagerMock);
        reset(aclManagerMock);
    }

    @Test
    public void getAllPermissionTargetsForAdminUser() {
        Authentication authentication = setAdminAuthentication();

        expect(aclManagerMock.getAllPermissionTargets()).andReturn(permissionTargets);
        //expect(aclManagerMock.getAllAcls()).andReturn(testAcls);
        //expectGetAllAclsCall(authentication);
        replay(aclManagerMock);
        List<PermissionTargetInfo> permissionTargets = service.getAdministrativePermissionTargets();
        assertEquals(permissionTargets.size(), permissionTargets.size());
        verify(aclManagerMock);
    }

    @Test
    public void getAllPermissionTargetsForUserWithNoPermission() {
        setSimpleUserAuthentication("noadminpermissionsuser");

        expectAclScan();

        List<PermissionTargetInfo> permissionTargets = service.getAdministrativePermissionTargets();
        assertEquals(permissionTargets.size(), 0);

        verify(aclManagerMock);
    }

    @Test
    public void getDeployPermissionTargetsForUserWithNoPermission() {
        setSimpleUserAuthentication("user");

        expectAclScan();

        List<PermissionTargetInfo> targets = service.getDeployablePermissionTargets();
        assertEquals(targets.size(), 0);

        verify(aclManagerMock);
    }

    @Test
    public void getDeployPermissionTargetsForUserWithDeployPermission() {
        setSimpleUserAuthentication("yossis");

        expectAclScan();

        List<PermissionTargetInfo> targets = service.getDeployablePermissionTargets();
        assertEquals(targets.size(), 1, "Expecting one deploy permission");

        verify(aclManagerMock);
    }

    private void expectAclScan() {
        expect(aclManagerMock.getAllPermissionTargets()).andReturn(permissionTargets);
        expect(aclManagerMock.findAclById(permissionTargets.get(0))).andReturn(testAcls.get(0));
        expect(aclManagerMock.findAclById(permissionTargets.get(1))).andReturn(testAcls.get(1));
        expect(aclManagerMock.findAclById(permissionTargets.get(2))).andReturn(testAcls.get(2));
        replay(aclManagerMock);
    }

    private void expectGetAllAclsCall(Authentication authentication) {
        ArtifactorySid[] sids = {new ArtifactorySid(authentication.getName())};
        expect(aclManagerMock.getAllAcls(aryEq(sids))).andReturn(testAcls);
    }

    private void expectGetAllAclsCallWithAnyArray() {
        expect(aclManagerMock.getAllAcls(new ArtifactorySid[]{(ArtifactorySid) anyObject()}))
                .andReturn(testAcls);
    }

    private List<Acl> createTestAcls() {
        userAndGroupSharedName = "usergroup";
        PermissionTargetInfo pmi = new PermissionTargetInfo("target1", "testRepo1");
        // yossis has all the permissions (when all permissions are checked)
        AceInfo adminAce = new AceInfo("yossis", false, ArtifactoryPermisssion.ADMIN.getMask());
        adminAce.setDeploy(true);
        adminAce.setRead(true);
        AceInfo readerAce = new AceInfo("user", false, ArtifactoryPermisssion.READ.getMask());
        AceInfo userGroupAce =
                new AceInfo(userAndGroupSharedName, false, ArtifactoryPermisssion.READ.getMask());
        AceInfo deployerGroupAce =
                new AceInfo("deployGroup", true, ArtifactoryPermisssion.DEPLOY.getMask());
        Set<AceInfo> aces = new HashSet<AceInfo>(
                Arrays.asList(adminAce, readerAce, userGroupAce, deployerGroupAce));
        Acl aclInfo = new Acl(new AclInfo(pmi, aces, "me"));

        PermissionTargetInfo pmi2 = new PermissionTargetInfo("target2", "testRepo2");
        AceInfo target2GroupAce = new AceInfo(userAndGroupSharedName, true,
                ArtifactoryPermisssion.READ.getMask());
        Set<AceInfo> target2Aces = new HashSet<AceInfo>(Arrays.asList(target2GroupAce));
        Acl aclInfo2 = new Acl(new AclInfo(pmi2, target2Aces, "me"));

        // acl for any repository with read permissions to group
        PermissionTargetInfo anyTarget = new PermissionTargetInfo("anyRepoTarget",
                PermissionTargetInfo.ANY_REPO);
        AceInfo readerGroupAce =
                new AceInfo("anyRepoReadersGroup", true, ArtifactoryPermisssion.READ.getMask());
        Set<AceInfo> anyTargetAces = new HashSet<AceInfo>(Arrays.asList(readerGroupAce));
        Acl anyTargetAcl = new Acl(new AclInfo(anyTarget, anyTargetAces, "me"));

        List<Acl> acls = Arrays.asList(aclInfo, aclInfo2, anyTargetAcl);
        permissionTargets = Arrays.asList(
                aclInfo.getPermissionTarget(), aclInfo2.getPermissionTarget(),
                anyTargetAcl.getPermissionTarget());
        return acls;
    }

    private Authentication setAdminAuthentication() {
        SimpleUser adminUser = createAdminUser();
        TestingAuthenticationToken authenticationToken = new TestingAuthenticationToken(
                adminUser, null, SimpleUser.ADMIN_GAS);
        authenticationToken.setAuthenticated(true);
        securityContext.setAuthentication(authenticationToken);
        return authenticationToken;
    }

    private Authentication setSimpleUserAuthentication() {
        return setSimpleUserAuthentication("user");
    }

    private Authentication setSimpleUserAuthentication(String username, String... groups) {
        SimpleUser simpleUser = createNonAdminUser(username, groups);
        TestingAuthenticationToken authenticationToken = new TestingAuthenticationToken(
                simpleUser, null, SimpleUser.USER_GAS);
        authenticationToken.setAuthenticated(true);
        securityContext.setAuthentication(authenticationToken);
        return authenticationToken;
    }

    private SimpleUser createNonAdminUser(String username, String... groups) {
        UserInfo userInfo = new UserInfo(username, "", "", false, true, true, true, true, true);
        userInfo.setGroups(new HashSet<String>(Arrays.asList(groups)));
        return new SimpleUser(userInfo);
    }

    private SimpleUser createAdminUser() {
        return new SimpleUser("spiderman", "", "", true, true, true, true, true, true);
    }

}
