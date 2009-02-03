package org.artifactory.api.security;

import static org.testng.Assert.*;
import org.testng.annotations.Test;

/**
 * AceInfo unit tests.
 *
 * @author Yossi Shaul
 */
@Test
public class AceInfoTest {

    public void adminMaskOnly() {
        AceInfo aceInfo = new AceInfo();
        aceInfo.setAdmin(true);

        assertTrue(aceInfo.canAdmin(), "Should be admin");
        assertFalse(aceInfo.canRead(), "Shouldn't be a reader");
        assertFalse(aceInfo.canDeploy(), "Shouldn't be a deployer");
    }

    public void readerMaskOnly() {
        AceInfo aceInfo = new AceInfo();
        aceInfo.setRead(true);

        assertFalse(aceInfo.canAdmin(), "Shouldn't be an admin");
        assertTrue(aceInfo.canRead(), "Should be a reader");
        assertFalse(aceInfo.canDeploy(), "Shouldn't be a deployer");
    }

    public void deployerMaskOnly() {
        AceInfo aceInfo = new AceInfo();
        aceInfo.setDeploy(true);

        assertFalse(aceInfo.canAdmin(), "Shouldn't be an admin");
        assertFalse(aceInfo.canRead(), "Shouldn't be a reader");
        assertTrue(aceInfo.canDeploy(), "Shouldn be a deployer");
    }

    public void allMasks() {
        AceInfo aceInfo = new AceInfo();
        aceInfo.setAdmin(true);
        aceInfo.setDeploy(true);
        aceInfo.setRead(true);

        assertTrue(aceInfo.canAdmin(), "Shouldn have all roles");
        assertTrue(aceInfo.canRead(), "Shouldn have all roles");
        assertTrue(aceInfo.canDeploy(), "Shouldn have all roles");
    }

    public void testCopyConstructor() {
        AceInfo orig = new AceInfo("koko", true, ArtifactoryPermisssion.ADMIN.getMask());
        AceInfo copy = new AceInfo(orig);

        assertEquals(orig.getPrincipal(), copy.getPrincipal());
        assertEquals(orig.getMask(), copy.getMask());
        assertEquals(orig.isGroup(), copy.isGroup());
    }

}
