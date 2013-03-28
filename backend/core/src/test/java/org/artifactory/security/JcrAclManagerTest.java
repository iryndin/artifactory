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

package org.artifactory.security;

import com.google.common.collect.MapMaker;
import org.artifactory.factory.InfoFactory;
import org.artifactory.factory.InfoFactoryHolder;
import org.artifactory.jcr.JcrService;
import org.artifactory.security.jcr.JcrAclManager;
import org.easymock.EasyMock;
import org.springframework.test.util.ReflectionTestUtils;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.testng.Assert.*;

/**
 * JcrAclManager unit tests.
 *
 * @author Yossi Shaul
 */
@Test
public class JcrAclManagerTest {

    private InfoFactory factory = InfoFactoryHolder.get();

    private JcrAclManager manager;
    private List<Acl> testAcls;
    private Acl mixedAcl;
    private Acl anyTargetAcl;

    @BeforeMethod
    public void setUp() {
        manager = new JcrAclManager();
        DummyOcm dummyOcm = new DummyOcm();
        Map<String, Acl> cache = new MapMaker().initialCapacity(30).makeMap();
        ReflectionTestUtils.setField(manager, "acls", cache);
        testAcls = createTestAcls();

        JcrService jcr = EasyMock.createNiceMock(JcrService.class);
        ReflectionTestUtils.setField(manager, "jcr", jcr);
        EasyMock.expect(jcr.getOcm()).andReturn(dummyOcm).anyTimes();
        EasyMock.replay(jcr);
        for (Acl acl : testAcls) {
            manager.createAcl(acl);
        }
        for (Acl acl : testAcls) {
            Assert.assertEquals(acl, cache.get(acl.getPermissionTarget().getName()));
        }
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
        PermissionTargetInfo pti1 = InfoFactoryHolder.get().createPermissionTarget("target1",
                Arrays.asList("testRepo1"));
        MutableAceInfo yoavAce = factory.createAce("yoavl", false, ArtifactoryPermission.READ.getMask());
        // yossis has all the permissions (when all permissions are checked)
        MutableAceInfo adminAce = factory.createAce("yossis", false, ArtifactoryPermission.ADMIN.getMask());
        adminAce.setDeploy(true);
        adminAce.setRead(true);
        MutableAceInfo readerAce = factory.createAce("user", false, ArtifactoryPermission.READ.getMask());
        MutableAceInfo deployerGroupAce =
                factory.createAce("deployGroup", true, ArtifactoryPermission.DEPLOY.getMask());
        Set<AceInfo> aces = new HashSet<AceInfo>(
                Arrays.asList(yoavAce, adminAce, readerAce, deployerGroupAce));
        mixedAcl = new Acl(factory.createAcl(pti1, aces, "me"));

        // another acl
        PermissionTargetInfo pti2 = InfoFactoryHolder.get().createPermissionTarget("target2",
                Arrays.asList("testRepo2"));
        MutableAceInfo deployersTestRepo2 =
                factory.createAce("deployersTestRepo2", true, ArtifactoryPermission.DEPLOY.getMask());
        MutableAceInfo adminTestRepo2 =
                factory.createAce("adminTestRepo2", true, ArtifactoryPermission.ADMIN.getMask());
        Set<AceInfo> testRepo2Aces = new HashSet<AceInfo>(
                Arrays.asList(deployersTestRepo2, adminTestRepo2, readerAce));
        Acl testRepo2Acl = new Acl(factory.createAcl(pti2, testRepo2Aces, "admin"));

        // acl for any repository with read permissions to group
        PermissionTargetInfo anyTarget = InfoFactoryHolder.get().createPermissionTarget("anyRepoTarget",
                Arrays.asList(PermissionTargetInfo.ANY_REPO));
        MutableAceInfo readerGroupAce =
                factory.createAce("anyRepoReadersGroup", true, ArtifactoryPermission.READ.getMask());
        Set<AceInfo> anyTargetAces = new HashSet<AceInfo>(Arrays.asList(readerGroupAce, yoavAce));
        anyTargetAcl = new Acl(factory.createAcl(anyTarget, anyTargetAces, "me"));

        List<Acl> acls = Arrays.asList(mixedAcl, anyTargetAcl, testRepo2Acl);
        return acls;
    }
}