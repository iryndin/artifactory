/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2011 JFrog Ltd.
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

package org.artifactory.api.security;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

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

    public void copyConstructor() {
        AceInfo orig = new AceInfo("koko", true, ArtifactoryPermission.ADMIN.getMask());
        AceInfo copy = new AceInfo(orig);

        assertEquals(orig.getPrincipal(), copy.getPrincipal());
        assertEquals(orig.getMask(), copy.getMask());
        assertEquals(orig.isGroup(), copy.isGroup());
    }

    public void copyConstructorReflectionEquality() {
        AceInfo orig = new AceInfo("koko", true, ArtifactoryPermission.ADMIN.getMask());
        AceInfo copy = new AceInfo(orig);

        assertTrue(EqualsBuilder.reflectionEquals(orig, copy), "Orig and copy differ");
    }
}
