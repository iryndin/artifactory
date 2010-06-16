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

package org.artifactory.api.config;

import org.artifactory.api.common.MultiStatusHolder;
import org.artifactory.descriptor.repo.LocalRepoDescriptor;
import org.slf4j.Logger;

import java.io.File;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * @author freds
 * @date Sep 29, 2008
 */
public class BaseSettings implements Serializable {
    private final File baseDir;
    private boolean includeMetadata = true;
    private boolean verbose;
    private boolean failFast;
    private boolean failIfEmpty;
    protected boolean excludeContent;
    private MultiStatusHolder statusHolder = new MultiStatusHolder();
    /**
     * List of repositories to do export or import on. When empty - export or import all
     */
    private List<LocalRepoDescriptor> repositories = Collections.emptyList();

    public BaseSettings(File baseDir) {
        this.baseDir = baseDir;
    }

    public BaseSettings(File baseDir, MultiStatusHolder statusHolder) {
        this.baseDir = baseDir;
        this.statusHolder = statusHolder;
    }

    public BaseSettings(File baseDir, BaseSettings settings) {
        this(baseDir);
        this.includeMetadata = settings.includeMetadata;
        this.repositories = settings.repositories;
        this.verbose = settings.verbose;
        this.failFast = settings.failFast;
        this.failIfEmpty = settings.failIfEmpty;
        this.excludeContent = settings.excludeContent;
        this.statusHolder = settings.statusHolder;
    }

    public BaseSettings(File baseDir, BaseSettings settings, MultiStatusHolder statusHolder) {
        this(baseDir, settings);
        this.statusHolder = statusHolder;
    }

    /**
     * @return Base directory of the operation (target directory of the export or source directory of an import)
     */
    public File getBaseDir() {
        return baseDir;
    }

    public boolean isIncludeMetadata() {
        return includeMetadata;
    }

    public void setIncludeMetadata(boolean includeMetadata) {
        this.includeMetadata = includeMetadata;
    }

    /**
     * @return List of repositories to do export or import on. Empty if needs to export or import all.
     */
    public List<LocalRepoDescriptor> getRepositories() {
        return repositories;
    }

    public void setRepositories(List<LocalRepoDescriptor> repositories) {
        this.repositories = repositories;
    }

    public boolean isVerbose() {
        return verbose;
    }

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
        statusHolder.setVerbose(verbose);
    }

    public boolean isFailFast() {
        return failFast;
    }

    public void setFailFast(boolean failFast) {
        this.failFast = failFast;
        statusHolder.setFailFast(failFast);
    }

    public boolean isFailIfEmpty() {
        return failIfEmpty;
    }

    public void setFailIfEmpty(boolean failIfEmpty) {
        this.failIfEmpty = failIfEmpty;
    }

    public MultiStatusHolder getStatusHolder() {
        return statusHolder;
    }

    public boolean isExcludeContent() {
        return excludeContent;
    }

    public void setExcludeContent(boolean excludeContent) {
        this.excludeContent = excludeContent;
    }

    public void alertFailIfEmpty(String message, Logger log) {
        if (isFailIfEmpty()) {
            statusHolder.setError(message, log);
        } else {
            statusHolder.setWarning(message, log);
        }
    }
}
