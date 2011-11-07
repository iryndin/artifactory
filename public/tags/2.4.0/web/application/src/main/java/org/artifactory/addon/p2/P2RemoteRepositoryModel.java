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

package org.artifactory.addon.p2;

import java.io.Serializable;

/**
 * The model class for P2 remote repositories to create/modify/add.
 *
 * @author Yossi Shaul
 */
public class P2RemoteRepositoryModel implements Serializable {
    public final P2RemoteRepository p2RemoteRepository;
    /**
     * True if the checkbox can be checked. It cannot when there is nothing to do (already included).
     */
    boolean selectable;

    public P2RemoteRepositoryModel(P2RemoteRepository p2RemoteRepository) {
        this.p2RemoteRepository = p2RemoteRepository;
        // If already included and supports P2 (no need to modify), checkbox is disabled
        selectable = !p2RemoteRepository.alreadyIncluded || p2RemoteRepository.modified;
    }

    public boolean isSelectable() {
        return selectable;
    }

    public boolean isSelected() {
        return p2RemoteRepository.selected;
    }

    /**
     * Toggle selection of the checkbox. Called from the UI.
     */
    public void setSelected(boolean selected) {
        p2RemoteRepository.selected = selected;
    }

    public boolean isToCreate() {
        return p2RemoteRepository.toCreate;
    }

    public String getRepoKey() {
        return p2RemoteRepository.descriptor.getKey();
    }

    /**
     * Sets a new repo key. Called from the UI.
     */
    public void setRepoKey(String repoKey) {
        p2RemoteRepository.descriptor.setKey(repoKey);
    }

    public String getRepoUrl() {
        return p2RemoteRepository.descriptor.getUrl();
    }

    public String getAction() {
        if (p2RemoteRepository.toCreate) {
            return "Create";
        } else if (p2RemoteRepository.modified) {
            return "Modify";
        } else if (p2RemoteRepository.exist && !p2RemoteRepository.alreadyIncluded) {
            return "Include";
        } else { // already included
            return "Included";
        }
    }
}
