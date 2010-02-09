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

import org.testng.annotations.Test;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

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
        List<String> includes = Collections.emptyList();
        List<String> excludes = Arrays.asList("apath", "**/my/test/path", "commons-*", "main/*");
        assertTrue(PathMatcher.matches("apat", includes, excludes));
        assertFalse(PathMatcher.matches("apath", includes, excludes));
        assertTrue(PathMatcher.matches("apath2", includes, excludes));
        assertFalse(PathMatcher.matches("commons-codec", includes, excludes));
        assertTrue(PathMatcher.matches("main.123", includes, excludes));
        assertTrue(PathMatcher.matches("apath/deep", includes, excludes));
        assertFalse(PathMatcher.matches("this/is/my/test/path", includes, excludes));
        assertTrue(PathMatcher.matches("this/is/my/test/path/andmore", includes, excludes));
    }

    public void matchExcludesAndIncludes() {
        List<String> includes = Arrays.asList("org/**", "com/**", "net/**");
        List<String> excludes = Arrays.asList("org/apache/**", "commons-*");
        assertTrue(PathMatcher.matches("org/codesulting", includes, excludes));
        assertTrue(PathMatcher.matches("com/test/123", includes, excludes));
        assertFalse(PathMatcher.matches("org/apache/bla", includes, excludes));
        assertFalse(PathMatcher.matches("commons-lang", includes, excludes));
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
