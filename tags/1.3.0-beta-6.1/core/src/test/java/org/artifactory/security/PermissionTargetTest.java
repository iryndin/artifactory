package org.artifactory.security;

import org.artifactory.api.security.PermissionTargetInfo;
import static org.testng.Assert.assertEquals;
import org.testng.annotations.Test;

/**
 * PermissionTarget unis tests.
 *
 * @author Yossi Shaul
 */
@Test
public class PermissionTargetTest {
    private final String includes = "**/*-sources.*,**/*-SNAPSHOT/**";
    private final String excludes = "**/secretjars/**";

    public void createWithIncluesAndExcludesPatterns() {
        PermissionTarget permissionTarget = new PermissionTarget(
                "permissionName", "aRepo", includes, excludes);

        assertEquals(permissionTarget.getName(), "permissionName");
        assertEquals(permissionTarget.getRepoKey(), "aRepo");
        assertEquals(permissionTarget.getIncludesPattern(), includes);
        assertEquals(permissionTarget.getIncludes().size(), 2);
        assertEquals(permissionTarget.getExcludesPattern(), excludes);
        assertEquals(permissionTarget.getExcludes().size(), 1);
    }

    public void createFromDescriptor() {
        PermissionTargetInfo pmi = new PermissionTargetInfo(
                "permissionName", "aRepo", includes, excludes);

        PermissionTarget permissionTarget = new PermissionTarget(pmi);

        assertEquals(permissionTarget.getName(), "permissionName");
        assertEquals(permissionTarget.getRepoKey(), "aRepo");
        assertEquals(permissionTarget.getIncludesPattern(), includes);
        assertEquals(permissionTarget.getIncludes().size(), 2);
        assertEquals(permissionTarget.getExcludesPattern(), excludes);
        assertEquals(permissionTarget.getExcludes().size(), 1);

        assertDescriptorsEquals(pmi, permissionTarget.getDescriptor());

    }

    private static void assertDescriptorsEquals(
            PermissionTargetInfo orig, PermissionTargetInfo copy) {
        assertEquals(copy.getName(), orig.getName());
        assertEquals(copy.getRepoKey(), copy.getRepoKey());
        assertEquals(copy.getIncludesPattern(), copy.getIncludesPattern());
        assertEquals(copy.getExcludesPattern(), copy.getExcludesPattern());
    }

}
