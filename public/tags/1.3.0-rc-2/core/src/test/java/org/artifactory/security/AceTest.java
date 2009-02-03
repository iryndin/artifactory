package org.artifactory.security;

import org.artifactory.api.security.AceInfo;
import org.artifactory.api.security.ArtifactoryPermisssion;
import org.springframework.security.acls.domain.BasePermission;
import static org.testng.Assert.*;
import org.testng.annotations.Test;

/**
 * Ace unit tests.
 *
 * @author Yossi Shaul
 */
@Test
public class AceTest {

    public void createForUserFromAceInfo() {
        AceInfo aceInfo = new AceInfo("momo", false, ArtifactoryPermisssion.ADMIN.getMask());
        Ace ace = new Ace(new Acl(), aceInfo);

        // now test the getters
        assertEquals(ace.getPrincipal(), aceInfo.getPrincipal(), "Principal mismatch");
        assertEquals(ace.getMask(), aceInfo.getMask(), "Mask mismatch");
        assertFalse(ace.isGroup(), "Not a group");

        // test descriptor doesn't loose data
        assertReturnedDescriptorEquals(aceInfo, ace);
    }

    public void createForGroupFromAceInfo() {
        AceInfo aceInfo = new AceInfo("momo", true, ArtifactoryPermisssion.DEPLOY.getMask());
        Ace ace = new Ace(new Acl(), aceInfo);

        // now test the getters
        assertEquals(ace.getPrincipal(), aceInfo.getPrincipal(), "Principal mismatch");
        assertEquals(ace.getMask(), aceInfo.getMask(), "Mask mismatch");
        assertTrue(ace.isGroup(), "Should be Ace of a group");

        assertReturnedDescriptorEquals(aceInfo, ace);
    }

    public void createUsingPermissionAndSid() {
        Ace ace = new Ace(new Acl(), BasePermission.READ, new ArtifactorySid("momo"));

        // now test the getters
        assertEquals(ace.getPrincipal(), "momo", "Principal mismatch");
        assertEquals(ace.getMask(), BasePermission.READ.getMask(), "Mask mismatch");
        assertFalse(ace.isGroup(), "Create from sid is always not group");

        assertTrue(ace.getDescriptor().canRead(), "Created a reader");
        assertFalse(ace.getDescriptor().canDeploy(), "Created a reader");
        assertFalse(ace.getDescriptor().canAdmin(), "Created a reader");
    }

    public void constructorWithSid() {
        ArtifactorySid sid = new ArtifactorySid("momo");
        Ace ace = new Ace(new Acl(), BasePermission.READ, sid);
        assertEquals(ace.getSid(), sid, "Returned sid not equals to original");
    }

    public void matchSid() {
        ArtifactorySid sid = new ArtifactorySid("momo");
        ArtifactorySid groupSid = new ArtifactorySid("momo", true);
        Ace ace = new Ace(new Acl(), BasePermission.READ, sid);
        assertTrue(ace.matchSids(sid));
        assertFalse(ace.matchSids(groupSid),
                "Should not match to group sid");
        assertTrue(ace.matchSids(sid, groupSid));
    }

    private void assertReturnedDescriptorEquals(AceInfo aceInfo, Ace ace) {
        AceInfo descriptor = ace.getDescriptor();
        assertNotNull(descriptor);
        assertEquals(descriptor.getPrincipal(), aceInfo.getPrincipal());
        assertEquals(descriptor.getMask(), aceInfo.getMask());
        assertEquals(descriptor.isGroup(), aceInfo.isGroup());
    }
}
