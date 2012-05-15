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

package org.artifactory.repo.virtual;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.httpclient.HttpStatus;
import org.artifactory.addon.AddonsManager;
import org.artifactory.addon.LayoutsCoreAddon;
import org.artifactory.api.config.CentralConfigService;
import org.artifactory.api.context.ArtifactoryContext;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.api.module.ModuleInfo;
import org.artifactory.api.repo.exception.FileExpectedException;
import org.artifactory.api.repo.exception.RepoRejectException;
import org.artifactory.api.request.TranslatedArtifactoryRequest;
import org.artifactory.descriptor.repo.RemoteRepoDescriptor;
import org.artifactory.descriptor.repo.RepoLayout;
import org.artifactory.fs.RepoResource;
import org.artifactory.jcr.lock.LockingHelper;
import org.artifactory.log.LoggerFactory;
import org.artifactory.mime.MavenNaming;
import org.artifactory.mime.NamingUtils;
import org.artifactory.repo.InternalRepoPathFactory;
import org.artifactory.repo.LocalCacheRepo;
import org.artifactory.repo.LocalRepo;
import org.artifactory.repo.RealRepo;
import org.artifactory.repo.RemoteRepo;
import org.artifactory.repo.Repo;
import org.artifactory.repo.RepoPath;
import org.artifactory.repo.service.InternalRepositoryService;
import org.artifactory.request.ArtifactoryRequest;
import org.artifactory.request.DownloadRequestContext;
import org.artifactory.request.InternalRequestContext;
import org.artifactory.request.NullRequestContext;
import org.artifactory.request.RequestContext;
import org.artifactory.request.RequestTraceLogger;
import org.artifactory.resource.ResourceStreamHandle;
import org.artifactory.resource.UnfoundRepoResource;
import org.slf4j.Logger;

import javax.jcr.RepositoryException;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Default download strategy of a virtual repository.
 *
 * @author Yossi Shaul
 */
public class VirtualRepoDownloadStrategy {
    private final static Logger log = LoggerFactory.getLogger(VirtualRepoDownloadStrategy.class);

    private InternalRepositoryService repositoryService;

    private CentralConfigService centralConfig;

    private final VirtualRepo virtualRepo;
    private LayoutsCoreAddon layoutsCoreAddon;

    public VirtualRepoDownloadStrategy(VirtualRepo virtualRepo) {
        this.virtualRepo = virtualRepo;
        ArtifactoryContext artifactoryContext = ContextHelper.get();
        centralConfig = artifactoryContext.getCentralConfig();
        repositoryService = artifactoryContext.beanForType(InternalRepositoryService.class);
        AddonsManager addonsManager = artifactoryContext.beanForType(AddonsManager.class);
        layoutsCoreAddon = addonsManager.addonByType(LayoutsCoreAddon.class);
    }

    public RepoResource getInfo(InternalRequestContext context) {
        RequestTraceLogger.log("Consulting the virtual repo download strategy");
        // first look in local storage
        RepoResource cachedResource = getInfoFromLocalStorage(context);
        String path = context.getResourcePath();
        if (MavenNaming.isIndex(path)) {
            // for index files just return the result from the cache (we don't want to process it or to return index
            // from other repositories)
            RequestTraceLogger.log("Requested resource is a Maven index - returning the cached resource");
            return cachedResource;
        }

        // release the read lock on the virtual repo local cache to prevent deadlock in any of the interceptors
        // (in case one of them needs to write back to the virtual repo cache)
        RepoPath localCacheRepoPath = InternalRepoPathFactory.create(virtualRepo.getKey(), context.getResourcePath());
        RequestTraceLogger.log("Releasing the cached resource read lock");
        LockingHelper.releaseReadLock(localCacheRepoPath);

        // not found in local virtual repository storage, look in configured repositories
        RepoResource searchableResource = getInfoFromSearchableRepositories(context);
        if (!cachedResource.isFound() && !searchableResource.isFound()) {
            // not found
            return searchableResource;
        } else if (cachedResource.isFound() && !searchableResource.isFound()) {
            // delete the local cached artifact and return the not found resource
            RequestTraceLogger.log("Resource was not found but is still cached - removing from the cache");
            virtualRepo.undeploy(localCacheRepoPath, false);
            return searchableResource;
        } else if (cachedResource.isFound() && searchableResource.isFound()) {
            String sourceRepoKey = searchableResource.getResponseRepoPath().getRepoKey();
            // if the searchable resource is a remote repo - it is not downloaded yet, so we shouldn't
            // return the cached artifact
            if (repositoryService.remoteRepositoryByKey(sourceRepoKey) == null &&
                    cachedResource.getLastModified() >= searchableResource.getLastModified()) {
                RequestTraceLogger.log("Resource was found locally and the cached instance is the latest - " +
                        "updating the stats and returning the cached instance");
                // locally stored resource is latest, update the stats of the original resource and return our cached file
                ResourceStreamHandle searchableHandle = null;
                try {
                    // strangely enough, to update the stats we must get the resource stream handle which will also
                    // call update stats (see RTFACT-2958)
                    LocalRepo repo = repositoryService.localOrCachedRepositoryByKey(sourceRepoKey);
                    searchableHandle = repositoryService.getResourceStreamHandle(context, repo, searchableResource);
                } catch (IOException ioe) {
                    RequestTraceLogger.log("Error while updating download stats: %s", ioe.getMessage());
                    log.error("Could not update download stats", ioe);
                } catch (RepoRejectException rre) {
                    RequestTraceLogger.log("Error while updating download stats: %s", rre.getMessage());
                    log.error("Could not update download stats", rre);
                } catch (RepositoryException re) {
                    RequestTraceLogger.log("Error while updating download stats: %s", re.getMessage());
                    log.error("Could not update download stats", re);
                } finally {
                    if (searchableHandle != null) {
                        searchableHandle.close();
                    }
                }
                return cachedResource;
            }
        }

        // found newer resource in the searchable repositories
        RequestTraceLogger.log("Returning resource as found in the aggregated repositories");
        return virtualRepo.interceptBeforeReturn(context, searchableResource);
    }

    private RepoResource getInfoFromLocalStorage(InternalRequestContext context) {
        try {
            RequestTraceLogger.log("Trying to retrieve resource info from the local storage");
            return virtualRepo.storageMixin.getInfo(context);
        } catch (FileExpectedException e) {
            RequestTraceLogger.log("Unable to find resource info in the local storage - " +
                    "already exists as a directory: %s", e.getMessage());
            // see RTFACT-3721
            return new UnfoundRepoResource(
                    InternalRepoPathFactory.create(virtualRepo.getKey(), context.getResourcePath()),
                    "File expected but the cache already points to directory");
        }
    }

    private RepoResource getInfoFromSearchableRepositories(InternalRequestContext context) {
        RequestTraceLogger.log("Searching for info in aggregated repositories");
        String path = context.getResourcePath();
        RepoPath repoPath = InternalRepoPathFactory.create(virtualRepo.getKey(), path);
        RepoResource result;
        try {
            List<RealRepo> repositories = assembleSearchRepositoriesList(repoPath, context);
            if (repositories.isEmpty()) {
                RequestTraceLogger.log("Unable to find aggregated repositories to search within - returning " +
                        "unfound resource");
                return new UnfoundRepoResource(repoPath, "No repository found to serve the request for " + repoPath);
            }

            ModuleInfo artifactModuleInfo = virtualRepo.getItemModuleInfo(path);

            result = virtualRepo.interceptGetInfo(context, repoPath, repositories);
            if (result != null) {
                RequestTraceLogger.log("Received resource from an interceptor - returning");
                return result;
            }

            /**
             * The repo might not define a layout, so also check specifically for a Maven snapshot if the resulting
             * module info is invalid
             */
            boolean mavenSnapshotPath = MavenNaming.isSnapshot(path);
            if (artifactModuleInfo.isIntegration() || (!artifactModuleInfo.isValid() && mavenSnapshotPath)) {
                RequestTraceLogger.log("Processing request as a snapshot resource (Module info validity = %s, " +
                        "Module info identified as integration = %s, Path identified as Maven snapshot = %s)",
                        artifactModuleInfo.isValid(), artifactModuleInfo.isIntegration(), mavenSnapshotPath);
                result = processSnapshot(context, repoPath, repositories);
            } else {
                RequestTraceLogger.log("Processing request as a release resource");
                result = processStandard(context, repoPath, repositories);
            }
        } catch (IOException e) {
            RequestTraceLogger.log("Processing to get resource info: %s", e.getMessage());
            log.error("Failed to get resource info", e);
            result = new UnfoundRepoResource(repoPath, "IOException: " + e.getMessage());
        }
        return result;
    }

    /**
     * @return A list of local and remote repositories to search the resource in, ordered first by type (local non-cache
     *         first) and secondly by order of appearance. We don't simply add all the lists since some of the real
     *         repos are resolved transitively and we might have to remove them in case the path is excluded in the
     *         virtual repo they belong to.
     */
    private List<RealRepo> assembleSearchRepositoriesList(RepoPath repoPath, RequestContext context) {
        RequestTraceLogger.log("Preparing list of aggregated repositories to search in");
        Map<String, LocalRepo> searchableLocalRepositories = Maps.newLinkedHashMap();
        Map<String, LocalCacheRepo> searchableLocalCacheRepositories = Maps.newLinkedHashMap();
        Map<String, RemoteRepo> searchableRemoteRepositories = Maps.newLinkedHashMap();
        deeplyAssembleSearchRepositoryLists(repoPath.getPath(), Maps.<String, VirtualRepo>newLinkedHashMap(),
                searchableLocalRepositories, searchableLocalCacheRepositories, searchableRemoteRepositories);

        //Add all local repositories
        List<RealRepo> repositories = Lists.newArrayList();
        RequestTraceLogger.log("Appending collective local repositories");
        repositories.addAll(searchableLocalRepositories.values());
        //Add all caches
        RequestTraceLogger.log("Appending collective local cache repositories");
        repositories.addAll(searchableLocalCacheRepositories.values());

        //Add all remote repositories conditionally
        boolean fromAnotherArtifactory = context.isFromAnotherArtifactory();
        boolean artifactoryRequestsCanRetrieveRemoteArtifacts =
                virtualRepo.isArtifactoryRequestsCanRetrieveRemoteArtifacts();
        if (fromAnotherArtifactory && !artifactoryRequestsCanRetrieveRemoteArtifacts) {
            //If the request comes from another artifactory don't bother checking any remote repos
            RequestTraceLogger.log("Collective remote repositories aren't appended - Download request was " +
                    "received from another Artifactory instance but is forbidden to search in remote repositories.");
        } else {
            RequestTraceLogger.log("Appending collective remote repositories");
            repositories.addAll(searchableRemoteRepositories.values());
        }
        return repositories;
    }

    /**
     * Iterate over a list of repos until a resource is found in one of them, and return that resource. The first
     * resource that is found is returned. The order of searching is: local repos, cache repos and remote repos (unless
     * request originated from another Artifactory).
     *
     * @param context      Original download request context
     * @param repoPath     The repository path of the resource to find
     * @param repositories List of repositories to look in (doesn't include virtual repos)
     */
    private RepoResource processStandard(InternalRequestContext context, RepoPath repoPath, List<RealRepo> repositories)
            throws IOException {
        // save forbidden unfound response
        UnfoundRepoResource forbidden = null;
        //Locate the resource matching the request
        RepoResource closestMatch = null;

        for (RealRepo repo : repositories) {
            RequestTraceLogger.log("Searching for the resource within %s", repo.getKey());
            // Since we are in process standard, repositories that does not process releases should be skipped.
            // Now, checksums are always considered standard, even if executed against a snapshot repository.
            // So, we should not skip snapshots repositories for checksums.
            String path = repoPath.getPath();
            if (!repo.isHandleReleases() && !NamingUtils.isChecksum(path)) {
                RequestTraceLogger.log("Skipping %s - doesn't handle releases", repo.getKey());
                continue;
            }

            if (closestMatch != null && isExactMatchRequired(repo)) {
                continue;
            }

            InternalRequestContext translatedContext = translateRepoRequestContext(virtualRepo, repo, context);
            if (translatedContext instanceof TranslatedArtifactoryRequest) {
                RequestTraceLogger.log("Request path was translated to %s due to repository layout differences",
                        translatedContext.getResourcePath());
            }
            RepoResource res = repo.getInfo(translatedContext);

            //Retry the original path if the path was translated and failed (RTFACT-4329)
            if (!res.isFound() && !translatedContext.getResourcePath().equals(context.getResourcePath())) {
                RequestTraceLogger.log("Unable to find the resource in the translated path - " +
                        "retrying with the original");
                res = repo.getInfo(context);
            }
            // release all read locks acquired by the repo during the getInfo
            LockingHelper.getSessionLockManager().unlockAllReadLocks(repo.getKey());
            if (res.isFound()) {
                RequestTraceLogger.log("Resource was found in %s", repo.getKey());
                updateResponseRepoPath(repo, res);
                if (res.isExactQueryMatch()) {
                    //return the exact match
                    RequestTraceLogger.log("Resource is an exact match - returning");
                    return res;
                } else {
                    RequestTraceLogger.log("Resource is not an exact match - keeping as closest match");
                    closestMatch = res;
                }
            } else if (forbidden == null) {
                forbidden = checkIfForbidden(res);
                if (forbidden != null) {
                    RequestTraceLogger.log("Request is forbidden by %s", repo.getKey());
                }
            }
        }

        //If we didn't find an exact match return the first found resource (closest match)
        if (closestMatch != null) {
            RequestTraceLogger.log("Unable to find an exact matching resource - returning closest match");
            return closestMatch;
        }

        // not found in any repo
        if (forbidden != null) {
            RequestTraceLogger.log("Returning a forbidden-unfound resource");
            return new UnfoundRepoResource(repoPath, forbidden.getReason(), forbidden.getStatusCode());
        } else {
            RequestTraceLogger.log("Returning an unfound resource");
            return new UnfoundRepoResource(repoPath, "Could not find resource");
        }
    }

    /**
     * Iterate over the repos and return the latest resource found (content or just head information) on the response.
     */
    private RepoResource processSnapshot(InternalRequestContext context, RepoPath repoPath, List<RealRepo> repositories)
            throws IOException {

        //Find the latest in all repositories
        RepoResource latestRes = null;
        // save forbidden unfound response
        UnfoundRepoResource forbidden = null;
        //Traverse the local, caches and remote repositories and search for the newest snapshot
        //Make sure local repos are always searched first
        boolean foundInLocalRepo = false;

        for (RealRepo repo : repositories) {
            RequestTraceLogger.log("Searching for the resource within %s", repo.getKey());
            if (shouldSkipSnapshotRepo(foundInLocalRepo, repo)) {
                RequestTraceLogger.log("Skipping %s", repo.getKey());
                continue;
            }
            InternalRequestContext translatedContext = translateRepoRequestContext(virtualRepo, repo, context);
            String translatedPath = translatedContext.getResourcePath();
            if (translatedContext instanceof TranslatedArtifactoryRequest) {
                RequestTraceLogger.log("Request path was translated to %s due to repository layout differences",
                        translatedPath);
            }

            final RepoResource res = repo.getInfo(translatedContext);
            if (res.isFound()) {
                foundInLocalRepo = isSnapshotFoundInLocalRepo(foundInLocalRepo, repo, translatedPath);
                RequestTraceLogger.log("Resource last modified time - %s",
                        centralConfig.format(res.getLastModified()));

                boolean firstFoundResource = latestRes == null;
                boolean currentResourceIsAnExactMatchAndLatterFoundIsNot = !firstFoundResource &&
                        !latestRes.isExactQueryMatch() && res.isExactQueryMatch();
                boolean currentResourceWasModifiedLater = !firstFoundResource &&
                        (res.getLastModified() > latestRes.getLastModified());

                RequestTraceLogger.log("Current found resource is the first candidate = %s, is an exact match " +
                        "query while the former candidate isn't = %s, has later modified time than former = %s",
                        firstFoundResource, currentResourceIsAnExactMatchAndLatterFoundIsNot,
                        currentResourceWasModifiedLater);

                //If we haven't found one yet
                if (firstFoundResource ||
                        //or this one is a better match
                        currentResourceIsAnExactMatchAndLatterFoundIsNot
                        //or newer than the one found
                        || currentResourceWasModifiedLater) {
                    RequestTraceLogger.log("Selecting current found resource as best candidate");
                    //take it
                    updateResponseRepoPath(repo, res);
                    latestRes = res;
                }
            } else if (forbidden == null) {
                forbidden = checkIfForbidden(res);
                if (forbidden != null) {
                    RequestTraceLogger.log("Request is forbidden by %s", repo.getKey());
                }
            }
        }

        String resourcePath = repoPath.getPath();
        boolean nonFoundRetrievalCacheHit = latestRes != null && !latestRes.isFound();
        if (latestRes == null || nonFoundRetrievalCacheHit) {
            if (forbidden != null) {
                RequestTraceLogger.log("Returning a forbidden-unfound resource");
                return new UnfoundRepoResource(repoPath, forbidden.getReason(), forbidden.getStatusCode());
            } else {
                RequestTraceLogger.log("Returning an unfound resource");
                String msg = "Artifact not found: " + resourcePath +
                        (nonFoundRetrievalCacheHit ? " (cached on " +
                                centralConfig.format(latestRes.getLastModified()) + ")" : "");
                return new UnfoundRepoResource(repoPath, msg);
            }
        }

        RequestTraceLogger.log("Returning found resource from %s", latestRes.getResponseRepoPath().getId());
        return latestRes;
    }

    private boolean shouldSkipSnapshotRepo(boolean foundInLocalRepo, RealRepo repo) {
        //Skip if not handling
        if (!repo.isHandleSnapshots()) {
            RequestTraceLogger.log("%s doesn't handle snapshot resources", repo.getKey());
            return true;
        }
        //Skip remote repos if found in local repo (including caches)
        boolean exactMatchRequired = isExactMatchRequired(repo);
        RequestTraceLogger.log("Resource was found in the local repo '%s' = %s, Repo is remote and doesn't " +
                "sync properties = %s", repo.getKey(), foundInLocalRepo, exactMatchRequired);
        return foundInLocalRepo && exactMatchRequired;
    }

    private boolean isSnapshotFoundInLocalRepo(boolean foundInLocalRepo, RealRepo repo, String requestPath) {
        if (repo.isLocal()) {
            if (foundInLocalRepo) {
                //Issue a warning for a resource found multiple times in local repos
                RequestTraceLogger.log("Found multiple instances of the resource in local repositories.");
                log.warn("{}: found multiple resource instances of '{}' in local repositories.",
                        repo, requestPath);
            } else {
                RequestTraceLogger.log("Resource was found in %s", repo.getKey());
                foundInLocalRepo = true;
            }
        }
        return foundInLocalRepo;
    }


    private UnfoundRepoResource checkIfForbidden(RepoResource resource) {
        if (resource instanceof UnfoundRepoResource) {
            if (((UnfoundRepoResource) resource).getStatusCode() == HttpStatus.SC_FORBIDDEN) {
                return (UnfoundRepoResource) resource;
            }
        }
        return null;
    }

    private void updateResponseRepoPath(Repo foundInRepo, RepoResource resource) {
        resource.setResponseRepoPath(
                InternalRepoPathFactory.create(foundInRepo.getKey(), resource.getRepoPath().getPath()));
    }


    /**
     * Assembles a list of search repositories grouped by type. Virtual repositories that don't accept the input
     * repoPath pattern are not added to the list and are not recursively visited.
     */
    private void deeplyAssembleSearchRepositoryLists(
            String path, Map<String, VirtualRepo> visitedVirtualRepositories,
            Map<String, LocalRepo> searchableLocalRepositories,
            Map<String, LocalCacheRepo> searchableLocalCacheRepositories,
            Map<String, RemoteRepo> searchableRemoteRepositories) {

        if (!virtualRepo.accepts(path)) {
            // includes/excludes should not affect system paths
            RequestTraceLogger.log("Adding no aggregated repositories - requested artifact is rejected by the " +
                    "include exclude patterns of '%s'", virtualRepo.getKey());
            return;
        }
        RequestTraceLogger.log("Appending '%s'", virtualRepo.getKey());
        visitedVirtualRepositories.put(virtualRepo.getKey(), virtualRepo);

        //Add its local repositories
        RequestTraceLogger.log("Appending the local repositories of '%s'", virtualRepo.getKey());
        searchableLocalRepositories.putAll(virtualRepo.getLocalRepositoriesMap());
        //Add the caches
        RequestTraceLogger.log("Appending the local cache repositories of '%s'", virtualRepo.getKey());
        searchableLocalCacheRepositories.putAll(virtualRepo.getLocalCacheRepositoriesMap());
        //Add the remote repositories
        RequestTraceLogger.log("Appending the remote repositories repositories of '%s'", virtualRepo.getKey());
        searchableRemoteRepositories.putAll(virtualRepo.getRemoteRepositoriesMap());
        //Add any contained virtual repo
        List<VirtualRepo> childrenVirtualRepos = virtualRepo.getVirtualRepositories();

        for (VirtualRepo childVirtualRepo : childrenVirtualRepos) {
            String key = childVirtualRepo.getKey();
            if (visitedVirtualRepositories.containsKey(key)) {
                //Avoid infinite loop - stop if already processed virtual repo is encountered
                RequestTraceLogger.log("Skipping '%s' to avoid infinite loop - already processed", key);
                return;
            } else {
                String translatedPath = translateRepoPath(virtualRepo, childVirtualRepo, path);
                if (!translatedPath.equals(path)) {
                    RequestTraceLogger.log("Resource was translated to '%s' in order to search within '%s'",
                            translatedPath, key);
                }
                childVirtualRepo.downloadStrategy.deeplyAssembleSearchRepositoryLists(
                        translatedPath, visitedVirtualRepositories,
                        searchableLocalRepositories,
                        searchableLocalCacheRepositories,
                        searchableRemoteRepositories);
            }
        }
    }

    /**
     * Translates the artifact request context if the layout of the target repository is different from the source
     *
     * @param source  Source repository
     * @param target  Target repository
     * @param context Request context to translate
     * @return Translated context if needed, original if not needed or if there is insufficient info
     */
    public <S extends Repo, T extends Repo> InternalRequestContext translateRepoRequestContext(S source,
            T target, InternalRequestContext context) {
        String originalPath = context.getResourcePath();
        String translatedPath = translateRepoPath(source, target, originalPath);
        if (originalPath.equals(translatedPath)) {
            return context;
        }

        if (context instanceof NullRequestContext) {
            return new NullRequestContext(target.getRepoPath(translatedPath));
        }

        ArtifactoryRequest artifactoryRequest = ((DownloadRequestContext) context).getRequest();
        RepoPath translatedRepoPath = InternalRepoPathFactory.create(artifactoryRequest.getRepoKey(), translatedPath);
        TranslatedArtifactoryRequest translatedRequest = new TranslatedArtifactoryRequest(translatedRepoPath,
                artifactoryRequest);
        return new DownloadRequestContext(translatedRequest);
    }

    /**
     * Translates the artifact path if the layout of the target repository is different from the source
     *
     * @param source Source repository
     * @param target Target repository
     * @param path   Path to translate
     * @return Translated path if needed, original if not needed of if there is insufficient info
     */
    private <S extends Repo, T extends Repo> String translateRepoPath(S source, T target, String path) {
        RepoLayout sourceRepoLayout = source.getDescriptor().getRepoLayout();
        RepoLayout targetRepoLayout = target.getDescriptor().getRepoLayout();

        return layoutsCoreAddon.translateArtifactPath(sourceRepoLayout, targetRepoLayout, path);
    }

    private boolean isExactMatchRequired(RealRepo repo) {
        return !repo.isLocal() && !((RemoteRepoDescriptor) repo.getDescriptor()).isSynchronizeProperties();
    }
}
