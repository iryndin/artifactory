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
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.List;

import static org.testng.Assert.assertEquals;

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
                "permissionName", Arrays.asList("aRepo", "repo2"), includes, excludes);

        assertEquals(permissionTarget.getName(), "permissionName");
        assertEquals(permissionTarget.getRepoKeys().size(), 2);
        assertEquals(permissionTarget.getRepoKeys().get(0), "aRepo");
        assertEquals(permissionTarget.getRepoKeys().get(1), "repo2");
        assertEquals(permissionTarget.getIncludesPattern(), includes);
        assertEquals(permissionTarget.getIncludes().size(), 2);
        assertEquals(permissionTarget.getExcludesPattern(), excludes);
        assertEquals(permissionTarget.getExcludes().size(), 1);
    }

    private MutablePermissionTargetInfo createPermissionTarget(String permName, List<String> repoKeys) {
        MutablePermissionTargetInfo permissionTarget = InfoFactoryHolder.get().createPermissionTarget();
        permissionTarget.setName(permName);
        permissionTarget.setRepoKeys(repoKeys);
        return permissionTarget;
    }

    public void createFromDescriptor() {
        MutablePermissionTargetInfo pmi = createPermissionTarget("permissionName", Arrays.asList("aRepo"));
        pmi.setIncludesPattern(includes);
        pmi.setExcludesPattern(excludes);

        PermissionTarget permissionTarget = new PermissionTarget(pmi);

        assertEquals(permissionTarget.getName(), "permissionName");
        assertEquals(permissionTarget.getRepoKeys(), Arrays.asList("aRepo"));
        assertEquals(permissionTarget.getIncludesPattern(), includes);
        assertEquals(permissionTarget.getIncludes().size(), 2);
        assertEquals(permissionTarget.getExcludesPattern(), excludes);
        assertEquals(permissionTarget.getExcludes().size(), 1);

        assertDescriptorsEquals(pmi, permissionTarget.getDescriptor());
    }

    private static void assertDescriptorsEquals(
            PermissionTargetInfo orig, PermissionTargetInfo copy) {
        assertEquals(copy.getName(), orig.getName());
        assertEquals(copy.getRepoKeys(), copy.getRepoKeys());
        assertEquals(copy.getIncludesPattern(), copy.getIncludesPattern());
        assertEquals(copy.getExcludesPattern(), copy.getExcludesPattern());
    }

}
