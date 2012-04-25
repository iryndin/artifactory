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

package org.artifactory.addon.p2;

import org.artifactory.descriptor.repo.RemoteRepoDescriptor;

import java.io.Serializable;

/**
 * Date: 9/26/11
 * Time: 2:05 PM
 *
 * @author Fred Simon
 */
public class P2RemoteRepository implements Serializable {
    /**
     * The descriptor of the remote repository up-to-date with either the modified or new data
     */
    public final RemoteRepoDescriptor descriptor;
    /**
     * Repo is already a part of the virtual repo list
     */
    public final boolean alreadyIncluded;
    /**
     * The remote repo is already defined remote repository.
     */
    public final boolean exist;
    /**
     * Repo exists but will be modified to enable p2 upon save
     */
    public final boolean modified;
    /**
     * New remote repo will be created
     */
    public final boolean toCreate;
    /**
     * True if the user selected the checkbox to approve the action on this remote repo
     */
    public boolean selected;

    public P2RemoteRepository(RemoteRepoDescriptor descriptor,
            boolean alreadyIncluded,
            boolean exist,
            boolean modified,
            boolean toCreate) {
        this.descriptor = descriptor;
        this.alreadyIncluded = alreadyIncluded;
        this.exist = exist;
        this.modified = modified;
        this.toCreate = toCreate;
        this.selected = true;
    }
}
