package org.artifactory.api.security;

import static org.testng.Assert.assertEquals;
import org.testng.annotations.Test;

/**
 * PermissionTargetInfo unit tests.
 *
 * @author Yossi Shaul
 */
@Test
public class PermissionTargetInfoTest {

    public void testDefaultConstructor() {
        PermissionTargetInfo pmi = new PermissionTargetInfo();

        assertEquals(pmi.getName(), "");
        assertEquals(pmi.getRepoKey(), PermissionTargetInfo.ANY_REPO);
        assertEquals(pmi.getIncludesPattern(), PermissionTargetInfo.ANY_PATH);
        assertEquals(pmi.getIncludes().size(), 1);
        assertEquals(pmi.getExcludesPattern(), "");
        assertEquals(pmi.getExcludes().size(), 0);
    }

    public void createWithNoIncluesExcludesPatterns() {
        PermissionTargetInfo pmi = new PermissionTargetInfo("permissionName", "aRepo");

        assertEquals(pmi.getName(), "permissionName");
        assertEquals(pmi.getRepoKey(), "aRepo");
        assertEquals(pmi.getIncludesPattern(), PermissionTargetInfo.ANY_PATH);
        assertEquals(pmi.getIncludes().size(), 1);
        assertEquals(pmi.getExcludesPattern(), "");
        assertEquals(pmi.getExcludes().size(), 0);
    }

    public void createWithIncluesAndExcludesPatterns() {
        String includes = "**/*-sources.*,**/*-SNAPSHOT/**";
        String excludes = "**/secretjars/**";
        PermissionTargetInfo pmi = new PermissionTargetInfo(
                "permissionName", "aRepo", includes, excludes);

        assertEquals(pmi.getName(), "permissionName");
        assertEquals(pmi.getRepoKey(), "aRepo");
        assertEquals(pmi.getIncludesPattern(), includes);
        assertEquals(pmi.getIncludes().size(), 2);
        assertEquals(pmi.getExcludesPattern(), excludes);
        assertEquals(pmi.getExcludes().size(), 1);
    }

    public void copyConstructorTest() {
        PermissionTargetInfo orig = new PermissionTargetInfo(
                "permissionName", "aRepo", "**/*-sources.*,**/*-SNAPSHOT/**",
                "**/secretjars/**");

        PermissionTargetInfo copy = new PermissionTargetInfo(orig);
        assertEquals(copy.getName(), orig.getName());
        assertEquals(copy.getRepoKey(), orig.getRepoKey());
        assertEquals(copy.getExcludes(), orig.getExcludes());
        assertEquals(copy.getExcludesPattern(), orig.getExcludesPattern());
        assertEquals(copy.getIncludes(), orig.getIncludes());
        assertEquals(copy.getIncludesPattern(), orig.getIncludesPattern());
    }

}
