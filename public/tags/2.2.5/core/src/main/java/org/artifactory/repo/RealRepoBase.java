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

package org.artifactory.repo;

import org.artifactory.api.common.StatusHolder;
import org.artifactory.api.maven.MavenNaming;
import org.artifactory.api.mime.NamingUtils;
import org.artifactory.api.repo.RepoPath;
import org.artifactory.api.repo.exception.BlackedOutException;
import org.artifactory.api.repo.exception.IncludeExcludeException;
import org.artifactory.api.repo.exception.SnapshotPolicyException;
import org.artifactory.descriptor.repo.RealRepoDescriptor;
import org.artifactory.log.LoggerFactory;
import org.artifactory.repo.service.InternalRepositoryService;
import org.slf4j.Logger;


public abstract class RealRepoBase<T extends RealRepoDescriptor> extends RepoBase<T> implements RealRepo<T> {
    private static final Logger log = LoggerFactory.getLogger(RealRepoBase.class);

    protected RealRepoBase(InternalRepositoryService repositoryService) {
        super(repositoryService);
    }

    public RealRepoBase(InternalRepositoryService repositoryService, T descriptor) {
        this(repositoryService);
        setDescriptor(descriptor);
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

    public boolean isBlackedOut() {
        return getDescriptor().isBlackedOut();
    }

    public int getMaxUniqueSnapshots() {
        return getDescriptor().getMaxUniqueSnapshots();
    }


    public boolean handles(String path) {
        // TODO: Refactor this using RepoPath
        if (NamingUtils.isMetadata(path) || NamingUtils.isChecksum(path) || NamingUtils.isSystem(path)) {
            return true;
        }
        boolean snapshot = MavenNaming.isSnapshot(path);
        if (snapshot && !isHandleSnapshots()) {
            log.debug("{} rejected '{}': not handling snapshots.", this, path);
            return false;
        } else if (!snapshot && !isHandleReleases()) {
            log.debug("{} rejected '{}': not handling releases.", this, path);
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
            BlackedOutException exception = new BlackedOutException(this.getDescriptor(), repoPath);
            statusHolder.setError(exception.getMessage(), exception.getErrorCode(), exception, log);
        } else if (!handles(repoPath.getPath())) {
            SnapshotPolicyException exception = new SnapshotPolicyException(this.getDescriptor(), repoPath);
            statusHolder.setError(exception.getMessage(), exception.getErrorCode(), exception, log);
        } else if (!accepts(repoPath)) {
            IncludeExcludeException exception = new IncludeExcludeException(this.getDescriptor(), repoPath);
            statusHolder.setError(exception.getMessage(), exception.getErrorCode(), exception, log);
        }
        return statusHolder;
    }
}