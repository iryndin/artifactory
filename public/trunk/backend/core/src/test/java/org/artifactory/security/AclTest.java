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

import org.artifactory.factory.InfoFactory;
import org.artifactory.factory.InfoFactoryHolder;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.testng.Assert.assertEquals;

/**
 * Acl unit tests.
 *
 * @author Yossi Shaul
 */
@Test
public class AclTest {
    private InfoFactory factory = InfoFactoryHolder.get();

    public void createFromAclInfo() {
        MutablePermissionTargetInfo permissionTarget = factory.createPermissionTarget("testPerm",
                Arrays.asList("repoblabla", "repo2"));
        MutableAceInfo ace1 = factory.createAce("user1", false, ArtifactoryPermission.DEPLOY.getMask());
        MutableAceInfo ace2 = factory.createAce("user2", false, ArtifactoryPermission.READ.getMask());
        MutableAceInfo ace3 = factory.createAce("group1", true, ArtifactoryPermission.ADMIN.getMask());

        Set<AceInfo> aces = new HashSet<AceInfo>(Arrays.asList(ace1, ace2, ace3));

        MutableAclInfo aclInfo = factory.createAcl(permissionTarget, aces, "momo");
        Acl acl = new Acl(aclInfo);

        assertEquals(acl.getUpdatedBy(), "momo", "Updated by mismatch");
        assertEquals(acl.getAces().size(), aces.size());
        assertEquals(acl.getPermissionTarget(), new PermissionTarget(permissionTarget));

        // test the returned duplicate descriptor
        AclInfo descriptor = acl.getDescriptor();
        assertEquals(descriptor.getUpdatedBy(), aclInfo.getUpdatedBy());
        assertEquals(descriptor.getAces().size(), aclInfo.getAces().size());
        assertEquals(descriptor.getPermissionTarget(), aclInfo.getPermissionTarget());
    }
}
