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

import org.artifactory.factory.InfoFactoryHolder;
import org.springframework.security.acls.domain.BasePermission;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

/**
 * Ace unit tests.
 *
 * @author Yossi Shaul
 */
@Test
public class AceTest {

    public void createForUserFromAceInfo() {
        MutableAceInfo aceInfo = InfoFactoryHolder.get()
                .createAce("momo", false, ArtifactoryPermission.ADMIN.getMask());
        Ace ace = new Ace(new Acl(), aceInfo);

        // now test the getters
        assertEquals(ace.getPrincipal(), aceInfo.getPrincipal(), "Principal mismatch");
        assertEquals(ace.getMask(), aceInfo.getMask(), "Mask mismatch");
        assertFalse(ace.isGroup(), "Not a group");

        // test descriptor doesn't loose data
        assertReturnedDescriptorEquals(aceInfo, ace);
    }

    public void createForGroupFromAceInfo() {
        MutableAceInfo aceInfo = InfoFactoryHolder.get()
                .createAce("momo", true, ArtifactoryPermission.DEPLOY.getMask());
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
