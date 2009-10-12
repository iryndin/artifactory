package org.artifactory.security;

import org.artifactory.api.security.AceInfo;
import org.artifactory.api.security.AclInfo;
import org.artifactory.api.security.ArtifactoryPermisssion;
import org.artifactory.api.security.PermissionTargetInfo;
import static org.testng.Assert.assertEquals;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Acl unit tests.
 *
 * @author Yossi Shaul
 */
@Test
public class AclTest {

    public void createFromAclInfo() {
        PermissionTargetInfo permissionTarget =
                new PermissionTargetInfo("testPerm", "repoblabla", "**", "");
        AceInfo ace1 = new AceInfo("user1", false, ArtifactoryPermisssion.DEPLOY.getMask());
        AceInfo ace2 = new AceInfo("user2", false, ArtifactoryPermisssion.READ.getMask());
        AceInfo ace3 = new AceInfo("group1", true, ArtifactoryPermisssion.ADMIN.getMask());

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
