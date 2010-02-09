/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2010 JFrog Ltd.
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

import org.artifactory.api.security.AceInfo;
import org.artifactory.api.security.AclInfo;
import org.artifactory.api.security.ArtifactoryPermission;
import org.artifactory.api.security.PermissionTargetInfo;
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

    public void createFromAclInfo() {
        PermissionTargetInfo permissionTarget =
                new PermissionTargetInfo("testPerm", Arrays.asList("repoblabla", "repo2"), "**", "");
        AceInfo ace1 = new AceInfo("user1", false, ArtifactoryPermission.DEPLOY.getMask());
        AceInfo ace2 = new AceInfo("user2", false, ArtifactoryPermission.READ.getMask());
        AceInfo ace3 = new AceInfo("group1", true, ArtifactoryPermission.ADMIN.getMask());

        Set<AceInfo> aces = new HashSet<AceInfo>(Arrays.asList(ace1, ace2, ace3));

        AclInfo aclInfo = new AclInfo(permissionTarget, aces, "momo");
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
