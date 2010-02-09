package org.artifactory.util;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Tests the PathUtils.
 *
 * @author Yossi Shaul
 */
@Test
public class PathUtilsTest {

    public void formatSimplePath() {
        String formatted = PathUtils.formatPath("\\this\\is\\a/path");
        Assert.assertEquals(formatted, "/this/is/a/path");
    }

    public void formatFormattedPath() {
        String formatted = PathUtils.formatPath("/this/is/a/path");
        Assert.assertEquals(formatted, "/this/is/a/path");
    }

    public void formatNullPath() {
        String formatted = PathUtils.formatPath(null);
        Assert.assertEquals(formatted, "");
    }

    public void stripSimpleExtension() {
        String result = PathUtils.stripExtension("file.ext");
        Assert.assertEquals(result, "file");
    }

    public void stripMultipleExtension() {
        String result = PathUtils.stripExtension("file.ext.ext2");
        Assert.assertEquals(result, "file.ext");
    }

    public void stripPathWithNoExtension() {
        String result = PathUtils.stripExtension("file");
        Assert.assertEquals(result, "file");
    }

    public void stripPathWithDotAtEnd() {
        String result = PathUtils.stripExtension("file.");
        Assert.assertEquals(result, "file");
    }

    public void stripNullPath() {
        String result = PathUtils.stripExtension(null);
        Assert.assertNull(result);
    }

    public void stripEmptyPath() {
        String result = PathUtils.stripExtension("");
        Assert.assertEquals(result, "");
    }

}
