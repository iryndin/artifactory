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

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.io.IOUtils;
import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.artifact.repository.metadata.Snapshot;
import org.apache.maven.artifact.repository.metadata.Versioning;
import org.artifactory.api.config.CentralConfigService;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.api.fs.MetadataInfoImpl;
import org.artifactory.api.fs.RepoResource;
import org.artifactory.api.maven.MavenNaming;
import org.artifactory.api.mime.NamingUtils;
import org.artifactory.api.repo.RepoPathImpl;
import org.artifactory.api.repo.exception.FileExpectedException;
import org.artifactory.api.repo.exception.RepoRejectException;
import org.artifactory.checksum.ChecksumInfo;
import org.artifactory.checksum.ChecksumType;
import org.artifactory.io.checksum.Checksum;
import org.artifactory.io.checksum.ChecksumCalculator;
import org.artifactory.jcr.lock.LockingHelper;
import org.artifactory.log.LoggerFactory;
import org.artifactory.maven.MavenModelUtils;
import org.artifactory.maven.versioning.MavenVersionComparator;
import org.artifactory.md.MetadataInfo;
import org.artifactory.repo.LocalCacheRepo;
import org.artifactory.repo.LocalRepo;
import org.artifactory.repo.RealRepo;
import org.artifactory.repo.RemoteRepo;
import org.artifactory.repo.Repo;
import org.artifactory.repo.RepoPath;
import org.artifactory.repo.service.InternalRepositoryService;
import org.artifactory.request.RequestContext;
import org.artifactory.resource.ResourceStreamHandle;
import org.artifactory.resource.StringResource;
import org.artifactory.resource.UnfoundRepoResource;
import org.artifactory.util.Utils;
import org.slf4j.Logger;

import javax.jcr.RepositoryException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
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

            if (MavenNaming.isMavenMetadata(path)) {
                result = processMavenMetadata(context, repoPath, repositories);
            } else if (MavenNaming.isSnapshot(path)) {
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
        deeplyAssembleSearchRepositoryLists(repoPath, Maps.<String, VirtualRepo>newLinkedHashMap(),
                searchableLocalRepositories, searchableLocalCacheRepositories, searchableRemoteRepositories);

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
        //For metadata checksums, first check the merged metadata cache
        String path = repoPath.getPath();
        // save forbidden unfound response
        UnfoundRepoResource forbidden = null;
        //Locate the resource matching the request
        RepoResource closetMatch = null;
        for (RealRepo repo : repositories) {
            // Since we are in process standard, repositories that does not process releases should be skipped.
            // Now, checksums are always considered standard, even if executed against a snapshot repository.
            // So, we should not skip snapshots repositories for checksums.
            if (!repo.isHandleReleases() && !NamingUtils.isChecksum(path)) {
                continue;
            }
            RepoResource res = repo.getInfo(context);
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
            } else {
                forbidden = checkIfForbidden(res);
            }
        }

        //If we didn't find an exact match return the first found resource (closest match)
        if (closetMatch != null) {
            return closetMatch;
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
        String resourcePath = repoPath.getPath();
        //Find the latest in all repositories
        RepoResource latestRes = null;
        // save forbidden unfound response
        UnfoundRepoResource forbidden = null;
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

            final RepoResource res = repo.getInfo(context);
            if (res.isFound()) {
                if (repo.isLocal()) {
                    if (foundInLocalRepo) {
                        //Issue a warning for a resource found multiple times in local repos
                        log.warn("{}: found multiple resource instances of '{}' in local repositories.",
                                repo, resourcePath);
                    } else {
                        log.debug("{}: found local res: {}", repo, resourcePath);
                        foundInLocalRepo = true;
                    }
                }
                if (log.isDebugEnabled()) {
                    log.debug("{} last modified {}", res.getRepoPath(),
                            centralConfig.format(res.getLastModified()));
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
            } else {
                forbidden = checkIfForbidden(res);
            }
        }

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
        String path = repoPath.getPath();
        MergedMavenMetadata mergedMavenMetadata = new MergedMavenMetadata();
        // save forbidden unfound response
        UnfoundRepoResource forbidden = null;
        for (RealRepo repo : repositories) {
            if (repo.isCache()) {
                //  Skip cache repos - we search in remote repos directly which will handle the cache retrieval
                // and expiry
                continue;
            }

            RepoResource res = repo.getInfo(context);
            if (!res.isFound()) {
                forbidden = checkIfForbidden(res);
                continue;
            }

            Metadata metadata = getMavenMetadataContent(context, repo, res);
            if (metadata != null) {
                if (log.isDebugEnabled()) {
                    log.debug("{}: found maven metadata res: {}", repo, path);
                    log.debug("{}: last modified {}", res.getRepoPath(), centralConfig.format(res.getLastModified()));
                }
                mergedMavenMetadata.merge(metadata, res);
            }
        }   // end repositories iteration

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
            //Hold on to the original metatdata string since regenerating it could result in
            //minor differences from the original, which will cause checksum errors
            metadataInputStream = handle.getInputStream();
            String metadataContent = IOUtils.toString(metadataInputStream, "utf-8");
            return MavenModelUtils.toMavenMetadata(metadataContent);
        } catch (RepoRejectException rre) {
            log.warn("Metadata retrieval failed on repo '{}': {}", repo, rre.getMessage());
        } catch (IOException ioe) {
            log.error("IO exception retrieving maven metadata content from repo '{}'", repo, ioe);
        } catch (RepositoryException re) {
            log.error("Metadata retrieval failed on repo '{}': {}", repo, re);
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
        MetadataInfo metadataInfo = new MetadataInfoImpl(mavenMetadataRepoPath);
        metadataInfo.setLastModified(mergedMavenMetadata.getLastModified());
        metadataInfo.setSize(metadataContent.length());
        ByteArrayInputStream bais = new ByteArrayInputStream(metadataContent.getBytes("utf-8"));

        Checksum[] checksums = ChecksumCalculator.calculate(bais, ChecksumType.values());
        Set<ChecksumInfo> checksumInfos = Sets.newHashSetWithExpectedSize(checksums.length);
        for (Checksum checksum : checksums) {
            ChecksumInfo checksumInfo =
                    new ChecksumInfo(checksum.getType(), checksum.getChecksum(), checksum.getChecksum());
            checksumInfos.add(checksumInfo);
        }
        metadataInfo.setChecksums(checksumInfos);
        return new StringResource(metadataInfo, metadataContent);
    }

    /**
     * Assembles a list of search repositories grouped by type. Virtual repositories that don't accept the input
     * repoPath pattern are not added to the list and are not recursively visited.
     */
    private void deeplyAssembleSearchRepositoryLists(
            RepoPath repoPath, Map<String, VirtualRepo> visitedVirtualRepositories,
            Map<String, LocalRepo> searchableLocalRepositories,
            Map<String, LocalCacheRepo> searchableLocalCacheRepositories,
            Map<String, RemoteRepo> searchableRemoteRepositories) {

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
                log.debug("Repositories list assembly has been truncated to avoid recursive loop " +
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

    private static class MergedMavenMetadata {
        private Metadata metadata;
        private long lastModified;

        public Metadata getMetadata() {
            return metadata;
        }

        public long getLastModified() {
            return lastModified;
        }

        public void merge(Metadata otherMetadata, RepoResource foundResource) {
            long otherLastModified = foundResource.getLastModified();
            if (metadata == null) {
                metadata = otherMetadata;
                lastModified = otherLastModified;
            } else {
                metadata.merge(otherMetadata);
                lastModified = Math.max(otherLastModified, lastModified);

                Versioning versioning = metadata.getVersioning();
                if (versioning != null) {
                    List<String> versions = versioning.getVersions();
                    if (!Utils.isNullOrEmpty(versions)) {
                        Collections.sort(versions, new MavenVersionComparator());
                        // latest is simply the last (be it snapshot or release version)
                        String latestVersion = versions.get(versions.size() - 1);
                        versioning.setLatest(latestVersion);

                        // release is the latest non snapshot version
                        for (String version : versions) {
                            if (!MavenNaming.isSnapshot(version)) {
                                versioning.setRelease(version);
                            }
                        }
                    }

                    // if there's a unique snapshot version prefer the one with the bigger build number
                    Snapshot snapshot = versioning.getSnapshot();
                    Snapshot otherSnapshot = otherMetadata.getVersioning() != null ?
                            otherMetadata.getVersioning().getSnapshot() : null;
                    if (snapshot != null && otherSnapshot != null) {
                        if (snapshot.getBuildNumber() < otherSnapshot.getBuildNumber()) {
                            snapshot.setBuildNumber(otherSnapshot.getBuildNumber());
                            snapshot.setTimestamp(otherSnapshot.getTimestamp());
                        }
                    }
                }
            }
        }
    }
}
