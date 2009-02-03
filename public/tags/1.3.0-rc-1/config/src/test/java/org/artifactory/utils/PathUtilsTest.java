package org.artifactory.utils;

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

}
