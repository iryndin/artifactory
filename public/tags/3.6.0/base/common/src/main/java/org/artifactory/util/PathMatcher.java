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

import org.artifactory.repo.RepoPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.AntPathMatcher;

import javax.annotation.Nullable;
import java.util.Collection;

/**
 * An Ant-based path matcher util class.
 *
 * @author Yossi Shaul
 */
public abstract class PathMatcher {
    private static final Logger log = LoggerFactory.getLogger(PathMatcher.class);

    private static final AntPathMatcher antPathMatcher = new AntPathMatcher();

    private PathMatcher() {
        // utility class
    }

    public static boolean matches(RepoPath repoPath, @Nullable Collection<String> includes,
            @Nullable Collection<String> excludes) {
        // If repoPath represents folder then use "startMatch"
        boolean useStartMatch = repoPath.isFolder();
        return matches(repoPath.getPath(), includes, excludes, useStartMatch);
    }

    public static boolean matches(String path, @Nullable Collection<String> includes,
            @Nullable Collection<String> excludes, boolean useStartMatch) {
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
                if (useStartMatch && antPathMatcher.matchStart(include, path)) {
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


}
