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

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

/**
 * @author Noam Y. Tenne
 */
@Test
public class ModuleInfoBuilderTest {

    public void testNoParams() {
        ModuleInfo build = new ModuleInfoBuilder().build();
        assertNull(build.getOrganization(), "Default 'organization' value of module info builder should be null.");
        assertNull(build.getModule(), "Default 'module' value of module info builder should be null.");
        assertNull(build.getBaseRevision(), "Default 'base revision' value of module info builder should be null.");
        assertNull(build.getFolderIntegrationRevision(),
                "Default 'folder integration revision' value of module info builder should be null.");
        assertNull(build.getFileIntegrationRevision(),
                "Default 'file integration revision' value of module info builder should be null.");
        assertNull(build.getClassifier(), "Default 'classifier' value of module info builder should be null.");
        assertNull(build.getExt(), "Default 'extension' value of module info builder should be null.");
        assertNull(build.getType(), "Default 'type' value of module info builder should be null.");
    }

    public void testNullParams() {
        ModuleInfo build = new ModuleInfoBuilder().organization(null).module(null).baseRevision(null).
                folderIntegrationRevision(null).fileIntegrationRevision(null).classifier(null).ext(null).type(null).
                build();
        assertNull(build.getOrganization(), "Expected 'organization' value of module info builder to be null.");
        assertNull(build.getModule(), "Expected 'module' value of module info builder to be null.");
        assertNull(build.getBaseRevision(), "Expected 'base revision' value of module info builder to be null.");
        assertNull(build.getFolderIntegrationRevision(),
                "Expected 'folder integration revision' value of module info builder to be null.");
        assertNull(build.getFileIntegrationRevision(),
                "Expected 'file integration revision' value of module info builder to be null.");
        assertNull(build.getClassifier(), "Expected 'classifier' value of module info builder to be null.");
        assertNull(build.getExt(), "Expected 'ext' value of module info builder to be null.");
        assertNull(build.getType(), "Expected 'type' value of module info builder to be null.");
    }

    public void testValidParams() {
        ModuleInfo build = new ModuleInfoBuilder().organization("organization").module("module").
                baseRevision("revisionBase").folderIntegrationRevision("pathRevisionIntegration").
                fileIntegrationRevision("artifactRevisionIntegration").classifier("classifier").ext("ext").
                type("type").build();
        assertEquals(build.getOrganization(), "organization",
                "Unexpected 'organization' value of module info builder.");
        assertEquals(build.getModule(), "module", "Unexpected 'module' value of module info builder.");
        assertEquals(build.getBaseRevision(), "revisionBase",
                "Unexpected 'base revision' value of module info builder.");
        assertEquals(build.getFolderIntegrationRevision(), "pathRevisionIntegration",
                "Unexpected 'folder integration revision' value of module info builder.");
        assertEquals(build.getFileIntegrationRevision(), "artifactRevisionIntegration",
                "Unexpected 'file integration revision' value of module info builder.");
        assertEquals(build.getClassifier(), "classifier", "Unexpected 'classifier' value of module info builder.");
        assertEquals(build.getExt(), "ext", "Unexpected 'ext' value of module info builder.");
        assertEquals(build.getType(), "type", "Unexpected 'type' value of module info builder.");
    }
}