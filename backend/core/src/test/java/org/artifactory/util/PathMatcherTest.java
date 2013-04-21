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

package org.artifactory.util;

import org.artifactory.test.ArtifactoryHomeBoundTest;
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
public class PathMatcherTest extends ArtifactoryHomeBoundTest {

    public void matchWithIncludeAll() {
        boolean matches = PathMatcher.matches("apath", Arrays.asList("**"), Arrays.asList(""));
        assertTrue(matches);
    }

    public void matchWithExcludeAll() {
        boolean matches = PathMatcher.matches("apath", Arrays.asList(""), Arrays.asList("**"));
        assertFalse(matches);
    }

    public void matchWithIncludesOnly() {
        List<String> includes = Arrays.asList("apath/*", "**/my/test/path", "public/in/**");
        List<String> excludes = Arrays.asList("");
        assertTrue(PathMatcher.matches("apath", includes, excludes));
        assertTrue(PathMatcher.matches("this/is/my/test/path", includes, excludes));
        assertFalse(PathMatcher.matches("this/is/my/test/path/andmore", includes, excludes));
        assertFalse(PathMatcher.matches("this/is/my/test/path/andmore", includes, excludes));
        assertTrue(PathMatcher.matches("public/in/the/public.jar", includes, excludes));
        assertTrue(PathMatcher.matches("public", includes, excludes));
        assertTrue(PathMatcher.matches("public/i", includes, excludes));
        assertFalse(PathMatcher.matches("public2", Arrays.asList("apath/*", "public/in/**"), excludes));
    }

    public void matchWithIncludesOnlyPartial() {
        List<String> excludes = Arrays.asList("");
        assertTrue(PathMatcher.matches("ap/x", Arrays.asList("*p/x/y"), excludes));
        assertTrue(PathMatcher.matches("ap/x", Arrays.asList("**/y/*"), excludes));
        assertTrue(PathMatcher.matches("ap/x", Arrays.asList("*/x/*"), excludes));
        assertTrue(PathMatcher.matches("ap/x", Arrays.asList("*/*/*"), excludes));
        assertTrue(PathMatcher.matches("ap/x", Arrays.asList("*/*/t"), excludes));
        assertTrue(PathMatcher.matches("ap/x", Arrays.asList("*/x/t"), excludes));
        assertTrue(PathMatcher.matches("ap/x", Arrays.asList("a?/x"), excludes));
        assertTrue(PathMatcher.matches("ap/x", Arrays.asList("?p*/?"), excludes));
        assertFalse(PathMatcher.matches("ap/x", Arrays.asList("?p*/??"), excludes));
        assertFalse(PathMatcher.matches("ap/x", Arrays.asList("aa?/x"), excludes));
        assertFalse(PathMatcher.matches("ap/x", Arrays.asList("*/d/*"), excludes));
        assertTrue(PathMatcher.matches("apath", Arrays.asList("apath/some/other/*"), excludes));
        assertTrue(PathMatcher.matches("apath/some", Arrays.asList("apath/some/other/*"), excludes));
        assertFalse(PathMatcher.matches("apath/some2", Arrays.asList("apath/some/other/*"), excludes));
        assertFalse(PathMatcher.matches("apath2", Arrays.asList("apath/some/other/*"), excludes));
        assertTrue(
                PathMatcher.matches("com", Arrays.asList("com/some/other/*", "com/acme/***", "com/toto/*"), excludes));
        assertFalse(
                PathMatcher.matches("org", Arrays.asList("com/some/other/*", "com/acme/***", "com/toto/*"), excludes));
        assertFalse(PathMatcher.matches("apath2", Arrays.asList("apath/*"), excludes));
        assertFalse(PathMatcher.matches("apath", Arrays.asList("apath2/*"), excludes));
        assertFalse(PathMatcher.matches("apath", Arrays.asList("apath2"), excludes));
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

    public void globalExcludes() {
        File file = new File("/work/tmp/t2/20080810.145407/repositories/repo1-cache/.DS_Store");
        assertTrue(PathMatcher.isInGlobalExcludes(file));
        assertFalse(PathMatcher.matches(file, null, PathMatcher.getGlobalExcludes()));
        file = new File("/repo1-cache/test~");
        assertTrue(PathMatcher.isInGlobalExcludes(file));
        assertFalse(PathMatcher.matches(file, null, PathMatcher.getGlobalExcludes()));
        // RTFACT-5394 -  Make sure global excludes doesn't check match start
        File shortFileName = new File("/t");
        assertFalse(PathMatcher.isInGlobalExcludes(shortFileName));
        // Make sure that global excludes, exclude CVS directories.
        File cvsFile = new File("/toto/CVS/should/be/excluded");
        assertTrue(PathMatcher.isInGlobalExcludes(cvsFile));
    }

    public void subPathIncludes() {
        List<String> includes = Arrays.asList("apath1/sub1/**", "apath1/sub2/**", "apath2/sub1/**");
        assertFalse(PathMatcher.matches("apath", includes, null));
        assertTrue(PathMatcher.matches("apath1", includes, null));
        assertTrue(PathMatcher.matches("apath2", includes, null));
        assertFalse(PathMatcher.matches("apath1/sub", includes, null));
        assertTrue(PathMatcher.matches("apath1/sub1", includes, null));
        assertTrue(PathMatcher.matches("apath1/sub1/t", includes, null));
        assertTrue(PathMatcher.matches("apath1/sub1/toto/tutu", includes, null));
    }

    public void sourcesIncludes() {
        List<String> includes = Arrays.asList("**/*-sources.jar*");
        assertTrue(PathMatcher.matches("apath/", includes, null));
        assertTrue(PathMatcher.matches("apath1/", includes, null));
        assertTrue(PathMatcher.matches("apath2/", includes, null));
        assertTrue(PathMatcher.matches("apath1/subfolder-longer-than-pattern/", includes, null));
        assertTrue(PathMatcher.matches("apath1/sub1/", includes, null));
        assertTrue(PathMatcher.matches("apath1/sub1/t/", includes, null));
        assertTrue(PathMatcher.matches("apath1/sub1/toto/tutu/", includes, null));
        assertTrue(PathMatcher.matches("apath1/toto-sources.jar", includes, null));
        assertTrue(PathMatcher.matches("apath1/sub/toto-sources.jar", includes, null));
        assertTrue(PathMatcher.matches("apath1/toto-sources.jar.md5", includes, null));
        assertTrue(PathMatcher.matches("apath1/sub/toto-sources.jar.md5", includes, null));
        assertFalse(PathMatcher.matches("apath1/toto-1.0.jar", includes, null));
        assertFalse(PathMatcher.matches("apath1/sub/toto-1.0.jar", includes, null));
        assertFalse(PathMatcher.matches("apath1/toto-1.0.jar.md5", includes, null));
        assertFalse(PathMatcher.matches("apath1/sub/toto-1.0.jar.md5", includes, null));
    }
}
