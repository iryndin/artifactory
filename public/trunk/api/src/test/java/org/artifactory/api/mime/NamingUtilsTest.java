/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2010 JFrog Ltd.
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

package org.artifactory.api.mime;

import org.artifactory.test.SystemPropertiesBoundTest;
import org.testng.annotations.Test;

import static org.artifactory.api.mime.NamingUtils.*;
import static org.testng.Assert.*;

/**
 * @author yoavl
 */
@Test
public class NamingUtilsTest extends SystemPropertiesBoundTest {

    public void testIsMetadata() {
        assertTrue(isMetadata("path/1.0-SNAPSHOT/maven-metadata.xml"));
        assertTrue(isMetadata("path/1.0/resource:md"));
    }

    public void testIsMetadataChecksum() {
        assertFalse(isMetadataChecksum("path/1.0-SNAPSHOT/maven-metadata.xml"));
        assertFalse(isMetadataChecksum("1.0-SNAPSHOT/maven-metadata.xml.bla1"));
        assertTrue(isMetadataChecksum("1.0-SNAPSHOT/maven-metadata.xml.sha1"));
        assertTrue(isMetadataChecksum("maven-metadata.xml.md5"));
        assertFalse(isMetadataChecksum("path/1.0/resource:md"));
        assertTrue(isMetadataChecksum("path/1.0/resource:md.md5"));
        assertTrue(isMetadataChecksum("path/1.0/resource:md.sha1"));
    }

    public void testJavaSourceNameFromClassName() {
        assertEquals(javaSourceNameFromClassName("/a/b/c.class"), "/a/b/c.java");
        assertEquals(javaSourceNameFromClassName("c.bla"), "c.bla");
        assertEquals(javaSourceNameFromClassName("a$1.class"), "a.java");
        assertEquals(javaSourceNameFromClassName("a$b$c.class"), "a.java");
        assertEquals(javaSourceNameFromClassName("z/y/x/a$b$c.class"), "z/y/x/a.java");
    }

    public void testIsViewable() {
        assertFalse(isViewable("a.class"));
        assertFalse(isViewable("a/b/c.class"));
        assertFalse(isViewable("/c.class"));

        assertTrue(isViewable("a.java"));
        assertTrue(isViewable("a.b.c.groovy"));
        assertTrue(isViewable("a.b.c.gradle"));
        assertTrue(isViewable("ivy-4.3.xml"));
        assertTrue(isViewable("a.ivy"));
        assertTrue(isViewable("a.xml"));
        assertTrue(isViewable("a.css"));
        assertTrue(isViewable("a.html"));
    }
}
