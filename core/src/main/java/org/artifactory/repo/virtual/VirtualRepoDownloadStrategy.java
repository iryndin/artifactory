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

package org.artifactory.repo.virtual;

import org.apache.commons.collections15.OrderedMap;
import org.apache.commons.collections15.map.ListOrderedMap;
import org.apache.commons.io.IOUtils;
import org.apache.maven.artifact.repository.metadata.Metadata;
import org.artifactory.api.config.CentralConfigService;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.api.fs.ChecksumInfo;
import org.artifactory.api.fs.MetadataInfo;
import org.artifactory.api.maven.MavenNaming;
import org.artifactory.api.mime.ChecksumType;
import org.artifactory.api.mime.NamingUtils;
import org.artifactory.api.repo.RepoPath;
import org.artifactory.api.repo.exception.RepoAccessException;
import org.artifactory.common.ResourceStreamHandle;
import org.artifactory.io.checksum.Checksum;
import org.artifactory.io.checksum.ChecksumCalculator;
import org.artifactory.jcr.lock.LockingHelper;
import org.artifactory.log.LoggerFactory;
import org.artifactory.maven.MavenModelUtils;
import org.artifactory.repo.LocalCacheRepo;
import org.artifactory.repo.LocalRepo;
import org.artifactory.repo.RealRepo;
import org.artifactory.repo.RemoteRepo;
import org.artifactory.repo.Repo;
import org.artifactory.repo.context.NullRequestContext;
import org.artifactory.repo.context.RequestContext;
import org.artifactory.repo.service.InternalRepositoryService;
import org.artifactory.resource.RepoResource;
import org.artifactory.resource.StringResource;
import org.artifactory.resource.UnfoundRepoResource;
import org.slf4j.Logger;

import javax.jcr.RepositoryException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
        RepoPath localCacheRepoPath = new RepoPath(virtualRepo.getKey(), context.getResourcePath());
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
                    searchableHandle = repositoryService.getResourceStreamHandle(repo, searchableResource);
                } catch (IOException e) {
                    log.error("Could not update download stats", e);
                } catch (RepoAccessException e) {
                    log.error("Could not update download stats", e);
                } catch (RepositoryException e) {
                    log.error("Could not update download stats", e);
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
        return virtualRepo.storageMixin.getInfo(context);
    }

    public RepoResource getInfoFromSearchableRepositories(RequestContext context) {
        String path = context.getResourcePath();
        RepoPath repoPath = new RepoPath(virtualRepo.getKey(), path);
        RepoResource result;
        try {
            List<RealRepo> repositories = assembleSearchRepositoriesList(repoPath, context);
            if (repositories.isEmpty()) {
                return new UnfoundRepoResource(repoPath, "No repository found to serve the request for " + repoPath);
            }

            if (MavenNaming.isMavenMetadata(path)) {
                result = processMavenMetadata(repoPath, repositories);
            } else if (MavenNaming.isSnapshot(path)) {
                result = processSnapshot(repoPath, repositories);
            } else {
                result = processStandard(repoPath, repositories);
            }
            return result;
        } catch (IOException e) {
            log.error("Failed processing get resource info", e);
            result = new UnfoundRepoResource(repoPath, "IOException: " + e.getMessage());
        }
        return result;
    }

    /**
     * Iterate over a list of repos until a resource is found in one of them, and return that resource. The first
     * resource that is found is returned. The order of searching is: local repos, cache repos and remote repos (unless
     * request originated from another Artifactory).
     *
     * @param repoPath     The repository path of the resource to find
     * @param repositories List of repositories to look in (doesn't include virtual repos)
     */
    private RepoResource processStandard(RepoPath repoPath, List<RealRepo> repositories) throws IOException {
        //For metadata checksums, first check the merged metadata cache
        String path = repoPath.getPath();
        //Locate the resource matching the request
        RepoResource closetMatch = null;
        for (RealRepo repo : repositories) {
            // Since we are in process standard, repositories that does not process releases should be skipped.
            // Now, checksums are always considered standard, even if executed against a snapshot repository.
            // So, we should not skip snapshots repositories for checksums.
            if (!repo.isHandleReleases() && !NamingUtils.isChecksum(path)) {
                continue;
            }
            RepoResource res = repo.getInfo(new NullRequestContext(path));
            // release all read locks acquired by the repo during the getInfo
            LockingHelper.getSessionLockManager().unlockAllReadLocks(repo.getKey());
            if (res.isFound()) {
                updateResponseRepoPath(repo, res);
                if (res.isExactQueryMatch()) {
                    //return the exact match
                    return res;
                } else {
                    closetMatch = res;
                }
            }
        }

        //If we didn't find an exact match return the first found resource (closest match)
        if (closetMatch != null) {
            return closetMatch;
        }

        // not found in any repo
        return new UnfoundRepoResource(repoPath, "Could not found resource");
    }

    /**
     * Iterate over the repos and return the latest resource found (content or just head information) on the response.
     */
    private RepoResource processSnapshot(RepoPath repoPath, List<RealRepo> repositories) throws IOException {
        String resourcePath = repoPath.getPath();
        //Find the latest in all repositories
        RepoResource latestRes = null;
        //Traverse the local, caches and remote repositories and search for the newest snapshot
        //Make sure local repos are always searched first
        boolean foundInLocalRepo = false;
        for (RealRepo repo : repositories) {
            //Skip if not handling
            if (!repo.isHandleSnapshots()) {
                continue;
            }
            //Skip remote repos if found in local repo (including caches)
            if (foundInLocalRepo && !repo.isLocal()) {
                continue;
            }

            final RepoResource res = repo.getInfo(new NullRequestContext(resourcePath));
            if (res.isFound()) {
                if (repo.isLocal()) {
                    if (foundInLocalRepo) {
                        //Issue a warning for a resource found multiple times in local repos
                        log.warn(repo + ": found multiple resource instances of '" + resourcePath +
                                "' in local repositories.");
                    } else {
                        log.debug("{}: found local res: {}", repo, resourcePath);
                        foundInLocalRepo = true;
                    }
                }
                if (log.isDebugEnabled()) {
                    log.debug("{} last modified {}", res.getRepoPath(), centralConfig.format(res.getLastModified()));
                }

                //If we haven't found one yet
                if (latestRes == null ||
                        //or this one is a better match
                        (!latestRes.isExactQueryMatch() && res.isExactQueryMatch())
                        //or newer than the one found
                        || res.getLastModified() > latestRes.getLastModified()) {
                    log.debug("{}: found newer res: {}", repo, resourcePath);
                    //take it
                    updateResponseRepoPath(repo, res);
                    latestRes = res;
                }
            }
        }

        boolean nonFoundRetrievalCacheHit = latestRes != null && !latestRes.isFound();
        if (latestRes == null || nonFoundRetrievalCacheHit) {
            String msg = "Artifact not found: " + resourcePath +
                    (nonFoundRetrievalCacheHit ? " (cached on " +
                            centralConfig.format(latestRes.getLastModified()) + ")" : "");
            return new UnfoundRepoResource(repoPath, msg);
        }
        //Found a newer version
        log.debug("{}: Found the latest version of {}", latestRes.getResponseRepoPath().getRepoKey(), resourcePath);
        return latestRes;
    }

    /**
     * @param repoPath     The repository path pointing to a maven-metadata.xml
     * @param repositories List of repositories to search
     * @return A StringResource with the metadata content or an unfound resource.
     */
    private RepoResource processMavenMetadata(RepoPath repoPath, List<RealRepo> repositories) throws IOException {
        String path = repoPath.getPath();
        boolean foundInLocalRepo = false;
        MergedMavenMetadata mergedMavenMetadata = new MergedMavenMetadata();
        for (RealRepo repo : repositories) {
            //Skip remote repos if found in local repo (including caches)
            if (foundInLocalRepo && !repo.isLocal()) {
                continue;
            }

            // get the resource
            final RepoResource res = repo.getInfo(new NullRequestContext(path));
            if (!res.isFound()) {
                continue;
            }

            Metadata metadata = getMavenMetadataContent(repo, res);
            if (metadata != null) {
                mergedMavenMetadata.merge(metadata, res);
                if (log.isDebugEnabled()) {
                    log.debug("{}: found maven metadata res: {}", repo, path);
                    log.debug(res.getRepoPath() + " last modified " + centralConfig.format(res.getLastModified()));
                }
                if (repo.isLocal()) {
                    foundInLocalRepo = true;
                }
            }
        }   // end repositories iteration

        if (mergedMavenMetadata.getMetadata() == null) {
            String msg = "Maven metadata not found for '" + path + "'.";
            return new UnfoundRepoResource(repoPath, msg);
        } else {
            log.debug("Maven artifact metadata found for '{}'.", path);
            return createMavenMetadataFoundResource(repoPath, mergedMavenMetadata);
        }
    }

    private void updateResponseRepoPath(Repo foundInRepo, RepoResource resource) {
        resource.setResponseRepoPath(new RepoPath(foundInRepo.getKey(), resource.getRepoPath().getPath()));
    }

    private Metadata getMavenMetadataContent(Repo repo, RepoResource res) {
        ResourceStreamHandle handle = null;
        try {
            handle = repositoryService.getResourceStreamHandle(repo, res);
            //Create metadata
            InputStream metadataInputStream;
            //Hold on to the original metatdata string since regenerating it could result in
            //minor differences from the original, which will cause checksum errors
            metadataInputStream = handle.getInputStream();
            String metadataContent = IOUtils.toString(metadataInputStream, "utf-8");
            return MavenModelUtils.toMavenMetadata(metadataContent);
        } catch (RepoAccessException e) {
            log.warn("Metadata retrieval failed on repo '{}': {}", repo, e.getMessage());
        } catch (IOException ioe) {
            log.error("IO exception retrieving maven metadata content from repo '{}'", repo, ioe);
        } catch (RepositoryException e) {
            log.error("Metadata retrieval failed on repo '{}': {}", repo, e);
        } finally {
            if (handle != null) {
                handle.close();
            }
        }
        return null;
    }

    private RepoResource createMavenMetadataFoundResource(RepoPath mavenMetadataRepoPath,
            MergedMavenMetadata mergedMavenMetadata) throws IOException {
        String metadataContent = MavenModelUtils.mavenMetadataToString(mergedMavenMetadata.getMetadata());
        MetadataInfo metadataInfo = new MetadataInfo(mavenMetadataRepoPath);
        metadataInfo.setLastModified(mergedMavenMetadata.getLastModified());
        metadataInfo.setSize(metadataContent.length());
        ByteArrayInputStream bais = new ByteArrayInputStream(metadataContent.getBytes("utf-8"));
        Checksum checksum = ChecksumCalculator.calculate(bais, ChecksumType.sha1);
        String sha1 = checksum.getChecksum();
        Set<ChecksumInfo> checksumInfos = new HashSet<ChecksumInfo>(1);
        ChecksumInfo sha1Info = new ChecksumInfo(ChecksumType.sha1, sha1, sha1);
        checksumInfos.add(sha1Info);
        metadataInfo.setChecksums(checksumInfos);
        return new StringResource(metadataInfo, metadataContent);
    }

    /**
     * @return A list of local and remote repositories to search the resource in, ordered first by type (local non-cache
     *         first) and secondly by order of appearance. We don't simply add all the lists since some of the real
     *         repos are resolved transitively and we might have to remove them in case the path is excluded in the
     *         virtual repo they belong to.
     */
    private List<RealRepo> assembleSearchRepositoriesList(RepoPath repoPath, RequestContext context) {
        OrderedMap<String, LocalRepo> searchableLocalRepositories = newOrderedMap();
        OrderedMap<String, LocalCacheRepo> searchableLocalCacheRepositories = newOrderedMap();
        OrderedMap<String, RemoteRepo> searchableRemoteRepositories = newOrderedMap();
        deeplyAssembleSearchRepositoryLists(repoPath,
                new ListOrderedMap<String, VirtualRepo>(),
                searchableLocalRepositories,
                searchableLocalCacheRepositories,
                searchableRemoteRepositories);

        //Add all local repositories
        List<RealRepo> repositories = new ArrayList<RealRepo>();
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
     * Assembles a list of search repositories grouped by type. Virtual repositories that don't accept the input
     * repoPath pattern are not added to the list and are not recursively visited.
     */
    private void deeplyAssembleSearchRepositoryLists(
            RepoPath repoPath, OrderedMap<String, VirtualRepo> visitedVirtualRepositories,
            OrderedMap<String, LocalRepo> searchableLocalRepositories,
            OrderedMap<String, LocalCacheRepo> searchableLocalCacheRepositories,
            OrderedMap<String, RemoteRepo> searchableRemoteRepositories) {

        if (!virtualRepo.accepts(repoPath)) {
            // includes/excludes should not affect system paths
            log.debug("The repository '{}' rejected the artifact '{}' due to its include/exclude pattern settings.",
                    virtualRepo, repoPath);
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
                log.warn("Repositories list assembly has been truncated to avoid recursive loop " +
                        "on the virtual repo '{}'. Already processed virtual repositories: {}.",
                        key, visitedVirtualRepositories.keySet());
                return;
            } else {
                childVirtualRepo.downloadStrategy.deeplyAssembleSearchRepositoryLists(
                        repoPath, visitedVirtualRepositories,
                        searchableLocalRepositories,
                        searchableLocalCacheRepositories,
                        searchableRemoteRepositories);
            }
        }
    }

    private <K, V> OrderedMap<K, V> newOrderedMap() {
        return new ListOrderedMap<K, V>();
    }

    private static class MergedMavenMetadata {
        private Metadata metadata;
        private long lastModified;

        public Metadata getMetadata() {
            return metadata;
        }

        public long getLastModified() {
            return lastModified;
        }

        public void merge(Metadata otherMetedata, RepoResource foundResource) {
            long otherLastModified = foundResource.getLastModified();
            if (metadata == null) {
                metadata = otherMetedata;
                lastModified = otherLastModified;
            } else {
                metadata.merge(otherMetedata);
                lastModified = Math.max(otherLastModified, lastModified);
            }
        }
    }
}
