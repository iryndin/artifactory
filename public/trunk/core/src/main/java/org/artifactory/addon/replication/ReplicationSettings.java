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

package org.artifactory.addon.replication;

import org.artifactory.addon.ReplicationAddon;
import org.artifactory.repo.RepoPath;

import java.io.Writer;

/**
 * @author Noam Y. Tenne
 */
public class ReplicationSettings {

    private final RepoPath repoPath;
    private final boolean progress;
    private final int mark;
    private final boolean deleteExisting;
    private final boolean includeProperties;
    private final ReplicationAddon.Overwrite overwrite;
    private final Writer responseWriter;

    /**
     * <B>NOTE<B>: Try to refrain from using this constructor directly and use the builder instead
     */
    ReplicationSettings(RepoPath repoPath, boolean progress, int mark, boolean deleteExisting,
            boolean includeProperties, ReplicationAddon.Overwrite overwrite, Writer responseWriter) {
        this.repoPath = repoPath;
        this.progress = progress;
        this.mark = mark;
        this.deleteExisting = deleteExisting;
        this.includeProperties = includeProperties;
        this.overwrite = overwrite;
        this.responseWriter = responseWriter;
    }

    public RepoPath getRepoPath() {
        return repoPath;
    }

    public boolean isProgress() {
        return progress;
    }

    public int getMark() {
        return mark;
    }

    public boolean isDeleteExisting() {
        return deleteExisting;
    }

    public boolean isIncludeProperties() {
        return includeProperties;
    }

    public ReplicationAddon.Overwrite getOverwrite() {
        return overwrite;
    }

    public Writer getResponseWriter() {
        return responseWriter;
    }
}