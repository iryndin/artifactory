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

package org.artifactory.util;

import com.google.common.collect.Lists;
import org.artifactory.log.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.util.AntPathMatcher;

import java.io.File;
import java.util.List;

/**
 * An Ant based path matcher util class.
 *
 * @author Yossi Shaul
 */
public abstract class PathMatcher {
    private static final Logger log = LoggerFactory.getLogger(PathMatcher.class);

    private static final AntPathMatcher antPathMatcher = new AntPathMatcher();
    private static final List<String> DEFAULT_EXCLUDES = Lists.newArrayList(
            "**/*~",
            "**/#*#",
            "**/.#*",
            "**/%*%",
            "**/._*",
            "**/CVS",
            "**/CVS/**",
            "**/.cvsignore",
            "**/SCCS",
            "**/SCCS/**",
            "**/vssver.scc",
            "**/.svn",
            "**/.svn/**",
            "**/.DS_Store");

    private PathMatcher() {
        // utility class
    }

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
        if (!CollectionUtils.isNullOrEmpty(excludes)) {
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
        if (!CollectionUtils.isNullOrEmpty(includes)) {
            for (String include : includes) {
                // If path is smaller than include verify if it's a sub path then return true
                if (path.length() < include.length() &&
                        include.startsWith(path) &&
                        include.charAt(path.length()) == '/') {
                    return true;
                }
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
        return Lists.newArrayList(DEFAULT_EXCLUDES);
    }
}
