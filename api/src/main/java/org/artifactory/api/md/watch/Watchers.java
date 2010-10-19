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

package org.artifactory.api.md.watch;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import org.artifactory.common.Info;
import org.artifactory.repo.RepoPath;

import java.util.Set;
import java.util.TreeSet;

/**
 * Item watchers metadata object
 *
 * @author Noam Tenne
 */
@XStreamAlias(Watchers.ROOT)
public class Watchers implements Info {

    public static final String ROOT = "watchers";

    private RepoPath repoPath;
    /**
     * Watchers list
     */
    private final Set<Watcher> watchers;

    /**
     * Default constructor for xstream
     */
    public Watchers() {
        watchers = new TreeSet<Watcher>();
    }


    /**
     * Manual init constructor
     *
     * @param repoPath Path that watchers belongs to
     */
    public Watchers(RepoPath repoPath) {
        watchers = new TreeSet<Watcher>();
        this.repoPath = repoPath;
    }

    /**
     * Copy constructor
     *
     * @param watchersObject Object to copy
     */
    public Watchers(Watchers watchersObject) {
        watchers = watchersObject.getWatchers();
        repoPath = watchersObject.getRepoPath();
    }

    /**
     * Returns the set of watchers
     *
     * @return A set object of watchers
     */
    public Set<Watcher> getWatchers() {
        return watchers;
    }

    /**
     * Returns the repo path that the watchers belong to
     *
     * @return RepoPath
     */
    public RepoPath getRepoPath() {
        return repoPath;
    }

    /**
     * Sets the repo path for the current watchers
     *
     * @param repoPath Repo path
     */
    public void setRepoPath(RepoPath repoPath) {
        this.repoPath = repoPath;
    }

    /**
     * Returns the given user's watcher object (if watching)
     *
     * @param username Username of watcher to search
     * @return Watcher object if give user is a watcher. Null if not
     */
    public Watcher getWatcher(String username) {
        for (Watcher watcher : watchers) {
            if (watcher.getUsername().equals(username)) {
                return watcher;
            }
        }
        return null;
    }

    /**
     * Check is the given user is listed as a watcher for this object
     *
     * @param username Name of user to check
     * @return True if the given user is watching. False if not
     */
    public boolean isUserWatching(String username) {
        for (Watcher watcher : watchers) {
            if (watcher.getUsername().equals(username)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Adds the given watcher
     *
     * @param watcher Watcher object to add
     */
    public void addWatcher(Watcher watcher) {
        if (!isUserWatching(watcher.getUsername())) {
            watchers.add(watcher);
        }
    }

    /**
     * Removes the given username from the watcher list
     *
     * @param username Name of user to remove
     * @return True if the user has been moved. False if not
     */
    public void removeWatcher(String username) {
        Set<Watcher> watchersCopy = new TreeSet<Watcher>(watchers);
        for (Watcher watcher : watchersCopy) {
            if (watcher.getUsername().equals(username)) {
                watchers.remove(watcher);
            }
        }
    }

    /**
     * Indicates if the watcher set is empty
     *
     * @return True if the set is empty, False if not
     */
    public boolean isEmpty() {
        return watchers.isEmpty();
    }

    /**
     * Indicates if the given repo path is equal to the contained one
     *
     * @param repoPath To compare
     * @return True if both paths are equal. False if not
     */
    public boolean isRepoPathEqual(RepoPath repoPath) {
        return this.repoPath.equals(repoPath);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Watchers)) {
            return false;
        }

        Watchers watchers1 = (Watchers) o;

        if (watchers != null ? !watchers.equals(watchers1.watchers) : watchers1.watchers != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return watchers != null ? watchers.hashCode() : 0;
    }
}
