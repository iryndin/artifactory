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

/*
 * Copyright 2012 JFrog Ltd. All rights reserved.
 * JFROG PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package org.artifactory.repo.service.mover;

import org.artifactory.md.Properties;
import org.artifactory.repo.RepoPath;

/**
 * Repo path mover configuration object
 *
 * @author Noam Y. Tenne
 */
public class MoverConfig {
    private final RepoPath fromRepoPath;
    private final RepoPath targetLocalRepoPath;
    private final String targetLocalRepoKey;
    private final boolean copy;
    private final boolean dryRun;
    private final boolean executeMavenMetadataCalculation;
    private final boolean searchResult;
    private final Properties properties;
    private final boolean suppressLayouts;
    private final boolean failFast;

    /**
     * Main constructor
     *
     * @param fromRepoPath       Source repo path
     * @param targetLocalRepoKey Target repo key
     * @param copy               Is a copy being performed
     * @param dryRun             Is the current run a dry one (no items actually moved)
     * @param executeMavenMetadataCalculation
     *                           Should immediately execute metadata calculation, or schedule
     * @param searchResult       Is moving search results
     * @param suppressLayouts    True if path translation across different layouts should be suppressed.
     * @param failFast           Flag to indicate whether the operation should fail upon encountering an error.
     */
    public MoverConfig(RepoPath fromRepoPath, RepoPath targetLocalRepoPath, String targetLocalRepoKey, boolean copy,
            boolean dryRun, boolean executeMavenMetadataCalculation, boolean searchResult, Properties properties,
            boolean suppressLayouts, boolean failFast) {
        this.fromRepoPath = fromRepoPath;
        this.targetLocalRepoPath = targetLocalRepoPath;
        this.targetLocalRepoKey = targetLocalRepoKey;
        this.copy = copy;
        this.dryRun = dryRun;
        this.executeMavenMetadataCalculation = executeMavenMetadataCalculation;
        this.searchResult = searchResult;
        this.properties = properties;
        this.suppressLayouts = suppressLayouts;
        this.failFast = failFast;
    }

    public Properties getProperties() {
        return properties;
    }

    /**
     * Returns the repo path of the source
     *
     * @return Source repo path
     */
    public RepoPath getFromRepoPath() {
        return fromRepoPath;
    }

    /**
     * @return Returns the target repo path (if null, the target path should be in targetLocalRepoKey)
     */
    public RepoPath getTargetLocalRepoPath() {
        return targetLocalRepoPath;
    }

    /**
     * @return Returns the repo key of the target (if null, the target path should be in targetLocalRepoPath)
     */
    public String getTargetLocalRepoKey() {
        return targetLocalRepoKey;
    }

    /**
     * Indicates if a copy is being performed
     *
     * @return True if performing a copy, false if not
     */
    public boolean isCopy() {
        return copy;
    }

    /**
     * Indicates if the current run is a dry one (no items actually moved)
     *
     * @return True if run is dry, false if not
     */
    public boolean isDryRun() {
        return dryRun;
    }

    /**
     * Indicates if the metadata should be calculated immediately or scheduled
     *
     * @return True if metadata should be calculated immediately, false if not
     */
    public boolean isExecuteMavenMetadataCalculation() {
        return executeMavenMetadataCalculation;
    }

    /**
     * Indicates if search results are being moved (will perform empty dir cleanup)
     *
     * @return True if search results are being moved, false if not
     */
    public boolean isSearchResult() {
        return searchResult;
    }

    /**
     * Indicates whether path translation across different layouts should be suppressed.
     *
     * @return True if path translation should be suppressed
     */
    public boolean isSuppressLayouts() {
        return suppressLayouts;
    }

    /**
     * Indicates whether the operation should fail upon encountering an error.
     *
     * @return True if the operation should fail on the first error
     */
    public boolean isFailFast() {
        return failFast;
    }
}
