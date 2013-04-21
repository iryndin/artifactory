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

import com.google.common.collect.Lists;
import org.artifactory.common.ArtifactoryHome;
import org.artifactory.common.ConstantValues;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.AntPathMatcher;

import javax.annotation.Nullable;
import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * An Ant-based path matcher util class.
 *
 * @author Yossi Shaul
 */
public abstract class PathMatcher {
    private static final Logger log = LoggerFactory.getLogger(PathMatcher.class);

    private static final AntPathMatcher antPathMatcher = new AntPathMatcher();

    /**
     * Default global exclude patterns. Will be used if not configured by the user.
     */
    private static final List<String> DEFAULT_GLOBAL_EXCLUDES = Lists.newArrayList(
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

    /**
     * The global excludes that applies to all repositories. Configurable by the user with {@link
     * org.artifactory.common.ConstantValues#globalExcludes}.
     */
    private static List<String> GLOBAL_EXCLUDES;

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

    public static boolean matches(File file, @Nullable Collection<String> includes,
            @Nullable Collection<String> excludes) {
        return matches(cleanPath(file), includes, excludes, true);
    }

    public static boolean matches(String path, @Nullable Collection<String> includes,
            @Nullable Collection<String> excludes) {
        return matches(path, includes, excludes, true);
    }

    public static boolean matches(String path, @Nullable Collection<String> includes,
            @Nullable Collection<String> excludes, boolean matchStart) {
        if (CollectionUtils.notNullOrEmpty(excludes)) {
            for (String exclude : excludes) {
                boolean match = antPathMatcher.match(exclude, path);
                if (match) {
                    log.debug("excludes pattern ({}) rejected path '{}'.", exclude, path);
                    return false;
                }
            }
        }

        if (CollectionUtils.notNullOrEmpty(includes)) {
            for (String include : includes) {
                // If path is smaller than include or end with / verify if it's a sub path then return true
                if (matchStart
                        && (path.endsWith("/") || path.length() <= include.length())
                        && antPathMatcher.matchStart(include, path)) {
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

    private static List<String> initOrGetGlobalExcludes() {
        if (GLOBAL_EXCLUDES == null) {
            try {
                String defaultExcludes = ArtifactoryHome.get().getArtifactoryProperties().getProperty(
                        ConstantValues.globalExcludes);
                if (defaultExcludes == null) {
                    GLOBAL_EXCLUDES = DEFAULT_GLOBAL_EXCLUDES;
                } else {
                    GLOBAL_EXCLUDES = Arrays.asList(defaultExcludes.split(","));
                }
            } catch (Exception e) {
                log.error("Failed to parse global default excludes. Using default values: " + e.getMessage());
                GLOBAL_EXCLUDES = DEFAULT_GLOBAL_EXCLUDES;
            }
        }
        return GLOBAL_EXCLUDES;
    }

    public static boolean isInGlobalExcludes(File file) {
        // global excludes are used as includes to test if path is in the default excludes
        return matches(cleanPath(file), initOrGetGlobalExcludes(), null, false);
    }

    public static List<String> getGlobalExcludes() {
        return Lists.newArrayList(initOrGetGlobalExcludes());
    }
}
