package org.artifactory.engine;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.log4j.Logger;
import org.artifactory.maven.MavenUtil;
import org.artifactory.repo.CentralConfig;
import org.artifactory.repo.LocalRepo;
import org.artifactory.repo.Repo;
import org.artifactory.request.ArtifactoryRequest;
import org.artifactory.request.ArtifactoryResponse;
import org.artifactory.request.RequestResponseHelper;
import org.artifactory.resource.RepoResource;
import org.artifactory.spring.ArtifactoryContext;
import org.artifactory.utils.DateUtils;
import org.artifactory.utils.IoUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class DownloadEngine {

    private static final org.apache.log4j.Logger LOGGER = Logger.getLogger(DownloadEngine.class);

    private final ArtifactoryContext context;

    public DownloadEngine(ArtifactoryContext context) {
        this.context = context;
    }

    /**
     * Expects requests that starts with the repo url prefix and contains an optional target local
     * repository. E.g.:
     * <pre>http://localhost:8080/proxy/repo/ant/ant-antlr/1.6.5/ant-antlr-1.6.5.jar</pre>
     * <pre>http://localhost:8080/proxy/repo/org/codehaus/xdoclet/xdoclet/2.0.5-SNAPSHOT/xdoclet-2.0.5-SNAPSHOT.jar</pre>
     * or with a target local repo:
     * <pre>http://localhost:8080/proxy/repo@local-repo/ant/ant-antlr/1.6.5/ant-antlr-1.6.5.jar</pre>
     *
     * @param request
     * @param response
     * @throws IOException
     */
    public void process(ArtifactoryRequest request, ArtifactoryResponse response)
            throws IOException {
        CentralConfig cc = context.getCentralConfig();
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Request: source=" + request.getSourceDescription() +
                    ", path=" + request.getPath() +
                    ", lastModified=" + DateUtils.format(request.getLastModified(), cc) +
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
            if (request.isSnapshot() || request.isMetaData()) {
                processUpdate(request, response);
            } else {
                processStandard(request, response);
            }
        } catch (IOException e) {
            LOGGER.error("Response error: " + e.getMessage());
            response.sendError(HttpStatus.SC_INTERNAL_SERVER_ERROR);
            throw e;
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
            IoUtils.transferStream(is, os);
            IoUtils.close(os);
            IoUtils.close(is);
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
            IoUtils.close(os);
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
        List<Repo> repositories = getLocalRepositories(request);
        CentralConfig cc = context.getCentralConfig();
        repositories.addAll(cc.getLocalCaches());
        //If the request comes from another artifactory don't bother checking
        //remote repositories
        if (request.isFromAnotherArtifactory()) {
            LOGGER.info("Received another Artifactory request '" + request +
                    "' from '" + request.getSourceDescription() +
                    "'. Skipping remote repos checks.");
        } else {
            //Check remote repositories
            repositories.addAll(cc.getRemoteRepositories());
        }

        //Locate the resource matching the request
        RepoResource foundRes = null;
        Repo foundRepo = null;
        String path = request.getPath();
        for (Repo repo : repositories) {
            //Skip if not handling
            if (!repo.isHandleReleases()) {
                continue;
            }
            RepoResource res = repo.getInfo(path);
            if (res.isFound()) {
                foundRes = res;
                foundRepo = repo;
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
    private void processUpdate(ArtifactoryRequest request, ArtifactoryResponse response)
            throws IOException {
        String path = request.getPath();
        //Find the latest in all repositories
        RepoResource latestRes = null;
        Repo latestArtifactRepo = null;
        CentralConfig cc = context.getCentralConfig();
        //Traverse the local, caches and remote repositories and search for the newest snapshot
        List<Repo> repositories = getLocalRepositories(request);
        repositories.addAll(cc.getLocalCaches());
        repositories.addAll(cc.getRemoteRepositories());
        for (Repo repo : repositories) {
            //Skip if not handling
            if (!repo.isHandleSnapshots() && MavenUtil.isSnapshot(path)) {
                continue;
            }
            //TODO: [by yl] getInfo retrieves the file
            final RepoResource res = repo.getInfo(path);
            if (!res.isFound()) {
                continue;
            }
            LOGGER.info(repo + ": " + res.getRelPath() + " last modified "
                    + DateUtils.format(res.getLastModified(), cc));
            //If we haven't found one yet, or this one is newer than the one found, take it
            if (latestRes == null
                    || res.getLastModified().after(latestRes.getLastModified())) {
                LOGGER.info(repo + ": found newer res: " + path);
                latestRes = res;
                latestArtifactRepo = repo;
            }
        }
        //Not found
        boolean nonFoundRetrievalCacheHit = latestRes != null && !latestRes.isFound();
        if (latestRes == null || nonFoundRetrievalCacheHit) {
            LOGGER.info("Artifact not found: " + path +
                    (nonFoundRetrievalCacheHit ? " (cache on " +
                            DateUtils.format(latestRes.getLastModified(), cc) + ")" : ""));
            response.sendError(HttpStatus.SC_NOT_FOUND);
            return;
        }
        //Found a newer version
        String repositoryName = latestRes.getRepoKey();
        LOGGER.info(repositoryName + ": Found the latest version of " + request.getPath());
        respond(request, response, latestArtifactRepo, latestRes);
    }

    private List<Repo> getLocalRepositories(ArtifactoryRequest request) {
        List<Repo> repositories = new ArrayList<Repo>();
        //Check if we target a specific local repo, otherwise search all local repos
        String localRepoKey = request.getTargetRepoGroup();
        if (CentralConfig.DEFAULT_REPO_GROUP.equals(localRepoKey)) {
            CentralConfig cc = context.getCentralConfig();
            repositories.addAll(cc.getLocalRepositories());
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(
                        "Update request processing (" + request + ") includes all local repos.");
            }
        } else {
            repositories.add(getLocalRepo(localRepoKey));
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Update request processing (" + request + ") includes local repo '" +
                        localRepoKey + "'.");
            }
        }
        return repositories;
    }

    //Send the response back to the client
    private void respond(ArtifactoryRequest request, ArtifactoryResponse response,
            Repo repo, RepoResource res) throws IOException {
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
    }

    private LocalRepo getLocalRepo(String localRepoKey) {
        CentralConfig cc = context.getCentralConfig();
        LocalRepo localRepo = cc.localOrCachedRepositoryByKey(localRepoKey);
        if (localRepo == null) {
            throw new RuntimeException(
                    "Failed to find local repository '" + localRepoKey + "' specified in request.");
        }
        return localRepo;
    }
}