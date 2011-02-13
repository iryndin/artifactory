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

package org.artifactory.api.repo;

import org.artifactory.repo.RepoPath;

/**
 * Represents an item on a remote server.
 *
 * @author Tomer Cohen
 */
public class RemoteBrowsableItem extends BrowsableItem {

    /**
     * Creates a new remote item.
     *
     * @param name     Item display name
     * @param folder   True if the item represents a folder
     * @param repoPath Item repo path
     */
    public RemoteBrowsableItem(String name, boolean folder, RepoPath repoPath) {
        super(name, folder, 0L, 0L, 0L, repoPath);
        setRemote(true);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RemoteBrowsableItem)) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }

        RemoteBrowsableItem item = (RemoteBrowsableItem) o;

        if (name != null ? !name.equals(item.name) : item.name != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (name != null ? name.hashCode() : 0);
        return result;
    }
}
