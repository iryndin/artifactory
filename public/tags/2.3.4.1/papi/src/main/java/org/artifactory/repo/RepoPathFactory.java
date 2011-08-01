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

package org.artifactory.repo;

import java.lang.reflect.Constructor;

/**
 * A factory for creating RepoPath objects.
 * <p/>
 * Has runtime dependency on the core.
 *
 * @author Yoav Landman
 */
public final class RepoPathFactory {

    private static final Constructor<?> ctor;

    static {
        ctor = getCtor();
    }

    /**
     * @param repoKey The key of any repo
     * @param path    The relative path inside the repo
     */
    public static RepoPath create(String repoKey, String path) {
        try {
            return (RepoPath) ctor.newInstance(repoKey, path);
        } catch (Exception e) {
            throw new RuntimeException("Could not create a RepoPath object.", e);
        }
    }

    private static Constructor<?> getCtor() {
        try {
            Class<?> cls =
                    Thread.currentThread().getContextClassLoader().loadClass("org.artifactory.api.repo.RepoPathImpl");
            Constructor<?> ctor = cls.getConstructor(String.class, String.class);
            return ctor;
        } catch (Exception e) {
            throw new RuntimeException("Could not create a RepoPath object.", e);
        }
    }
}