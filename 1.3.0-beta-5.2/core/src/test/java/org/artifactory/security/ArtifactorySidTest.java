package org.artifactory.security;

import static org.testng.Assert.*;
import org.testng.annotations.Test;

/**
 * ArtifactorySid unit tests.
 *
 * @author Yossi Shaul
 */
@Test
public class ArtifactorySidTest {

    public void userConstructor() {
        ArtifactorySid sid = new ArtifactorySid("momo");
        assertEquals(sid.getPrincipal(), "momo");
        assertFalse(sid.isGroup(), "Default group value should be false");
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void nullPrincipal() {
        new ArtifactorySid(null);
    }

    public void userSidCreation() {
        ArtifactorySid sid = new ArtifactorySid("momo", false);
        assertEquals(sid.getPrincipal(), "momo");
        assertFalse(sid.isGroup());
    }

    public void groupSidCreation() {
        ArtifactorySid sid = new ArtifactorySid("agroup", true);
        assertEquals(sid.getPrincipal(), "agroup");
        assertTrue(sid.isGroup());
    }

    public void userSidsEquals() {
        ArtifactorySid sid1 = new ArtifactorySid("user1");
        // equal entry
        ArtifactorySid sid2 = new ArtifactorySid("user1", false);
        // different user
        ArtifactorySid sid3 = new ArtifactorySid("user2");
        // subclass with same value
        ArtifactorySid sid4 = new ArtifactorySid("user1") {
        };
        // group with same name
        ArtifactorySid groupSid = new ArtifactorySid("user1", true);

        assertTrue(sid1.equals(sid2));
        assertFalse(sid1.equals(sid3), "Different user sid");
        assertFalse(sid1.equals(sid4), "Subclass should not equal");
    }

    public void groupSidsEquals() {
        ArtifactorySid sid1 = new ArtifactorySid("group1", true);
        // equal entry
        ArtifactorySid sid2 = new ArtifactorySid("group1", true);
        // different user
        ArtifactorySid sid3 = new ArtifactorySid("group2");
        // subclass with same value
        ArtifactorySid sid4 = new ArtifactorySid("group1") {
        };

        assertTrue(sid1.equals(sid2));
        assertFalse(sid1.equals(sid3), "Different group sid");
        assertFalse(sid1.equals(sid4), "Subclass should not equal");
    }

    public void groupAndUserNotEquals() {
        ArtifactorySid userSid = new ArtifactorySid("principal");
        ArtifactorySid groupSid = new ArtifactorySid("principal", true);

        assertFalse(userSid.equals(groupSid), "User and group SIDs cannot be equal");
    }
}
