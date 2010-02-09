package org.artifactory.security;

import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import org.artifactory.api.security.AceInfo;
import org.artifactory.api.security.AclInfo;
import org.artifactory.api.security.ArtifactoryPermisssion;
import org.artifactory.api.security.PermissionTargetInfo;
import org.easymock.classextension.EasyMock;
import org.springframework.test.util.ReflectionTestUtils;
import static org.testng.Assert.*;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * JcrAclManager unit tests.
 *
 * @author Yossi Shaul
 */
@Test
public class JcrAclManagerTest {
    private JcrAclManager manager;
    private List<Acl> testAcls;
    private Acl mixedAcl;
    private Acl testRepo2Acl;
    private Acl anyTargetAcl;

    @BeforeMethod
    public void setUp() {
        manager = new JcrAclManager();
        Ehcache aclsCacheMock = EasyMock.createNiceMock(Ehcache.class);
        ReflectionTestUtils.setField(manager, "aclsCache", aclsCacheMock);
        testAcls = createTestAcls();

        Element element = new Element(JcrAclManager.ACLS_KEY, testAcls);
        EasyMock.expect(aclsCacheMock.get(JcrAclManager.ACLS_KEY)).andReturn(element);
        EasyMock.replay(aclsCacheMock);
    }

    public void getAllAcls() {
        List<Acl> allAcls = manager.getAllAcls();
        assertNotNull(allAcls);
        assertEquals(allAcls.size(), testAcls.size());
    }

    public void getAllAclsBySingleSid() {
        ArtifactorySid[] sids = {new ArtifactorySid("yossis")};
        List<Acl> sidAcls = manager.getAllAcls(sids);
        assertEquals(sidAcls.size(), 1);
        assertEquals(sidAcls.get(0).getId(), "target1");
    }

    public void getAllAclsOfUsersWithNone() {
        ArtifactorySid[] sids = {new ArtifactorySid("koko"), new ArtifactorySid("loko")};
        List<Acl> sidAcls = manager.getAllAcls(sids);
        assertTrue(sidAcls.isEmpty());
    }

    public void getAllAclsOfUserWithMultipleMatches() {
        ArtifactorySid[] sids = {new ArtifactorySid("user")};
        List<Acl> sidAcls = manager.getAllAcls(sids);
        assertEquals(sidAcls.size(), 2);
    }

    public void getAllAclsForGroup() {
        ArtifactorySid[] sids = {new ArtifactorySid("deployGroup", true)};
        List<Acl> sidAcls = manager.getAllAcls(sids);
        assertEquals(sidAcls.size(), 1);
        assertEquals(sidAcls.get(0).getId(), "target1");
    }

    public void getAllAclsForMixedGroupAndUser() {
        ArtifactorySid[] sids =
                {new ArtifactorySid("anyRepoReadersGroup", true), new ArtifactorySid("yossis")};
        List<Acl> sidAcls = manager.getAllAcls(sids);
        assertEquals(sidAcls.size(), 2);
        assertTrue(sidAcls.contains(mixedAcl));
        assertTrue(sidAcls.contains(anyTargetAcl));
    }

    private List<Acl> createTestAcls() {
        PermissionTargetInfo pti1 = new PermissionTargetInfo("target1", "testRepo1");
        AceInfo yoavAce = new AceInfo("yoavl", false, ArtifactoryPermisssion.READ.getMask());
        // yossis has all the permissions (when all permissions are checked)
        AceInfo adminAce = new AceInfo("yossis", false, ArtifactoryPermisssion.ADMIN.getMask());
        adminAce.setDeploy(true);
        adminAce.setRead(true);
        AceInfo readerAce = new AceInfo("user", false, ArtifactoryPermisssion.READ.getMask());
        AceInfo deployerGroupAce =
                new AceInfo("deployGroup", true, ArtifactoryPermisssion.DEPLOY.getMask());
        Set<AceInfo> aces = new HashSet<AceInfo>(
                Arrays.asList(yoavAce, adminAce, readerAce, deployerGroupAce));
        mixedAcl = new Acl(new AclInfo(pti1, aces, "me"));

        // another acl
        PermissionTargetInfo pti2 = new PermissionTargetInfo("target2", "testRepo2");
        AceInfo deployersTestRepo2 =
                new AceInfo("deployersTestRepo2", true, ArtifactoryPermisssion.DEPLOY.getMask());
        AceInfo adminTestRepo2 =
                new AceInfo("adminTestRepo2", true, ArtifactoryPermisssion.ADMIN.getMask());
        Set<AceInfo> testRepo2Aces = new HashSet<AceInfo>(
                Arrays.asList(deployersTestRepo2, adminTestRepo2, readerAce));
        testRepo2Acl = new Acl(new AclInfo(pti2, testRepo2Aces, "admin"));

        // acl for any repository with read permissions to group
        PermissionTargetInfo anyTarget = new PermissionTargetInfo("anyRepoTarget",
                PermissionTargetInfo.ANY_REPO);
        AceInfo readerGroupAce =
                new AceInfo("anyRepoReadersGroup", true, ArtifactoryPermisssion.READ.getMask());
        Set<AceInfo> anyTargetAces = new HashSet<AceInfo>(Arrays.asList(readerGroupAce, yoavAce));
        anyTargetAcl = new Acl(new AclInfo(anyTarget, anyTargetAces, "me"));

        List<Acl> acls = Arrays.asList(mixedAcl, anyTargetAcl, testRepo2Acl);
        return acls;
    }

}
