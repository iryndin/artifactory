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

package org.artifactory.jcr.md;

import org.artifactory.api.repo.exception.RepositoryRuntimeException;
import org.artifactory.jcr.JcrPath;
import org.artifactory.repo.RepoPath;
import org.artifactory.repo.jcr.JcrHelper;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

/**
 * @author freds
 */
public class MetadataAwareAdapter implements MetadataAware {

    private final Node node;
    private final String absPath;
    private final RepoPath repoPath;
    private final boolean folder;

    public MetadataAwareAdapter(Node node) {
        try {
            this.node = node;
            this.absPath = node.getPath();
            this.repoPath = JcrPath.get().getRepoPath(absPath);
            this.folder = JcrHelper.isFolder(node);
        } catch (RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    public Node getNode() {
        return node;
    }

    public String getAbsolutePath() {
        return absPath;
    }

    public RepoPath getRepoPath() {
        return repoPath;
    }

    public boolean isFile() {
        return !folder;
    }

    public boolean isDirectory() {
        return folder;
    }
}
