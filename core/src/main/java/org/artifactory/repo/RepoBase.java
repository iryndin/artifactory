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

import org.artifactory.api.fs.ItemInfo;
import org.artifactory.api.mime.NamingUtils;
import org.artifactory.api.repo.RepoPath;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.descriptor.repo.RepoDescriptor;
import org.artifactory.jcr.md.MetadataService;
import org.artifactory.repo.service.InternalRepositoryService;
import org.artifactory.spring.InternalContextHelper;
import org.artifactory.util.PathMatcher;
import org.artifactory.util.PathUtils;
import org.springframework.util.StringUtils;

import java.util.List;

public abstract class RepoBase<T extends RepoDescriptor> implements Repo<T> {
    private T descriptor;
    private InternalRepositoryService repositoryService;
    private MetadataService metadataService;

    protected List<String> includes;
    protected List<String> excludes;

    protected RepoBase(InternalRepositoryService repositoryService) {
        this.repositoryService = repositoryService;
        //The metadata service was intialized before so we can locate it at this stage
        this.metadataService = InternalContextHelper.get().beanForType(MetadataService.class);
        this.excludes = PathMatcher.getDefaultExcludes();
    }

    protected RepoBase(InternalRepositoryService repositoryService, T descriptor) {
        this(repositoryService);
        setDescriptor(descriptor);
    }

    public void setDescriptor(T descriptor) {
        this.descriptor = descriptor;
        this.includes = PathUtils.delimitedListToStringList(descriptor.getIncludesPattern(), ",", "\r\n\f ");
        this.excludes = PathUtils.delimitedListToStringList(descriptor.getExcludesPattern(), ",", "\r\n\f ");
        excludes.addAll(PathMatcher.getDefaultExcludes());
    }

    public T getDescriptor() {
        return descriptor;
    }

    public InternalRepositoryService getRepositoryService() {
        return repositoryService;
    }

    public final MetadataService getMetadataService() {
        return metadataService;
    }

    public String getKey() {
        return descriptor.getKey();
    }

    public String getDescription() {
        return descriptor.getDescription();
    }

    public boolean isReal() {
        return getDescriptor().isReal();
    }

    public boolean accepts(RepoPath repoPath) {
        String path = repoPath.getPath();
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

    @Override
    public String toString() {
        return getKey();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RepoBase)) {
            return false;
        }
        RepoBase base = (RepoBase) o;
        return descriptor.equals(base.descriptor);
    }

    @Override
    public int hashCode() {
        return descriptor.hashCode();
    }

    protected final AuthorizationService getAuthorizationService() {
        // TODO: Analyze the optimization if made as a member
        return InternalContextHelper.get().getAuthorizationService();
    }
}