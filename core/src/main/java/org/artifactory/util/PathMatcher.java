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

import org.artifactory.log.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.util.AntPathMatcher;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * An Ant based path matcher util class.
 *
 * @author Yossi Shaul
 */
public class PathMatcher {
    private static final Logger log = LoggerFactory.getLogger(PathMatcher.class);

    private static AntPathMatcher antPathMatcher = new AntPathMatcher();
    private static final List<String> DEFAULT_EXCLUDES = new ArrayList<String>(14) {
        {
            add("**/*~");
            add("**/#*#");
            add("**/.#*");
            add("**/%*%");
            add("**/._*");
            add("**/CVS");
            add("**/CVS/**");
            add("**/.cvsignore");
            add("**/SCCS");
            add("**/SCCS/**");
            add("**/vssver.scc");
            add("**/.svn");
            add("**/.svn/**");
            add("**/.DS_Store");
        }
    };

    public static String cleanPath(File file) {
        String path = file.getAbsolutePath();
        path = path.replace('\\', '/');
        if (path.startsWith("/") && path.length() > 1) {
            return path.substring(1);
        }
        return path;
    }

    public static boolean matches(File file, List<String> includes, List<String> excludes) {
        return matches(cleanPath(file), includes, excludes);
    }

    public static boolean matches(String path, List<String> includes, List<String> excludes) {
        if (excludes != null && !excludes.isEmpty()) {
            for (String exclude : excludes) {
                boolean match = antPathMatcher.match(exclude, path);
                if (match) {
                    log.debug("excludes pattern ({}) rejected path '{}'.", exclude, path);
                    return false;
                }
            }
        }
        //Always match the repository itself
        if ("".equals(path) || "/".equals(path)) {
            return true;
        }
        if (includes != null && !includes.isEmpty()) {
            for (String include : includes) {
                boolean match = antPathMatcher.match(include, path);
                if (match) {
                    return true;
                }
            }
        } else {
            return true;
        }
        return false;
    }

    public static boolean isInDefaultExcludes(File file) {
        return matches(file, DEFAULT_EXCLUDES, null);
    }

    public static List<String> getDefaultExcludes() {
        return new ArrayList<String>(DEFAULT_EXCLUDES);
    }
}
