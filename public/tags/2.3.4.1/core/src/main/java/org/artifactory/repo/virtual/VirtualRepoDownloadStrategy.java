/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2011 JFrog Ltd.
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
import org.apache.commons.io.IOUtils;
import org.apache.maven.artifact.repository.metadata.Metadata;
import org.artifactory.api.config.CentralConfigService;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.api.fs.MetadataInfoImpl;
import org.artifactory.api.fs.RepoResource;
import org.artifactory.api.maven.MavenNaming;
import org.artifactory.api.mime.NamingUtils;
import org.artifactory.api.module.ModuleInfo;
import org.artifactory.api.module.ModuleInfoUtils;
import org.artifactory.api.repo.RepoPathImpl;
import org.artifactory.api.repo.exception.FileExpectedException;
import org.artifactory.api.repo.exception.RepoRejectException;
import org.artifactory.api.request.TranslatedArtifactoryRequest;
import org.artifactory.descriptor.repo.RemoteRepoDescriptor;
import org.artifactory.descriptor.repo.RepoLayout;
import org.artifactory.jcr.lock.LockingHelper;
import org.artifactory.log.LoggerFactory;
import org.artifactory.maven.MavenModelUtils;
import org.artifactory.md.MetadataInfo;
import org.artifactory.repo.LocalCacheRepo;
import org.artifactory.repo.LocalRepo;
import org.artifactory.repo.RealRepo;
import org.artifactory.repo.RemoteRepo;
import org.artifactory.repo.Repo;
import org.artifactory.repo.RepoPath;
import org.artifactory.repo.service.InternalRepositoryService;
import org.artifactory.request.ArtifactoryRequest;
import org.artifactory.request.DownloadRequestContext;
import org.artifactory.request.NullRequestContext;
import org.artifactory.request.RequestContext;
import org.artifactory.resource.MetadataResource;
import org.artifactory.resource.ResolvedResource;
import org.artifactory.resource.ResourceStreamHandle;
import org.artifactory.resource.UnfoundRepoResource;
import org.slf4j.Logger;

import javax.jcr.RepositoryException;
import java.io.IOException;
import java.io.InputStream;
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

    public VirtualRepoDownloadStrategy(VirtualRepo virtualRepo) {
        this.virtualRepo = virtualRepo;
        centralConfig = ContextHelper.get().getCentralConfig();
        repositoryService = ContextHelper.get().beanForType(InternalRepositoryService.class);
    }

    public RepoResource getInfo(RequestContext context) {
        log.debug("Request processing done on virtual repo '{}'.", virtualRepo);
        // first look in local storage
        RepoResource cachedResource = getInfoFromLocalStorage(context);
        String path = context.getResourcePath();
        if (MavenNaming.isIndex(path)) {
            // for index files just return the result from the cache (we don't want to process it or to return index
            // from other repositories)
            return cachedResource;
        }

        // release the read lock on the virtual repo local cache to prevent deadlock in any of the interceptors
        // (in case one of them needs to write back to the virtual repo cache)
        RepoPath localCacheRepoPath = new RepoPathImpl(virtualRepo.getKey(), context.getResourcePath());
        LockingHelper.releaseReadLock(localCacheRepoPath);

        // not found in local virtual repository storage, look in configured repositories
        RepoResource searchableResource = getInfoFromSearchableRepositories(context);
        if (!cachedResource.isFound() && !searchableResource.isFound()) {
            // not found
            return searchableResource;
        } else if (cachedResource.isFound() && !searchableResource.isFound()) {
            // delete the local cached artifact and return the not found resource
            virtualRepo.undeploy(localCacheRepoPath, false);
            return searchableResource;
        } else if (cachedResource.isFound() && searchableResource.isFound()) {
            String sourceRepoKey = searchableResource.getResponseRepoPath().getRepoKey();
            // if the searchable resource is a remote repo - it is not downloaded yet, so we shouldn't
            // return the cached artifact
            if (repositoryService.remoteRepositoryByKey(sourceRepoKey) == null &&
                    cachedResource.getLastModified() >= searchableResource.getLastModified()) {
                // locally stored resource is latest, update the stats of the original resource and return our cached file
                ResourceStreamHandle searchableHandle = null;
                try {
                    // strangely enough, to update the stats we must get the resource stream handle which will also
                    // call update stats (see RTFACT-2958)
                    LocalRepo repo = repositoryService.localOrCachedRepositoryByKey(sourceRepoKey);
                    searchableHandle = repositoryService.getResourceStreamHandle(context, repo, searchableResource);
                } catch (IOException ioe) {
                    log.error("Could not update download stats", ioe);
                } catch (RepoRejectException rre) {
                    log.error("Could not update download stats", rre);
                } catch (RepositoryException re) {
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
        return virtualRepo.interceptBeforeReturn(context, searchableResource);
    }

    public RepoResource getInfoFromLocalStorage(RequestContext context) {
        try {
            return virtualRepo.storageMixin.getInfo(context);
        } catch (FileExpectedException e) {
            // see RTFACT-3721
            return new UnfoundRepoResource(new RepoPathImpl(virtualRepo.getKey(), context.getResourcePath()),
                    "File expected but the cache already points to directory");
        }
    }

    public RepoResource getInfoFromSearchableRepositories(RequestContext context) {
        String path = context.getResourcePath();
        RepoPath repoPath = new RepoPathImpl(virtualRepo.getKey(), path);
        RepoResource result;
        try {
            List<RealRepo> repositories = assembleSearchRepositoriesList(repoPath, context);
            if (repositories.isEmpty()) {
                return new UnfoundRepoResource(repoPath, "No repository found to serve the request for " + repoPath);
            }

            ModuleInfo artifactModuleInfo = virtualRepo.getItemModuleInfo(path);

            /**
             * The repo might not define a layout, so also check specifically for a Maven snapshot if the resulting
             * module info is invalid
             */
            if (MavenNaming.isMavenMetadata(path)) {
                result = processMavenMetadata(context, repoPath, repositories);
            } else if (artifactModuleInfo.isIntegration() ||
                    (!artifactModuleInfo.isValid() && MavenNaming.isSnapshot(path))) {
                result = processSnapshot(context, repoPath, repositories);
            } else {
                result = processStandard(context, repoPath, repositories);
            }
            return result;
        } catch (IOException e) {
            log.error("Failed processing get resource info", e);
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
    List<RealRepo> assembleSearchRepositoriesList(RepoPath repoPath, RequestContext context) {
        Map<String, LocalRepo> searchableLocalRepositories = Maps.newLinkedHashMap();
        Map<String, LocalCacheRepo> searchableLocalCacheRepositories = Maps.newLinkedHashMap();
        Map<String, RemoteRepo> searchableRemoteRepositories = Maps.newLinkedHashMap();
        deeplyAssembleSearchRepositoryLists(repoPath.getPath(), Maps.<String, VirtualRepo>newLinkedHashMap(),
                searchableLocalRepositories, searchableLocalCacheRepositories, searchableRemoteRepositories);

        //Add all local repositories
        List<RealRepo> repositories = Lists.newArrayList();
        repositories.addAll(searchableLocalRepositories.values());
        //Add all caches
        repositories.addAll(searchableLocalCacheRepositories.values());

        //Add all remote repositories conditionally
        boolean fromAnotherArtifactory = context.isFromAnotherArtifactory();
        boolean artifactoryRequestsCanRetrieveRemoteArtifacts =
                virtualRepo.isArtifactoryRequestsCanRetrieveRemoteArtifacts();
        if (fromAnotherArtifactory && !artifactoryRequestsCanRetrieveRemoteArtifacts) {
            //If the request comes from another artifactory don't bother checking any remote repos
            log.debug("Skipping remote repository checks for path '{}'", repoPath);
        } else {
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
    private RepoResource processStandard(RequestContext context, RepoPath repoPath, List<RealRepo> repositories)
            throws IOException {
        // save forbidden unfound response
        UnfoundRepoResource forbidden = null;
        //Locate the resource matching the request
        RepoResource closestMatch = null;

        for (RealRepo repo : repositories) {
            // Since we are in process standard, repositories that does not process releases should be skipped.
            // Now, checksums are always considered standard, even if executed against a snapshot repository.
            // So, we should not skip snapshots repositories for checksums.
            String path = repoPath.getPath();
            if (!repo.isHandleReleases() && !NamingUtils.isChecksum(path)) {
                log.debug("Repo '{}' not handling releases - skipping '{}'.", repo.getKey(), path);
                continue;
            }

            if (closestMatch != null && isExactMatchRequired(repo)) {
                continue;
            }

            RequestContext translatedContext = translateRepoRequestContext(virtualRepo, repo, context);

            RepoResource res = repo.getInfo(translatedContext);
            // release all read locks acquired by the repo during the getInfo
            LockingHelper.getSessionLockManager().unlockAllReadLocks(repo.getKey());
            if (res.isFound()) {
                log.debug("Found resource '{}' found in repo '{}'.", path, repo.getKey());
                updateResponseRepoPath(repo, res);
                if (res.isExactQueryMatch()) {
                    //return the exact match
                    return res;
                } else {
                    closestMatch = res;
                }
            } else if (forbidden == null) {
                forbidden = checkIfForbidden(res);
                if (log.isDebugEnabled() && forbidden != null) {
                    log.debug("Resource '{}' forbidden by repo '{}'.", repoPath, repo.getKey());
                }
            }
        }

        //If we didn't find an exact match return the first found resource (closest match)
        if (closestMatch != null) {
            return closestMatch;
        }

        // not found in any repo
        if (forbidden != null) {
            return new UnfoundRepoResource(repoPath, forbidden.getReason(), forbidden.getStatusCode());
        } else {
            return new UnfoundRepoResource(repoPath, "Could not find resource");
        }
    }

    /**
     * Iterate over the repos and return the latest resource found (content or just head information) on the response.
     */
    private RepoResource processSnapshot(RequestContext context, RepoPath repoPath, List<RealRepo> repositories)
            throws IOException {

        //Find the latest in all repositories
        RepoResource latestRes = null;
        // save forbidden unfound response
        UnfoundRepoResource forbidden = null;
        //Traverse the local, caches and remote repositories and search for the newest snapshot
        //Make sure local repos are always searched first
        boolean foundInLocalRepo = false;

        for (RealRepo repo : repositories) {
            if (shouldSkipSnapshotRepo(foundInLocalRepo, repo)) {
                continue;
            }
            RequestContext translatedContext = translateRepoRequestContext(virtualRepo, repo, context);
            String translatedPath = translatedContext.getResourcePath();

            final RepoResource res = repo.getInfo(translatedContext);
            if (res.isFound()) {
                foundInLocalRepo = isSnapshotFoundInLocalRepo(foundInLocalRepo, repo, translatedPath);
                if (log.isDebugEnabled()) {
                    log.debug("{} last modified {}", res.getRepoPath(), centralConfig.format(res.getLastModified()));
                }

                //If we haven't found one yet
                if (latestRes == null ||
                        //or this one is a better match
                        (!latestRes.isExactQueryMatch() && res.isExactQueryMatch())
                        //or newer than the one found
                        || res.getLastModified() > latestRes.getLastModified()) {
                    log.debug("{}: found newer res: {}", repo, translatedPath);
                    //take it
                    updateResponseRepoPath(repo, res);
                    latestRes = res;
                }
            } else if (forbidden == null) {
                forbidden = checkIfForbidden(res);
            }
        }

        String resourcePath = repoPath.getPath();
        boolean nonFoundRetrievalCacheHit = latestRes != null && !latestRes.isFound();
        if (latestRes == null || nonFoundRetrievalCacheHit) {
            if (forbidden != null) {
                return new UnfoundRepoResource(repoPath, forbidden.getReason(), forbidden.getStatusCode());
            } else {
                String msg = "Artifact not found: " + resourcePath +
                        (nonFoundRetrievalCacheHit ? " (cached on " +
                                centralConfig.format(latestRes.getLastModified()) + ")" : "");
                return new UnfoundRepoResource(repoPath, msg);
            }
        }
        //Found a newer version
        log.debug("{}: Found the latest version of {}", latestRes.getResponseRepoPath().getRepoKey(), resourcePath);
        return latestRes;
    }

    private boolean shouldSkipSnapshotRepo(boolean foundInLocalRepo, RealRepo repo) {
        //Skip if not handling
        if (!repo.isHandleSnapshots()) {
            return true;
        }
        //Skip remote repos if found in local repo (including caches)
        return foundInLocalRepo && !repo.isLocal() && isExactMatchRequired(repo);
    }

    private boolean isSnapshotFoundInLocalRepo(boolean foundInLocalRepo, RealRepo repo, String requestPath) {
        if (repo.isLocal()) {
            if (foundInLocalRepo) {
                //Issue a warning for a resource found multiple times in local repos
                log.warn("{}: found multiple resource instances of '{}' in local repositories.",
                        repo, requestPath);
            } else {
                log.debug("{}: found local res: {}", repo, requestPath);
                foundInLocalRepo = true;
            }
        }
        return foundInLocalRepo;
    }

    /**
     * Merges maven metadata from all the repositories that are part of this virtual repository.
     *
     * @param context      Request context
     * @param repoPath     The repository path pointing to a maven-metadata.xml
     * @param repositories List of repositories to search   @return A StringResource with the metadata content or an
     *                     un-found resource.
     */
    private RepoResource processMavenMetadata(RequestContext context, RepoPath repoPath, List<RealRepo> repositories)
            throws IOException {

        MergableMavenMetadata mergedMavenMetadata = new MergableMavenMetadata();
        // save forbidden unfound response
        UnfoundRepoResource forbidden = null;

        for (RealRepo repo : repositories) {
            if (repo.isCache()) {
                //  Skip cache repos - we search in remote repos directly which will handle the cache retrieval
                // and expiry
                continue;
            }
            RequestContext translatedContext = translateRepoRequestContext(virtualRepo, repo, context);

            RepoResource res = repo.getInfo(translatedContext);
            if (!res.isFound()) {
                if (forbidden == null) {
                    forbidden = checkIfForbidden(res);
                }
                continue;
            }
            findAndMergeMavenMetadata(mergedMavenMetadata, repo, translatedContext, res);
        }   // end repositories iteration

        String path = repoPath.getPath();
        if (mergedMavenMetadata.getMetadata() == null) {
            if (forbidden != null) {
                return new UnfoundRepoResource(repoPath, forbidden.getReason(), forbidden.getStatusCode());
            } else {
                return new UnfoundRepoResource(repoPath, "Maven metadata not found for '" + path + "'.");
            }
        } else {
            log.debug("Maven artifact metadata found for '{}'.", path);
            return createMavenMetadataFoundResource(repoPath, mergedMavenMetadata);
        }
    }

    /**
     * Check for maven metadata on the given resource and merge it
     *
     * @param mergedMavenMetadata Collected maven metadata
     * @param repo                Repo to check for metadata
     * @param context             Request context
     * @param res                 Resource info
     */
    private void findAndMergeMavenMetadata(MergableMavenMetadata mergedMavenMetadata, RealRepo repo,
            RequestContext context, RepoResource res) {
        String resourcePath = context.getResourcePath();
        Metadata metadata = getMavenMetadataContent(context, repo, res);
        if (metadata != null) {
            if (log.isDebugEnabled()) {
                log.debug("{}: found maven metadata res: {}", repo, resourcePath);
                log.debug("{}: last modified {}", res.getRepoPath(), centralConfig.format(res.getLastModified()));
            }
            mergedMavenMetadata.merge(metadata, res);
        }
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
        resource.setResponseRepoPath(new RepoPathImpl(foundInRepo.getKey(), resource.getRepoPath().getPath()));
    }

    private Metadata getMavenMetadataContent(RequestContext requestContext, Repo repo, RepoResource res) {
        ResourceStreamHandle handle = null;
        try {
            handle = repositoryService.getResourceStreamHandle(requestContext, repo, res);
            //Create metadata
            InputStream metadataInputStream;
            //Hold on to the original metadata string since regenerating it could result in
            //minor differences from the original, which will cause checksum errors
            metadataInputStream = handle.getInputStream();
            String metadataContent = IOUtils.toString(metadataInputStream, "utf-8");
            return MavenModelUtils.toMavenMetadata(metadataContent);
        } catch (RepoRejectException rre) {
            log.warn("Metadata retrieval failed on repo '{}': {}", repo, rre.getMessage());
        } catch (IOException ioe) {
            log.error("IO exception retrieving maven metadata content from repo '{}': {}.", repo, ioe.getMessage());
        } catch (RepositoryException re) {
            log.error("Metadata retrieval failed on repo '{}': {}", repo, re.getMessage());
        } finally {
            if (handle != null) {
                handle.close();
            }
        }
        return null;
    }

    private RepoResource createMavenMetadataFoundResource(RepoPath mavenMetadataRepoPath,
            MergableMavenMetadata mergedMavenMetadata) throws IOException {
        String metadataContent = MavenModelUtils.mavenMetadataToString(mergedMavenMetadata.getMetadata());
        MetadataInfo metadataInfo = new MetadataInfoImpl(mavenMetadataRepoPath);
        metadataInfo.setLastModified(mergedMavenMetadata.getLastModified());
        metadataInfo.setSize(metadataContent.length());
        return new ResolvedResource(new MetadataResource(metadataInfo), metadataContent);
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
            log.debug("The repository '{}' rejected the artifact '{}' due to its include/exclude pattern settings.",
                    virtualRepo, path);
            return;
        }
        visitedVirtualRepositories.put(virtualRepo.getKey(), virtualRepo);

        //Add its local repositories
        searchableLocalRepositories.putAll(virtualRepo.getLocalRepositoriesMap());
        //Add the caches
        searchableLocalCacheRepositories.putAll(virtualRepo.getLocalCacheRepositoriesMap());
        //Add the remote repositories
        searchableRemoteRepositories.putAll(virtualRepo.getRemoteRepositoriesMap());
        //Add any contained virtual repo
        List<VirtualRepo> childrenVirtualRepos = virtualRepo.getVirtualRepositories();

        for (VirtualRepo childVirtualRepo : childrenVirtualRepos) {
            String key = childVirtualRepo.getKey();
            if (visitedVirtualRepositories.containsKey(key)) {
                //Avoid infinite loop - stop if already processed virtual repo is encountered
                log.debug("Repositories list assembly has been truncated to avoid recursive loop " +
                        "on the virtual repo '{}'. Already processed virtual repositories: {}.",
                        key, visitedVirtualRepositories.keySet());
                return;
            } else {
                String translatedPath = translateRepoPath(virtualRepo, childVirtualRepo, path);
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
    public <S extends Repo, T extends Repo> RequestContext translateRepoRequestContext(S source,
            T target, RequestContext context) {
        String originalPath = context.getResourcePath();
        String translatedPath = translateRepoPath(source, target, originalPath);
        if (originalPath.equals(translatedPath)) {
            return context;
        }

        if (context instanceof NullRequestContext) {
            return new NullRequestContext(target.getRepoPath(translatedPath));
        }

        ArtifactoryRequest artifactoryRequest = ((DownloadRequestContext) context).getRequest();
        RepoPathImpl translatedRepoPath = new RepoPathImpl(artifactoryRequest.getRepoKey(), translatedPath);
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
    public <S extends Repo, T extends Repo> String translateRepoPath(S source, T target, String path) {
        RepoLayout sourceRepoLayout = source.getDescriptor().getRepoLayout();
        RepoLayout targetRepoLayout = target.getDescriptor().getRepoLayout();

        return ModuleInfoUtils.translateArtifactPath(sourceRepoLayout, targetRepoLayout, path);
    }

    private boolean isExactMatchRequired(RealRepo repo) {
        return !repo.isLocal() && !((RemoteRepoDescriptor) repo.getDescriptor()).isSynchronizeProperties();
    }
}
