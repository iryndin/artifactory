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

package org.artifactory.util;

import org.testng.Assert;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

/**
 * Tests the PathUtils.
 *
 * @author Yossi Shaul
 */
@Test
public class PathUtilsTest {

    public void formatSimplePath() {
        String formatted = PathUtils.formatPath("\\this\\is\\a/path");
        assertEquals(formatted, "/this/is/a/path");
    }

    public void formatFormattedPath() {
        String formatted = PathUtils.formatPath("/this/is/a/path");
        assertEquals(formatted, "/this/is/a/path");
    }

    public void formatNullPath() {
        String formatted = PathUtils.formatPath(null);
        assertEquals(formatted, "");
    }

    public void stripSimpleExtension() {
        String result = PathUtils.stripExtension("file.ext");
        assertEquals(result, "file");
    }

    public void stripMultipleExtension() {
        String result = PathUtils.stripExtension("file.ext.ext2");
        assertEquals(result, "file.ext");
    }

    public void stripPathWithNoExtension() {
        String result = PathUtils.stripExtension("file");
        assertEquals(result, "file");
    }

    public void stripPathWithDotAtEnd() {
        String result = PathUtils.stripExtension("file.");
        assertEquals(result, "file");
    }

    public void stripNullPath() {
        String result = PathUtils.stripExtension(null);
        Assert.assertNull(result);
    }

    public void stripEmptyPath() {
        String result = PathUtils.stripExtension("");
        assertEquals(result, "");
    }

    public void injectStringInMiddle() {
        String result = PathUtils.injectString("Arttory", "ifac", 3);
        assertEquals(result, "Artifactory");
    }

    public void injectStringAtTheBeginning() {
        String result = PathUtils.injectString("rtifactory", "A", 0);
        assertEquals(result, "Artifactory");
    }

    public void injectStringAtTheEnd() {
        String result = PathUtils.injectString("Artifactor", "y", 10);
        assertEquals(result, "Artifactory");
    }

    public void injectStringEmpty() {
        String result = PathUtils.injectString("Artifactory", "", 15);
        assertEquals(result, "Artifactory");
    }

    public void injectStringNull() {
        String result = PathUtils.injectString("Artifactory", null, 9);
        assertEquals(result, "Artifactory");
    }

    public void parentPathOfPathWithParent() {
        String result = PathUtils.getParent("/a/b/c");
        assertEquals(result, "/a/b");
    }

    public void parentPathOfRoot() {
        String result = PathUtils.getParent("/");
        assertEquals(result, "");
    }

    public void parentPathOfEmptyString() {
        String result = PathUtils.getParent("");
        assertEquals(result, "");
    }

    public void getPathElementsAbsolutePath() {
        String[] result = PathUtils.getPathElements("/a/b/c");
        assertEquals(result, new String[]{"a", "b", "c"});
    }

    public void getPathElementsRelativePath() {
        String[] result = PathUtils.getPathElements("a/b/c");
        assertEquals(result, new String[]{"a", "b", "c"});
    }

    public void getPathElementsPathWithTrailingSlash() {
        String[] result = PathUtils.getPathElements("a/b/");
        assertEquals(result, new String[]{"a", "b"});
    }

    public void getPathElementsRootPath() {
        String[] result = PathUtils.getPathElements("/");
        assertEquals(result, new String[]{""});
    }

    public void getPathElementsEmptyPath() {
        String[] result = PathUtils.getPathElements("");
        assertEquals(result, new String[]{""});
    }

    public void getPathElementsNullPath() {
        String[] result = PathUtils.getPathElements(null);
        assertEquals(result, new String[0]);
    }

    public void getFirstPathElementsAbsolutePath() {
        String result = PathUtils.getFirstPathElements("/a/b/c");
        assertEquals(result, "a");
    }

    public void getFirstPathElementsRelativePath() {
        String result = PathUtils.getFirstPathElements("a/b/c");
        assertEquals(result, "a");
    }

    public void getFirstPathElementsRootPath() {
        String result = PathUtils.getFirstPathElements("/");
        assertEquals(result, "");
    }

    public void getFirstPathElementsEmptyPath() {
        String result = PathUtils.getFirstPathElements("");
        assertEquals(result, "");
    }

    public void getFirstPathElementsNullPath() {
        String result = PathUtils.getFirstPathElements(null);
        Assert.assertNull(result);
    }

    public void trimLeadingSlashes() {
        String result = PathUtils.trimLeadingSlashes("////a/b/c");
        assertEquals(result, "a/b/c");
    }

    public void trimTrailingSlashes() {
        String result = PathUtils.trimTrailingSlashes("a/b/c///");
        assertEquals(result, "a/b/c");
    }

    public void trimTrailingSlashesChars() {
        CharSequence sequence = PathUtils.trimTrailingSlashesChars("a/b/c///");
        assertNotNull(sequence);
        assertEquals(sequence.toString(), "a/b/c");
    }

    public void trimLeadingSlashesChars() {
        CharSequence sequence = PathUtils.trimLeadingSlashesChars("////a/b/c");
        assertNotNull(sequence);
        assertEquals(sequence.toString(), "a/b/c");
    }
}
