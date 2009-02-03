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
package org.artifactory.engine;

import org.apache.commons.collections15.OrderedMap;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.io.IOUtils;
import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Reader;
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Writer;
import org.artifactory.api.cache.ArtifactoryCache;
import org.artifactory.api.cache.CacheService;
import org.artifactory.api.config.CentralConfigService;
import org.artifactory.api.repo.RepoPath;
import org.artifactory.api.repo.exception.FileExpectedException;
import org.artifactory.api.repo.exception.RepoAccessException;
import org.artifactory.api.request.ArtifactoryRequest;
import org.artifactory.api.request.ArtifactoryResponse;
import org.artifactory.common.ResourceStreamHandle;
import org.artifactory.descriptor.config.CentralConfigDescriptor;
import org.artifactory.io.checksum.Checksum;
import org.artifactory.io.checksum.ChecksumInputStream;
import org.artifactory.io.checksum.ChecksumType;
import org.artifactory.maven.MavenUtils;
import org.artifactory.repo.LocalCacheRepo;
import org.artifactory.repo.LocalRepo;
import org.artifactory.repo.RealRepo;
import org.artifactory.repo.RemoteRepo;
import org.artifactory.repo.service.InternalRepositoryService;
import org.artifactory.repo.virtual.VirtualRepo;
import org.artifactory.request.RequestResponseHelper;
import org.artifactory.resource.RepoResource;
import org.artifactory.spring.InternalContextHelper;
import org.artifactory.spring.ReloadableBean;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.Serializable;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class DownloadServiceImpl implements InternalDownloadService {

    private static final Logger log = LoggerFactory.getLogger(DownloadServiceImpl.class);

    @Autowired
    private InternalRepositoryService repositoryService;

    @Autowired
    private CentralConfigService centralConfig;

    // TODO: The all merge metadata process should be extracted into the VirtualRepo class
    private Map<RepoPath, CachedChecksumEntry> mergedMetadataChecksums;

    @PostConstruct
    private void register() {
        InternalContextHelper.get().addReloadableBean(InternalDownloadService.class);
    }

    @SuppressWarnings({"unchecked"})
    public void init() {
        // TODO: Should be Virtual Repo
        CacheService cacheService = InternalContextHelper.get().beanForType(CacheService.class);
        mergedMetadataChecksums = cacheService.getCache(ArtifactoryCache.mergedMetadataChecksums);
    }

    @SuppressWarnings({"unchecked"})
    public Class<? extends ReloadableBean>[] initAfter() {
        return new Class[]{InternalRepositoryService.class, CacheService.class};
    }

    public void reload(CentralConfigDescriptor oldDescriptor) {
    }

    public void destroy() {
    }

    /**
     * Expects requests that starts with the repo url prefix and contains an optional target local repository. E.g.:
     * <pre>http://localhost:8080/artifactory/repo/ant/ant-antlr/1.6.5/ant-antlr-1.6.5.jar</pre>
     * <pre>http://localhost:8080/artifactory/repo/org/codehaus/xdoclet/xdoclet/2.0.5-SNAPSHOT/xdoclet-2.0.5-SNAPSHOT.jar</pre>
     * or with a target local repo:
     * <pre>http://localhost:8080/artifactory/local-repo/ant/ant-antlr/1.6.5/ant-antlr-1.6.5.jar</pre>
     *
     * @param request
     * @param response
     * @throws IOException
     */
    public void process(ArtifactoryRequest request, ArtifactoryResponse response)
            throws IOException, FileExpectedException {
        String path = request.getPath();
        if (log.isDebugEnabled()) {
            log.debug("Request: source=" + request.getSourceDescription() +
                    ", path=" + path +
                    ", lastModified=" + centralConfig.format(request.getLastModified()) +
                    ", headOnly=" + request.isHeadOnly() + ", ifModifiedSince=" +
                    request.getIfModifiedSince());
        }
        //Check that this is not a recursive call
        if (request.isRecursive()) {
            String msg = "Recursive call detected for '" + request + "'. Returning nothing.";
            response.sendError(HttpStatus.SC_NOT_FOUND, msg, log);
            return;
        }
        try {
            //If we try to update snapshots, metadata, poms (caused by expiry or -U)
            if (request.isSnapshot()) {
                processSnapshot(request, response);
            } else if (request.isMetadata()) {
                processMetadata(request, response);
            } else {
                processStandard(request, response);
            }
        } catch (IOException e) {
            response.setException(e);
            //We can get here when sending a response while the client hangs up the connection.
            //In this case the response will be committed so there is no point in sending an error.
            if (!response.isCommitted()) {
                response.sendInternalError(e, log);
            }
            throw e;
        } finally {
            if (log.isDebugEnabled()) {
                if (response.isSuccessful()) {
                    log.debug("Request for path '" + path + "' succeeded.");
                } else {
                    Exception exception = response.getException();
                    if (exception != null) {
                        log.debug(
                                "Request for path '" + path + "' failed: " + exception.getMessage(),
                                exception);
                    } else {
                        log.debug("Request for path '" + path + "' failed with no exception.");
                    }
                }
            }
        }
    }

    public RepoResource getInfo(String path, RealRepo repo) {
        return repo.getInfo(path);
    }

    /**
     * Iterate over a list of repos until a resource is found in one of them, and return that resource (content or just
     * head information) on the response. The first resource that is found is returned. The order of searching is: local
     * repos, cache repos and remote repos (unless request originated from another Artifactory).
     *
     * @param request
     * @param response
     * @return
     * @throws IOException
     */
    private void processStandard(ArtifactoryRequest request, ArtifactoryResponse response)
            throws IOException, FileExpectedException {
        //For metadata checksums, first check the merged metadata cache
        RepoPath repoPath = request.getRepoPath();
        String path = repoPath.getPath();
        if (MavenUtils.isMetadataChecksum(path)) {
            CachedChecksumEntry cachedChecksumEntry = mergedMetadataChecksums.get(repoPath);
            if (cachedChecksumEntry != null) {
                //Send back a merged metdata and return
                String checksum = cachedChecksumEntry.checksum;
                RequestResponseHelper.sendBodyResponse(response, repoPath, checksum);
                return;
            }
        }
        String resourcePath = request.getResourcePath();
        List<RealRepo> repositories = assembleSearchRepositoriesList(request);
        //Locate the resource matching the request
        RepoResource foundRes = null;
        RealRepo foundRepo = null;
        for (RealRepo repo : repositories) {
            //Skip if not handling and not a snapshot checksum (on a snapshot repo)
            if (!repo.isHandleReleases() && !MavenUtils.isChecksum(path) && !MavenUtils.isIndex(path)) {
                continue;
            }
            RepoResource res = txGetInfo(repo, resourcePath);
            if (res.isFound()) {
                foundRes = res;
                foundRepo = repo;
                break;
            }
        }
        if (foundRes != null) {
            respond(request, response, foundRepo, foundRes);
        } else {
            //Found nothing and not commited (e.g. by last modified) - 404
            response.sendError(HttpStatus.SC_NOT_FOUND, resourcePath + " not found!", log);
        }
    }

    /**
     * Iterate over the repos and return the latest resource found (content or just head information) on the response.
     */
    @SuppressWarnings({"OverlyComplexMethod"})
    private void processSnapshot(ArtifactoryRequest request, ArtifactoryResponse response)
            throws IOException, FileExpectedException {
        String resourcePath = request.getResourcePath();
        //Find the latest in all repositories
        RepoResource latestRes = null;
        RealRepo latestArtifactRepo = null;
        //Traverse the local, caches and remote repositories and search for the newest snapshot
        //Make sure local repos are always searched first
        List<RealRepo> repositories = assembleSearchRepositoriesList(request);
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
            final RepoResource res = txGetInfo(repo, resourcePath);
            if (!res.isFound()) {
                continue;
            } else {
                //If found inside a local repsoitory, take the latest out of the local repo only
                if (repo.isLocal()) {
                    if (foundInLocalRepo) {
                        //Issue a warning for a resource found multiple times in local repos
                        log.warn(repo + ": found multiple resource instances of '" + resourcePath +
                                "' in local repositories.");
                    } else {
                        log.debug(repo + ": found local res: " + resourcePath);
                        foundInLocalRepo = true;
                    }
                }
            }
            if (log.isDebugEnabled()) {
                log.debug(res.getRepoPath().toString() + " last modified "
                        + centralConfig.format(res.getLastModified()));
            }
            //If we haven't found one yet, or this one is newer than the one found, take it
            if (latestRes == null
                    || res.getLastModified() > latestRes.getLastModified()) {
                if (log.isDebugEnabled()) {
                    log.debug(repo + ": found newer res: " + resourcePath);
                }
                latestRes = res;
                latestArtifactRepo = repo;
            }
        }
        //Not found
        boolean nonFoundRetrievalCacheHit = latestRes != null && !latestRes.isFound();
        if (latestRes == null || nonFoundRetrievalCacheHit) {
            String msg = "Artifact not found: " + resourcePath +
                    (nonFoundRetrievalCacheHit ? " (cached on " +
                            centralConfig.format(latestRes.getLastModified()) + ")" : "");
            response.sendError(HttpStatus.SC_NOT_FOUND, msg, log);
            return;
        }
        //Found a newer version
        if (log.isDebugEnabled()) {
            log.debug(latestRes.getRepoPath().getRepoKey() + ": Found the latest version of " +
                    request.getPath());
        }
        respond(request, response, latestArtifactRepo, latestRes);
    }

    /**
     * Iterate over the repos and return the latest resource found (content or just head information) on the response.
     */
    @SuppressWarnings({"ResultOfMethodCallIgnored", "OverlyComplexMethod"})
    private void processMetadata(ArtifactoryRequest request, ArtifactoryResponse response)
            throws IOException, FileExpectedException {
        String path = request.getPath();
        //Traverse the local, caches and remote repositories
        //Make sure local repos are always searched first
        List<RealRepo> repositories = assembleSearchRepositoriesList(request);
        boolean foundInLocalRepo = false;
        Metadata metadata = null;
        String origMetadataContent = null;
        long mergedMetadataLastModifiedTime = -1;
        for (RealRepo repo : repositories) {
            //Skip remote repos if found in local repo (including caches)
            if (foundInLocalRepo && !repo.isLocal()) {
                continue;
            }
            //TODO: [by yl] to get md - return a fake resource and add a coordinatesLocator
            final RepoResource res = txGetInfo(repo, path);
            if (!res.isFound()) {
                continue;
            } else {
                //If found inside a local repsoitory, take the latest out of the local repo only
                if (repo.isLocal()) {
                    if (log.isDebugEnabled()) {
                        log.debug(repo + ": found local res: " + path);
                    }
                    foundInLocalRepo = true;
                }
                ResourceStreamHandle handle;
                try {
                    handle = repositoryService.getResourceStreamHandle(repo, res);
                } catch (RepoAccessException e) {
                    String msg = "Metadata retrieval failed: " + e.getMessage();
                    response.sendError(HttpStatus.SC_FORBIDDEN, msg, log);
                    return;
                } catch (IOException ioe) {
                    handleIoException(response, res, ioe);
                    return;
                }
                //Create metadata
                InputStream metadataInputStream;
                try {
                    //Hold on to the original metatdata string since regenerating it could result in
                    //minor differences from the original, which will cause checksum errors
                    metadataInputStream = handle.getInputStream();
                    origMetadataContent = IOUtils.toString(metadataInputStream, "utf-8");
                    MetadataXpp3Reader reader = new MetadataXpp3Reader();
                    Metadata foundMetadata = reader.read(new StringReader(origMetadataContent));
                    //Merge with old metadata or create new one if doesn't exit
                    if (metadata != null) {
                        metadata.merge(foundMetadata);
                        //Remember the latest metdata resource that has been merged
                        mergedMetadataLastModifiedTime = Math.max(mergedMetadataLastModifiedTime,
                                res.getLastModified());
                    } else {
                        metadata = foundMetadata;
                    }
                } catch (IOException e) {
                    //Handle IOExceptions
                    if (e instanceof InterruptedIOException) {
                        String msg = this + ": Timeout occured when retrieving metadata for " +
                                res.getRepoPath() +
                                " (" + e.getMessage() + ").";
                        response.sendError(HttpStatus.SC_NOT_FOUND, msg, log);
                        return;
                    }
                } catch (XmlPullParserException e) {
                    log.error("Bad metadata encountered.", e);
                    throw new IOException("Failed to parse metadata: " + e.getMessage());
                } finally {
                    handle.close();
                }
            }
            if (log.isDebugEnabled()) {
                log.debug(res.getRepoPath().toString() + " last modified "
                        + centralConfig.format(res.getLastModified()));
            }
        }
        //Not found
        if (metadata == null) {
            String msg = "Artifact metadata not found for '" + path + "'.";
            response.sendError(HttpStatus.SC_NOT_FOUND, msg, log);
        } else {
            RepoPath repoPath = request.getRepoPath();
            RepoPath checkSumRepoPath = new RepoPath(repoPath.getRepoKey(),
                    repoPath.getPath() + ChecksumType.sha1.ext());
            String metadataContent;
            if (mergedMetadataLastModifiedTime > 0) {
                //Found and merged - return the merged metadata
                if (log.isDebugEnabled()) {
                    log.debug("Merged artifact metadata found for '" + path + "'.");
                }
                MetadataXpp3Writer writer = new MetadataXpp3Writer();
                StringWriter stringWriter = new StringWriter();
                writer.write(stringWriter, metadata);
                metadataContent = stringWriter.toString();
                CachedChecksumEntry cachedChecksumEntry =
                        mergedMetadataChecksums.get(checkSumRepoPath);
                if (cachedChecksumEntry == null ||
                        (cachedChecksumEntry != null &&
                                (mergedMetadataLastModifiedTime > cachedChecksumEntry
                                        .lastUpdated) || metadataContent.length() !=
                                cachedChecksumEntry.metadataContentLength)) {
                    //Calculate the checksum and cache it if newer metadata was merged or
                    //metadata length was changed (e.g. due to removal/replacement of a
                    //contributing repo
                    String name = new File(repoPath.getPath()).getName();
                    Checksum checksum = new Checksum(name, ChecksumType.sha1);
                    byte[] bytes = metadataContent.getBytes("utf-8");
                    ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
                    ChecksumInputStream csis = new ChecksumInputStream(bais, checksum);
                    csis.read(new byte[bytes.length], 0, bytes.length);
                    csis.read();
                    Checksum[] checksums = csis.getChecksums();
                    String checksumContent = checksums[0].getChecksum();
                    mergedMetadataChecksums
                            .put(checkSumRepoPath, new CachedChecksumEntry(checksumContent,
                                    metadataContent.length()));
                }
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("Non-merged artifact metadata found for '" + path + "'.");
                }
                //Found and not merged - return the original metadata
                metadataContent = origMetadataContent;
                //Remove a stale entry from the metadata checksums cache
                mergedMetadataChecksums.remove(checkSumRepoPath);
            }
            RequestResponseHelper.sendBodyResponse(response, repoPath, metadataContent);
        }
    }

    private List<RealRepo> assembleSearchRepositoriesList(ArtifactoryRequest request) {
        List<RealRepo> repositories = new ArrayList<RealRepo>();
        //Check if we target a specific local (or cache) repo or a virtual repository
        String realOrVirtualRepoKey = request.getRepoKey();
        if (log.isDebugEnabled()) {
            log.debug("Download request processing (" + request + ") for repo '" +
                    realOrVirtualRepoKey + "'.");
        }
        VirtualRepo virtualRepo = repositoryService.virtualRepositoryByKey(realOrVirtualRepoKey);
        if (virtualRepo != null) {
            if (log.isDebugEnabled()) {
                log.debug("Request processing (" + request + ") includes all local repos.");
            }
            //We are handling a virtual repo request
            //Get the virtual repo deep search lists
            OrderedMap<String, LocalRepo> localRepos =
                    virtualRepo.getSearchableLocalRepositories();
            OrderedMap<String, LocalCacheRepo> localCacheRepos =
                    virtualRepo.getSearchableLocalCacheRepositories();
            OrderedMap<String, RemoteRepo> remoteRepos =
                    virtualRepo.getSearchableRemoteRepositories();
            //Add all local repositories
            repositories.addAll(localRepos.values());
            //Add all caches
            repositories.addAll(localCacheRepos.values());
            //Add all remote repositories
            boolean fromAnotherArtifactory = request.isFromAnotherArtifactory();
            boolean artifactoryRequestsCanRetrieveRemoteArtifacts =
                    virtualRepo.isArtifactoryRequestsCanRetrieveRemoteArtifacts();
            if (fromAnotherArtifactory) {
                if (log.isDebugEnabled()) {
                    log.debug("Received another Artifactory request '" + request +
                            "' from '" + request.getSourceDescription() + "'.");
                }
            }
            if (fromAnotherArtifactory && !artifactoryRequestsCanRetrieveRemoteArtifacts) {
                //If the request comes from another artifactory don't bother checking any remote repos
                if (log.isDebugEnabled()) {
                    log.debug("Skipping remote repository checks for Artifactory request '" +
                            request + "'.");
                }
            } else {
                repositories.addAll(remoteRepos.values());
            }
        } else {
            //We are handling a specific local/cache or remote repo request - add the specific repo
            //Test remote repo fisrt, since the cache may be retrieved by the same name
            RemoteRepo remoteRepo = repositoryService.remoteRepositoryByKey(realOrVirtualRepoKey);
            if (remoteRepo != null) {
                repositories.add(remoteRepo);
            } else {
                LocalRepo localOrCacheRepo =
                        repositoryService.localOrCachedRepositoryByKey(realOrVirtualRepoKey);
                if (localOrCacheRepo != null) {
                    repositories.add(localOrCacheRepo);
                } else {
                    //Will return an empty repositories list (will eventually result in a 404)
                    log.warn("Failed to find the local or virtual repository '"
                            + realOrVirtualRepoKey + "' specified in the request.");
                }
            }
        }
        return repositories;
    }

    //Send the response back to the client
    private void respond(ArtifactoryRequest request, ArtifactoryResponse response,
            RealRepo repo, RepoResource res) throws IOException, FileExpectedException {
        try {
            //Send head response if that's what were asked for
            if (request.isHeadOnly()) {
                RequestResponseHelper.sendHeadResponse(response, res);
                return;
            }
            if (request.isNewerThanResource(res.getLastModified())) {
                RequestResponseHelper.sendNotModifiedResponse(response, res);
            } else {
                if (request.isResourceProperty()) {
                    String path = request.getPath();
                    String property = repositoryService.getMetadataProperty(repo, path);
                    RepoPath repoPath = request.getRepoPath();
                    RequestResponseHelper.sendBodyResponse(response, repoPath, property);
                } else {
                    //Send the resource file back (will update the cache for remote repositories)
                    ResourceStreamHandle handle;
                    try {
                        handle = repositoryService.getResourceStreamHandle(repo, res);
                    } catch (RepoAccessException e) {
                        String msg = "Rejected artifact download request: " + e.getMessage();
                        response.sendError(HttpStatus.SC_FORBIDDEN, msg, log);
                        return;
                    }
                    RequestResponseHelper.sendBodyResponse(response, res, handle);
                }
            }
        } catch (IOException e) {
            handleIoException(response, res, e);
        }
    }

    private RepoResource txGetInfo(RealRepo repo, String resourcePath) {
        InternalDownloadService txMe = InternalContextHelper.get().beanForType(
                InternalDownloadService.class);
        RepoResource res = txMe.getInfo(resourcePath, repo);
        return res;
    }

    private void handleIoException(ArtifactoryResponse response, RepoResource res, IOException e)
            throws IOException {
        //Handle IOExceptions
        if (e instanceof InterruptedIOException) {
            String msg = this + ": Timed out when retrieving data for " +
                    res.getRepoPath() + " (" + e.getMessage() + ").";
            response.sendError(HttpStatus.SC_NOT_FOUND, msg, log);
        } else {
            throw e;
        }
    }

    private static class CachedChecksumEntry implements Serializable {

        final String checksum;

        //When was this cache entry last updated
        final long lastUpdated;

        final long metadataContentLength;

        CachedChecksumEntry(String checksum, long metadataContentLength) {
            this.checksum = checksum;
            this.metadataContentLength = metadataContentLength;
            this.lastUpdated = System.currentTimeMillis();
        }

    }
}