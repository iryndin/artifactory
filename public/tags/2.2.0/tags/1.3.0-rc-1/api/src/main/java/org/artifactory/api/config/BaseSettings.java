/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.artifactory.api.config;

import org.artifactory.descriptor.repo.LocalRepoDescriptor;

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
    private boolean verbose = false;
    private boolean failFast = false;
    private boolean failIfEmpty = false;

    /**
     * List of repositories to do export or import on. When empty - export or import all
     */
    private List<LocalRepoDescriptor> repositories = Collections.emptyList();

    public BaseSettings(File baseDir) {
        this.baseDir = baseDir;
    }

    public BaseSettings(File baseDir, BaseSettings settings) {
        this(baseDir);
        this.includeMetadata = settings.includeMetadata;
        this.repositories = settings.repositories;
        this.verbose = settings.verbose;
        this.failFast = settings.failFast;
        this.failIfEmpty = settings.failIfEmpty;
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
    }

    public boolean isFailFast() {
        return failFast;
    }

    public void setFailFast(boolean failFast) {
        this.failFast = failFast;
    }

    public boolean isFailIfEmpty() {
        return failIfEmpty;
    }

    public void setFailIfEmpty(boolean failIfEmpty) {
        this.failIfEmpty = failIfEmpty;
    }
}
