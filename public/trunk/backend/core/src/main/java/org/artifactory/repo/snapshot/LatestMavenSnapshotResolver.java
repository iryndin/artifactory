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

package org.artifactory.repo.snapshot;

import org.artifactory.api.context.ContextHelper;
import org.artifactory.api.module.ModuleInfo;
import org.artifactory.api.repo.RepositoryService;
import org.artifactory.api.request.TranslatedArtifactoryRequest;
import org.artifactory.common.ConstantValues;
import org.artifactory.mime.MavenNaming;
import org.artifactory.repo.InternalRepoPathFactory;
import org.artifactory.repo.Repo;
import org.artifactory.repo.RepoPath;
import org.artifactory.request.ArtifactoryRequest;
import org.artifactory.request.DownloadRequestContext;
import org.artifactory.request.InternalRequestContext;
import org.artifactory.request.NullRequestContext;

/**
 * Resolves the latest unique snapshot version given a non-unique Maven snapshot artifact request.
 *
 * @author Shay Yaakov
 */
public abstract class LatestMavenSnapshotResolver {

    /**
     * Checks if the given path is in a non unique maven format and replaces it with
     * the path to the latest version of that file (whether it's unique or not).
     *
     * @param path
     * @return If the given path is in a maven non unique format and this repo is in maven layout then returns the
     *         latest path; if not, returns the given path as is
     */
    public InternalRequestContext convertNonUniqueSnapshotPath(Repo repo, InternalRequestContext requestContext) {
        if (ConstantValues.mvnLatestSnapshotResolutionEnabled.getBoolean()) {
            String path = requestContext.getResourcePath();
            if (MavenNaming.isNonUniqueSnapshot(path) && repo.getDescriptor().isMavenRepoLayout()) {
                ModuleInfo originalModuleInfo = getRepositoryService().getItemModuleInfo(repo.getRepoPath(path));
                if (!originalModuleInfo.isValid()) {
                    return requestContext;
                }

                requestContext = getRequestContext(requestContext, repo, originalModuleInfo);
            }
        }

        return requestContext;
    }

    /**
     * Derived classes should implement their logic and return the transformed {@link InternalRequestContext}
     *
     * @param requestContext     the original request context to transform
     * @param repo
     * @param path
     * @param originalModuleInfo
     * @return
     */
    protected abstract InternalRequestContext getRequestContext(InternalRequestContext requestContext, Repo repo,
            ModuleInfo originalModuleInfo);

    protected RepositoryService getRepositoryService() {
        return ContextHelper.get().getRepositoryService();
    }

    /**
     * Returns a new request context if the translated path is different from the original request path
     *
     * @param repo    Target repository
     * @param context Request context to translate
     * @return Translated context if needed, original if not needed or if there is insufficient info
     */
    protected InternalRequestContext translateRepoRequestContext(InternalRequestContext context, Repo repo,
            String translatedPath) {
        String originalPath = context.getResourcePath();
        if (originalPath.equals(translatedPath)) {
            return context;
        }

        if (context instanceof NullRequestContext) {
            return new NullRequestContext(repo.getRepoPath(translatedPath));
        }

        ArtifactoryRequest artifactoryRequest = ((DownloadRequestContext) context).getRequest();
        RepoPath translatedRepoPath = InternalRepoPathFactory.create(artifactoryRequest.getRepoKey(), translatedPath);
        TranslatedArtifactoryRequest translatedRequest = new TranslatedArtifactoryRequest(translatedRepoPath,
                artifactoryRequest);
        return new DownloadRequestContext(translatedRequest);
    }
}
