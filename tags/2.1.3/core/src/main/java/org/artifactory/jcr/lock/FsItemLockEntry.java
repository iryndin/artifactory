/*
 * This file is part of Artifactory.
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

package org.artifactory.jcr.lock;

import org.artifactory.api.repo.RepoPath;
import org.artifactory.jcr.fs.JcrFsItem;

/**
 * @author Noam Tenne
 */
public interface FsItemLockEntry {
    RepoPath getRepoPath();

    JcrFsItem getLockedFsItem();

    JcrFsItem getImmutableFsItem();

    MonitoringReadWriteLock getLock();

    void setWriteFsItem(JcrFsItem fsItem, JcrFsItem mutableFsItem);

    void setReadFsItem(JcrFsItem fsItem);

    boolean isLockedByMe();
}
