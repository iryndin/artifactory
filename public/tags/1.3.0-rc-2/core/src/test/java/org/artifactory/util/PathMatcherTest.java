package org.artifactory.util;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;
import org.testng.annotations.Test;

import java.io.File;
import java.util.Arrays;
import java.util.List;

/**
 * PathMatcher unit tests.
 *
 * @author Yossi Shaul
 */
@Test
public class PathMatcherTest {

    public void matchWithIncludeAll() {
        boolean matches = PathMatcher.matches("apath", Arrays.asList("**"), Arrays.asList(""));
        assertTrue(matches);
    }

    public void matchWithExcludeAll() {
        boolean matches = PathMatcher.matches("apath", Arrays.asList(""), Arrays.asList("**"));
        assertFalse(matches);
    }

    public void matchWithIncludesOnly() {
        List<String> includes = Arrays.asList("apath", "**/my/test/path", "public/**");
        List<String> excludes = Arrays.asList("");
        assertTrue(PathMatcher.matches("apath", includes, excludes));
        assertFalse(PathMatcher.matches("apath2", includes, excludes));
        assertTrue(PathMatcher.matches("this/is/my/test/path", includes, excludes));
        assertFalse(PathMatcher.matches("this/is/my/test/path/andmore", includes, excludes));
        assertFalse(PathMatcher.matches("this/is/my/test/path/andmore", includes, excludes));
        assertTrue(PathMatcher.matches("public/in/the/public.jar", includes, excludes));
        assertTrue(PathMatcher.matches("public", includes, excludes));
    }

    public void matchWithExcludesOnly() {
        // only excludes and no includes should exclude everything
        List<String> includes = Arrays.asList("");
        List<String> excludes = Arrays.asList("apath", "**/my/test/path");
        assertFalse(PathMatcher.matches("apath", includes, excludes));
        assertFalse(PathMatcher.matches("apath2", includes, excludes));
        assertFalse(PathMatcher.matches("this/is/my/test/path", includes, excludes));
        assertFalse(PathMatcher.matches("this/is/my/test/path/andmore", includes, excludes));
    }

    public void excludesTakesPrecedenceOverIncludes() {
        // currently excludes alway takes precedence, we might want to change it in the future so the closer will win
        List<String> excludes = Arrays.asList("apath/**");
        List<String> includes = Arrays.asList("**", "apath/sub/1");
        assertFalse(PathMatcher.matches("apath", includes, excludes));
        assertFalse(PathMatcher.matches("apath/sub", includes, excludes));
        assertFalse(PathMatcher.matches("apath/sub/1", includes, excludes));  // even though it is explicitly included
        assertTrue(PathMatcher.matches("apath2", includes, excludes));
    }

    public void defaultExcludes() {
        File file = new File("/work/tmp/t2/20080810.145407/repositories/repo1-cache/.DS_Store");
        assertTrue(PathMatcher.matches(file, PathMatcher.getDefaultExcludes(), null));
        assertTrue(PathMatcher.isInDefaultExcludes(file));
    }
}
