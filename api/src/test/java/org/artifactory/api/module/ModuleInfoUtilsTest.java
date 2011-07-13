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

package org.artifactory.api.module;

import org.artifactory.api.common.MultiStatusHolder;
import org.artifactory.util.RepoLayoutUtils;
import org.testng.annotations.Test;

import static org.artifactory.api.module.ModuleInfoUtils.*;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * @author Noam Y. Tenne
 */
@Test
public class ModuleInfoUtilsTest {

    @Test(expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = "Unable to construct a path from a null module info object.")
    public void constructArtifactPathWithNullModuleInfo() {
        constructArtifactPath(null, null);
    }

    @Test(expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = "Unable to construct a path from an invalid module info object.")
    public void constructArtifactPathWithInvalidModuleInfo() {
        constructArtifactPath(new ModuleInfo(), null);
    }

    @Test(expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = "Unable to construct a path from a null repository layout.")
    public void constructArtifactPathWithNullRepoLayout() {
        constructArtifactPath(new ModuleInfoBuilder().organization("org").module("mod").baseRevision("rev").build(),
                null);
    }

    @Test(expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = "Cannot construct a module info object from a blank item path.")
    public void moduleInfoFromArtifactPathWithNullArtifactPath() {
        moduleInfoFromArtifactPath(null, null);
    }

    @Test(expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = "Cannot construct a module info object from a blank item path.")
    public void moduleInfoFromDescriptorPathWithNullArtifactPath() {
        moduleInfoFromDescriptorPath(null, null);
    }

    @Test(expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = "Cannot construct a module info object from a blank item path.")
    public void moduleInfoFromArtifactPathWithBlankItemPath() {
        moduleInfoFromArtifactPath("", null);
    }

    @Test(expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = "Cannot construct a module info object from a blank item path.")
    public void moduleInfoFromDescriptorPathWithBlankItemPath() {
        moduleInfoFromDescriptorPath("", null);
    }

    @Test(expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = "Cannot construct a module info object from a null repository layout.")
    public void moduleInfoFromArtifactPathWithNullRepoLayout() {
        moduleInfoFromArtifactPath("org/meow", null);
    }

    @Test(expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = "Cannot construct a module info object from a null repository layout.")
    public void moduleInfoFromDescriptorPathWithNullRepoLayout() {
        moduleInfoFromDescriptorPath("org/meow", null);
    }

    public void translateArtifactPathWithUnCrossablePatterns() {
        //Test with no status holder
        assertEquals(translateArtifactPath(RepoLayoutUtils.MAVEN_2_DEFAULT, null, "[org]/momo/popo/[type]s/jojo.[ext]"),
                "[org]/momo/popo/[type]s/jojo.[ext]", "Uncrossable layouts should return the exact same path.");

        //Test with a status holder
        MultiStatusHolder status = new MultiStatusHolder();
        String translatedPath = translateArtifactPath(RepoLayoutUtils.MAVEN_2_DEFAULT, null,
                "[org]/momo/popo/[type]s/jojo.[ext]", status);
        assertEquals(translatedPath, "[org]/momo/popo/[type]s/jojo.[ext]",
                "Uncrossable layouts should return the exact same path.");
        assertTrue(status.hasWarnings(), "Uncrossable conversion should write an error to the status holder.");

        String firstMessage = status.getWarnings().get(0).getMessage();
        assertTrue(firstMessage.contains("Unable to translate"), "Unexpected uncrossable layouts warning message.");
        assertTrue(firstMessage.contains("[org]/momo/popo/[type]s/jojo.[ext]"),
                "Unexpected uncrossable layouts warning message.");
    }
}
