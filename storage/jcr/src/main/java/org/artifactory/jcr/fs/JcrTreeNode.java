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

package org.artifactory.jcr.fs;

import org.artifactory.repo.RepoPath;

import java.util.Calendar;
import java.util.Set;

/**
 * @author Noam Y. Tenne
 */
public class JcrTreeNode {

    private final RepoPath repoPath;
    private final boolean isFolder;
    private final Calendar created;
    private final Set<JcrTreeNode> children;

    public JcrTreeNode(RepoPath repoPath, boolean folder, Calendar created, Set<JcrTreeNode> children) {
        this.repoPath = repoPath;
        isFolder = folder;
        this.created = created;
        this.children = children;
    }

    public RepoPath getRepoPath() {
        return repoPath;
    }

    public boolean isFolder() {
        return isFolder;
    }

    public Calendar getCreated() {
        return created;
    }

    public Set<JcrTreeNode> getChildren() {
        return children;
    }

    public String getName() {
        return repoPath.getName();
    }
}