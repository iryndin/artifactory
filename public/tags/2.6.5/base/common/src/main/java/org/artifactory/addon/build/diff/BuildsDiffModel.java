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

package org.artifactory.addon.build.diff;

import org.artifactory.repo.RepoPath;

import java.io.Serializable;

/**
 * Builds diff model object, wraps artifacts diff or dependencies diff or environment variables diff.
 *
 * @author Shay Yaakov
 */
public class BuildsDiffModel implements Serializable {
    private String firstItemName;
    private String secondItemName;
    private Status status;

    /**
     * Module id is not mandatory, it is being used by artifacts and dependencies diff
     */
    private String moduleId;

    /**
     * RepoPath is not mandatory, it represents the repo path to the first item (artifact/dependency only)
     */
    private RepoPath repoPath;

    /**
     * Optionally indicate if the dependency is internal (is also an artifact of one of the build modules)
     */
    private boolean internalDependency;

    public String getFirstItemName() {
        return firstItemName;
    }

    public void setFirstItemName(String firstItemName) {
        this.firstItemName = firstItemName;
    }

    public String getSecondItemName() {
        return secondItemName;
    }

    public void setSecondItemName(String secondItemName) {
        this.secondItemName = secondItemName;
    }

    public String getModuleId() {
        return moduleId;
    }

    public void setModuleId(String moduleId) {
        this.moduleId = moduleId;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public RepoPath getRepoPath() {
        return repoPath;
    }

    public void setRepoPath(RepoPath repoPath) {
        this.repoPath = repoPath;
    }

    public boolean isInternalDependency() {
        return internalDependency;
    }

    public void setInternalDependency(boolean internalDependency) {
        this.internalDependency = internalDependency;
    }

    public enum Status {
        NEW("New"),
        UPDATED("Updated"),
        REMOVED("Removed"),
        UNCHANGED("Unchanged");

        private String description;

        Status(String description) {
            this.description = description;
        }

        @Override
        public String toString() {
            return description;
        }
    }
}
