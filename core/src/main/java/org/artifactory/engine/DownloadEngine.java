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

import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Reader;
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Writer;
import org.artifactory.checksum.Checksum;
import org.artifactory.checksum.ChecksumInputStream;
import org.artifactory.checksum.ChecksumType;
import org.artifactory.config.CentralConfig;
import org.artifactory.maven.MavenUtils;
import org.artifactory.repo.LocalCacheRepo;
import org.artifactory.repo.LocalRepo;
import org.artifactory.repo.RemoteRepo;
import org.artifactory.repo.Repo;
import org.artifactory.repo.virtual.VirtualRepo;
import org.artifactory.request.ArtifactoryRequest;
import org.artifactory.request.ArtifactoryResponse;
import org.artifactory.request.RequestResponseHelper;
import org.artifactory.resource.RepoResource;
import org.artifactory.security.RepoPath;
import org.artifactory.spring.ArtifactoryContext;
import org.artifactory.utils.DateUtils;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DownloadEngine {

    private static final org.apache.log4j.Logger LOGGER = Logger.getLogger(DownloadEngine.class);

    private final ArtifactoryContext context;
    private final Map<RepoPath, CachedChecksumEntry> mergedMetadataChecksums =
            new HashMap<RepoPath, CachedChecksumEntry>();

    public DownloadEngine(ArtifactoryContext context) {
        this.context = context;
    }

    /**
     * Expects requests that starts with the repo url prefix and contains an optional target local
     * repository. E.g.:
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
            throws IOException {
        String path = request.getPath();
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Request: source=" + request.getSourceDescription() +
                    ", path=" + path +
                    ", lastModified=" + DateUtils.format(request.getLastModified()) +
                    ", headOnly=" + request.isHeadOnly() + ", ifModifiedSince=" +
                    request.getIfModifiedSince());
        }
        //Check that this is not a recursive call
        if (request.isRecursive()) {
            LOGGER.info("Recursive call detected for '" + request + "'. Returning nothing.");
            response.sendError(HttpStatus.SC_NOT_FOUND);
            return;
        }
        try {
            //If we try to update snapshots, metadata, poms (caused by expiry or -U)
            if (request.isSnapshot()) {
                processSnapshot(request, response);
            } else if (request.isMetaData()) {
                processMetadata(request, response);
            } else {
                processStandard(request, response);
            }
        } catch (IOException e) {
            LOGGER.error("Response error: " + e.getMessage());
            //We can get here when sending a response while the client hangs up the connection.
            //In this case the response will be committed so there is no point in sending an error.
            if (!response.isCommitted()) {
                response.sendError(HttpStatus.SC_INTERNAL_SERVER_ERROR);
            }
            throw e;
        } finally {
            boolean successful = response.isSuccessful();
            LOGGER.debug("Request for path '" + path + "'" +
                    (successful ? " succeeded." : " failed."));
            /*
            if (!successful) {
                // Cleanup stray metadata.xml if not successful
                JcrHelper jcr = context.getJcr();
                jcr.doInSession(new StrayDataCleaner(request));
            }
            */
        }
    }

    /**
     * Downloads a stream to a file, using a temporary file to ensure any problems are not written
     * to target file
     */
    public static void download(File target, InputStream is, long lastModified) {
        File dir = target.getParentFile();
        dir.mkdirs();

        File tmpTarget = null;
        OutputStream os = null;
        try {
            tmpTarget = File.createTempFile("tmp", ".tmp", dir);
            os = new FileOutputStream(tmpTarget);
            IOUtils.copy(is, os);
            IOUtils.closeQuietly(os);
            IOUtils.closeQuietly(is);
            target.delete();
            tmpTarget.renameTo(target);

            //Update the last modified of the target file - it will be used to update the artifact
            //with the correct update time after restart
            if (!target.setLastModified(lastModified)) {
                LOGGER.warn(target + ".setLastModified(" + lastModified + ") failed");
            }
            if (target.lastModified() != lastModified) {
                LOGGER.warn(target + ".setLastModified(" + lastModified + ") didn't stick - now "
                        + target.lastModified());
            }
        } catch (Exception e) {
            IOUtils.closeQuietly(os);
            if (tmpTarget != null) {
                tmpTarget.delete();
            }
        }
    }

    /**
     * Iterate over a list of repos until a resource is found in one of them, and return that
     * resource (content or just head information) on the response. The first resource that is found
     * is returned. The order of searching is: local repos, cache repos and remote repos (unless
     * request originated from another Artifactory).
     *
     * @param request
     * @param response
     * @return
     * @throws IOException
     */
    private void processStandard(
            ArtifactoryRequest request, ArtifactoryResponse response) throws IOException {
        //For metadata checksums, first check the merged metadata cache
        RepoPath repoPath = request.getRepoPath();
        String path = repoPath.getPath();
        if (MavenUtils.isMetadataChecksum(path)) {
            synchronized (mergedMetadataChecksums) {
                CachedChecksumEntry cachedChecksumEntry = mergedMetadataChecksums.get(repoPath);
                if (cachedChecksumEntry != null) {
                    //Send back a merged metdata and return
                    String checksum = cachedChecksumEntry.checksum;
                    RequestResponseHelper.sendBodyResponse(response, path, checksum);
                    return;
                }
            }
        }
        List<Repo> repositories = assembleSearchRepositoriesList(request);
        //Locate the resource matching the request
        RepoResource foundRes = null;
        Repo foundRepo = null;
        for (Repo repo : repositories) {
            //Skip if not handling
            if (!repo.isHandleReleases() && !MavenUtils.isChecksum(path)) {
                continue;
            }
            RepoResource res = repo.getInfo(path);
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
            response.sendError(HttpStatus.SC_NOT_FOUND);
        }
    }

    /**
     * Iterate over the repos and return the latest resource found (content or just head
     * information) on the response.
     */
    private void processSnapshot(ArtifactoryRequest request, ArtifactoryResponse response)
            throws IOException {
        String path = request.getPath();
        //Find the latest in all repositories
        RepoResource latestRes = null;
        Repo latestArtifactRepo = null;
        //Traverse the local, caches and remote repositories and search for the newest snapshot
        //Make sure local repos are always searched first
        List<Repo> repositories = assembleSearchRepositoriesList(request);
        boolean foundInLocalRepo = false;
        for (Repo repo : repositories) {
            //Skip if not handling
            if (!repo.isHandleSnapshots()) {
                continue;
            }
            //Skip remote repos if found in local repo (including caches)
            if (foundInLocalRepo && !repo.isLocal()) {
                continue;
            }
            final RepoResource res = repo.getInfo(path);
            if (!res.isFound()) {
                continue;
            } else {
                //If found inside a local repsoitory, take the latest out of the local repo only
                if (repo.isLocal()) {
                    if (foundInLocalRepo) {
                        //Issue a warning for a resource found multiple times in local repos
                        LOGGER.warn(repo + ": found multiple resource instances of '" + path +
                                "' in local repositories.");
                    } else {
                        LOGGER.debug(repo + ": found local res: " + path);
                        foundInLocalRepo = true;
                    }
                }
            }
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(repo + ": " + res.getPath() + " last modified "
                        + DateUtils.format(res.getLastModified()));
            }
            //If we haven't found one yet, or this one is newer than the one found, take it
            if (latestRes == null
                    || res.getLastModified().after(latestRes.getLastModified())) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug(repo + ": found newer res: " + path);
                }
                latestRes = res;
                latestArtifactRepo = repo;
            }
        }
        //Not found
        boolean nonFoundRetrievalCacheHit = latestRes != null && !latestRes.isFound();
        if (latestRes == null || nonFoundRetrievalCacheHit) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Artifact not found: " + path +
                        (nonFoundRetrievalCacheHit ? " (cached on " +
                                DateUtils.format(latestRes.getLastModified()) + ")" : ""));
            }
            response.sendError(HttpStatus.SC_NOT_FOUND);
            return;
        }
        //Found a newer version
        String repositoryName = latestRes.getRepoKey();
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(repositoryName + ": Found the latest version of " + request.getPath());
        }
        respond(request, response, latestArtifactRepo, latestRes);
    }

    /**
     * Iterate over the repos and return the latest resource found (content or just head
     * information) on the response.
     */
    @SuppressWarnings({"ResultOfMethodCallIgnored"})
    private void processMetadata(ArtifactoryRequest request, ArtifactoryResponse response)
            throws IOException {
        String path = request.getPath();
        //Traverse the local, caches and remote repositories
        //Make sure local repos are always searched first
        List<Repo> repositories = assembleSearchRepositoriesList(request);
        boolean foundInLocalRepo = false;
        Metadata metadata = null;
        String origMetadataContent = null;
        long mergedMetadataLastModifiedTime = -1;
        for (Repo repo : repositories) {
            //Skip remote repos if found in local repo (including caches)
            if (foundInLocalRepo && !repo.isLocal()) {
                continue;
            }
            final RepoResource res = repo.getInfo(path);
            if (!res.isFound()) {
                continue;
            } else {
                //If found inside a local repsoitory, take the latest out of the local repo only
                if (repo.isLocal()) {
                    LOGGER.debug(repo + ": found local res: " + path);
                    foundInLocalRepo = true;
                }
                ResourceStreamHandle handle = repo.getResourceStreamHandle(res);
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
                                lastModifiedTime(res));
                    } else {
                        metadata = foundMetadata;
                    }
                } catch (IOException e) {
                    //Handle IOExceptions
                    if (e instanceof InterruptedIOException) {
                        LOGGER.info(this + ": Timeout occured when retrieving metadata for " +
                                res.getRepoKey() + "/" + res.getPath() +
                                " (" + e.getMessage() + ").");
                        response.sendError(HttpStatus.SC_NOT_FOUND);
                        return;
                    }
                } catch (XmlPullParserException e) {
                    LOGGER.error("Bad metadata encountered.", e);
                    throw new IOException("Failed to parse metadata: " + e.getMessage());
                } finally {
                    handle.close();
                }
            }
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(repo + ": " + res.getPath() + " last modified "
                        + DateUtils.format(res.getLastModified()));
            }
        }
        //Not found
        if (metadata == null) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Artifact metadata not found for '" + path + "'.");
            }
            response.sendError(HttpStatus.SC_NOT_FOUND);
        } else {
            RepoPath repoPath = request.getRepoPath();
            RepoPath checkSumRepoPath = new RepoPath(repoPath.getRepoKey(),
                    repoPath.getPath() + ChecksumType.sha1.ext());
            String metadataContent;
            if (mergedMetadataLastModifiedTime > 0) {
                //Found and merged - return the merged metadata
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Merged artifact metadata found for '" + path + "'.");
                }
                MetadataXpp3Writer writer = new MetadataXpp3Writer();
                StringWriter stringWriter = new StringWriter();
                writer.write(stringWriter, metadata);
                metadataContent = stringWriter.toString();
                synchronized (mergedMetadataChecksums) {
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
                }
            } else {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Non-merged artifact metadata found for '" + path + "'.");
                }
                //Found and not merged - return the original metadata
                metadataContent = origMetadataContent;
                //Remove a stale entry from the metadata checksums cache
                synchronized (mergedMetadataChecksums) {
                    mergedMetadataChecksums.remove(checkSumRepoPath);
                }
            }
            RequestResponseHelper.sendBodyResponse(response, path, metadataContent);
        }
    }

    private long lastModifiedTime(RepoResource res) {
        return res.getLastModifiedTime();
    }

    private List<Repo> assembleSearchRepositoriesList(ArtifactoryRequest request) {
        CentralConfig cc = context.getCentralConfig();
        List<Repo> repositories = new ArrayList<Repo>();
        //Check if we target a specific local (or cache) repo or a virtual repository
        String localOrVirtualRepoKey = request.getRepoKey();
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Download request processing (" + request + ") includes local repo '" +
                    localOrVirtualRepoKey + "'.");
        }
        boolean fromAnotherArtifactory = request.isFromAnotherArtifactory();
        if (fromAnotherArtifactory) {
            //If the request comes from another artifactory don't bother checking any remote repos
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Received another Artifactory request '" + request +
                        "' from '" + request.getSourceDescription() +
                        "'. Skipping remote repos checks.");
            }
        }
        VirtualRepo virtualRepo = cc.virtualRepositoryByKey(localOrVirtualRepoKey);
        //VirtualRepo globalVirtualRepo = cc.getGlobalVirtualRepo();
        if (virtualRepo != null) {
            //We are handling a virtual repo request - add its local repositories
            repositories.addAll(virtualRepo.getLocalRepositories());
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(
                        "Update request processing (" + request + ") includes all local repos.");
            }
            //Add the caches
            List<LocalCacheRepo> allCaches = virtualRepo.getLocalCaches();
            repositories.addAll(allCaches);
            //Add the remote repositories
            if (!fromAnotherArtifactory) {
                repositories.addAll(virtualRepo.getRemoteRepositories());
            }
        } else {
            //We are handling a specific local (or cache) repo request - add its local repositories
            VirtualRepo globalVirtualRepo = cc.getGlobalVirtualRepo();
            LocalRepo localOrCacheRepo =
                    globalVirtualRepo.localOrCachedRepositoryByKey(localOrVirtualRepoKey);
            if (localOrCacheRepo == null) {
                LOGGER.warn("Failed to find the local or virtual repository '"
                        + localOrVirtualRepoKey + "' specified in the request.");
                //Return an empty repositories list (that will eventually result in a 404)
                return repositories;
            }
            repositories.add(localOrCacheRepo);
            //Add the caches
            boolean targetRepoIsCache = localOrCacheRepo.isCache();
            if (!targetRepoIsCache) {
                //Only append all other caches if the request is not a direct cache request
                List<LocalCacheRepo> allCaches = globalVirtualRepo.getLocalCaches();
                repositories.addAll(allCaches);
            }
            //Add the remote repositories
            if (!fromAnotherArtifactory) {
                if (targetRepoIsCache) {
                    //If the request is directly to a cache add only the remote repo of this cache
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("Received a direct cache request '" + request +
                                "' from '" + request.getSourceDescription() +
                                "'. Adding only this cache's remote repo.");
                    }
                    LocalCacheRepo cacheRepo = (LocalCacheRepo) localOrCacheRepo;
                    RemoteRepo remoteRepo = cacheRepo.getRemoteRepo();
                    repositories.add(remoteRepo);
                } else {
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("Received a local repository request '" + request +
                                "' from '" + request.getSourceDescription() +
                                "'. Adding all global remote repositories.");
                    }
                    repositories.addAll(globalVirtualRepo.getRemoteRepositories());
                }
            }
        }
        return repositories;
    }

    //Send the response back to the client
    private void respond(ArtifactoryRequest request, ArtifactoryResponse response,
            Repo repo, RepoResource res) throws IOException {
        try {
            //Send head response
            if (request.isHeadOnly()) {
                //TODO: [by yl] When does the above condition occur?
                RequestResponseHelper.sendHeadResponse(response, res);
                return;
            }
            if (request.isNewerThanResource(res)) {
                RequestResponseHelper.sendNotModifiedResponse(response, res);
            } else {
                //Send the file back (will update the cache for remote repositories)
                ResourceStreamHandle handle = repo.getResourceStreamHandle(res);
                RequestResponseHelper.sendBodyResponse(response, res, handle);
            }
        } catch (IOException e) {
            //Handle IOExceptions
            if (e instanceof InterruptedIOException) {
                LOGGER.info(this + ": Timeout occured when retrieving data for " +
                        res.getRepoKey() + "/" + res.getPath() +
                        " (" + e.getMessage() + ").");
                response.sendError(HttpStatus.SC_NOT_FOUND);
            }
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