/*
 * This file is part of Artifactory.
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

package org.artifactory.repo;

import org.apache.commons.httpclient.HttpStatus;
import org.artifactory.api.common.StatusHolder;
import org.artifactory.api.fs.ItemInfo;
import org.artifactory.api.maven.MavenNaming;
import org.artifactory.api.mime.NamingUtils;
import org.artifactory.api.repo.RepoPath;
import org.artifactory.descriptor.repo.RealRepoDescriptor;
import org.artifactory.log.LoggerFactory;
import org.artifactory.repo.service.InternalRepositoryService;
import org.artifactory.util.PathMatcher;
import org.artifactory.util.PathUtils;
import org.slf4j.Logger;
import org.springframework.util.StringUtils;

import java.util.List;


public abstract class RealRepoBase<T extends RealRepoDescriptor> extends RepoBase<T> implements RealRepo<T> {
    private static final Logger log = LoggerFactory.getLogger(RealRepoBase.class);

    private List<String> includes;
    private List<String> excludes;

    protected RealRepoBase(InternalRepositoryService repositoryService) {
        super(repositoryService);
        this.excludes = PathMatcher.getDefaultExcludes();
    }

    public RealRepoBase(InternalRepositoryService repositoryService, T descriptor) {
        this(repositoryService);
        setDescriptor(descriptor);
    }

    @Override
    public void setDescriptor(T descriptor) {
        super.setDescriptor(descriptor);
        this.includes = PathUtils.delimitedListToStringList(descriptor.getIncludesPattern(), ",", "\r\n\f ");
        this.excludes = PathUtils.delimitedListToStringList(descriptor.getExcludesPattern(), ",", "\r\n\f ");
        excludes.addAll(PathMatcher.getDefaultExcludes());
    }

    @Override
    public T getDescriptor() {
        return super.getDescriptor();
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

    public boolean accepts(String path) {
        // TODO: Refactor this using RepoPath
        if (NamingUtils.isSystem(path)) {
            // includes/excludes should not affect system paths
            return true;
        }

        String toCheck = path;
        //For artifactory metadata the pattern apply to the object it represents
        if (path.endsWith(ItemInfo.METADATA_FOLDER)) {
            toCheck = path.substring(0, path.length() - ItemInfo.METADATA_FOLDER.length() - 1);
        } else if (NamingUtils.isMetadata(path)) {
            toCheck = NamingUtils.getMetadataParentPath(path);
        }
        return !StringUtils.hasLength(toCheck) || PathMatcher.matches(toCheck, includes, excludes);
    }

    public boolean handles(String path) {
        // TODO: Refactor this using RepoPath
        if (NamingUtils.isMetadata(path) || NamingUtils.isChecksum(path) || NamingUtils.isSystem(path)) {
            return true;
        }
        boolean snapshot = MavenNaming.isSnapshot(path);
        if (snapshot && !isHandleSnapshots()) {
            if (log.isDebugEnabled()) {
                log.debug("{} rejected '{}': not handling snapshots.", this, path);
            }
            return false;
        } else if (!snapshot && !isHandleReleases()) {
            if (log.isDebugEnabled()) {
                log.debug("{} rejected '{}': not handling releases.", this, path);
            }
            return false;
        }
        return true;
    }

    public boolean isLocal() {
        return getDescriptor().isLocal();
    }

    public StatusHolder assertValidPath(RepoPath repoPath) {
        StatusHolder statusHolder = new StatusHolder();
        statusHolder.setActivateLogging(log.isDebugEnabled());
        if (isBlackedOut()) {
            statusHolder.setError(
                    "The repository '" + this.getKey() + "' is blacked out and cannot accept artifact '" + repoPath +
                            "'.", HttpStatus.SC_FORBIDDEN, log);
        } else if (!handles(repoPath.getPath())) {
            statusHolder.setError("The repository '" + this.getKey() + "' rejected the artifact '" + repoPath +
                    "' due to its snapshot/release handling policy.",
                    HttpStatus.SC_FORBIDDEN, log);
        } else if (!accepts(repoPath.getPath())) {
            statusHolder.setError("The repository '" + this.getKey() + "' rejected the artifact '" + repoPath +
                    "' due to its include/exclude patterns settings.",
                    HttpStatus.SC_FORBIDDEN, log);

        }
        return statusHolder;
    }
}