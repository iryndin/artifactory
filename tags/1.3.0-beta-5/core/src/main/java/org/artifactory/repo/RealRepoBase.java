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
package org.artifactory.repo;

import org.artifactory.api.fs.ItemInfo;
import org.artifactory.descriptor.repo.RealRepoDescriptor;
import org.artifactory.maven.MavenUtils;
import org.artifactory.repo.service.InternalRepositoryService;
import org.artifactory.utils.PathMatcher;
import org.artifactory.utils.PathUtils;

import java.util.List;


public abstract class RealRepoBase<T extends RealRepoDescriptor>
        extends RepoBase<T> implements RealRepo<T> {

    @SuppressWarnings({"UnusedDeclaration"})
    private static final org.apache.log4j.Logger LOGGER =
            org.apache.log4j.Logger.getLogger(RealRepoBase.class);

    private List<String> includes;
    private List<String> excludes;

    protected RealRepoBase(InternalRepositoryService repositoryService) {
        super(repositoryService);
    }

    public RealRepoBase(InternalRepositoryService repositoryService, T descriptor) {
        this(repositoryService);
        setDescriptor(descriptor);
    }

    @Override
    public void setDescriptor(T descriptor) {
        super.setDescriptor(descriptor);
        this.includes = PathUtils.delimitedListToStringList(
                descriptor.getIncludesPattern(), ",", "\r\n\f ");
        this.excludes = PathUtils.delimitedListToStringList(
                descriptor.getExcludesPattern(), ",", "\r\n\f ");
        if (excludes.isEmpty()) {
            excludes = null;
        } else {
            excludes.addAll(PathMatcher.DEFAULT_EXCLUDES);
        }
    }

    public boolean isHandleReleases() {
        return getDescriptor().isHandleReleases();
    }

    public boolean isHandleSnapshots() {
        return getDescriptor().isHandleSnapshots();
    }

    public String getIncludesPattern() {
        return getDescriptor().getIncludesPattern();
    }

    public String getExcludesPattern() {
        return getDescriptor().getExcludesPattern();
    }

    public boolean isBlackedOut() {
        return getDescriptor().isBlackedOut();
    }

    public int getMaxUniqueSnapshots() {
        return getDescriptor().getMaxUniqueSnapshots();
    }

    @SuppressWarnings({"OverlyComplexMethod"})
    public boolean accepts(String path) {
        String toCheck = path;
        //For artifactory metadata the pattern apply to the object it represents
        if (path.endsWith(ItemInfo.METADATA_FOLDER)) {
            toCheck = path.substring(0, path.length() - ItemInfo.METADATA_FOLDER.length() - 1);
        }
        List<String> toExcludes = excludes;
        if (excludes != null && !excludes.isEmpty()) {
            toExcludes = PathMatcher.DEFAULT_EXCLUDES;
        }
        return PathMatcher.matches(toCheck, includes, toExcludes);
    }

    public boolean handles(String path) {
        if (MavenUtils.isMetadata(path) || MavenUtils.isChecksum(path)) {
            return true;
        }
        boolean snapshot = MavenUtils.isSnapshot(path);
        if (snapshot && !isHandleSnapshots()) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(this + " rejected '" + path + "': not handling snapshots.");
            }
            return false;
        } else if (!snapshot && !isHandleReleases()) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(this + " rejected '" + path + "': not handling releases.");
            }
            return false;
        }
        return true;
    }

    public boolean isLocal() {
        return getDescriptor().isLocal();
    }
}