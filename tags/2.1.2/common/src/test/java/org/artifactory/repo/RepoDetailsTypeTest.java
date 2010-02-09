package org.artifactory.repo;

import static org.testng.Assert.assertEquals;
import org.testng.annotations.Test;

/**
 * Tests the RepoDetailsType enum
 *
 * @author Noam Y. Tenne
 */
@Test
public class RepoDetailsTypeTest {

    /**
     * Tests the different type display names
     */
    public void testDisplayName() {
        assertEquals(RepoDetailsType.LOCAL.getTypeName(), "Local");
        assertEquals(RepoDetailsType.REMOTE.getTypeName(), "Remote");
        assertEquals(RepoDetailsType.VIRTUAL.getTypeName(), "Virtual");
    }
}