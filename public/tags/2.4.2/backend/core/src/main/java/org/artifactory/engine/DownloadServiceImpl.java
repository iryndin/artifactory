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

package org.artifactory.engine;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.lang.StringUtils;
import org.artifactory.addon.AddonsManager;
import org.artifactory.addon.plugin.PluginsAddon;
import org.artifactory.addon.plugin.ResponseCtx;
import org.artifactory.addon.plugin.download.AltResponseAction;
import org.artifactory.addon.plugin.download.BeforeDownloadAction;
import org.artifactory.api.config.CentralConfigService;
import org.artifactory.api.repo.exception.RepoRejectException;
import org.artifactory.api.repo.exception.maven.BadPomException;
import org.artifactory.api.request.ArtifactoryResponse;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.checksum.ChecksumType;
import org.artifactory.descriptor.config.CentralConfigDescriptor;
import org.artifactory.descriptor.repo.RemoteRepoDescriptor;
import org.artifactory.fs.RepoResource;
import org.artifactory.io.SimpleResourceStreamHandle;
import org.artifactory.io.StringResourceStreamHandle;
import org.artifactory.log.LoggerFactory;
import org.artifactory.repo.InternalRepoPathFactory;
import org.artifactory.repo.Repo;
import org.artifactory.repo.RepoPath;
import org.artifactory.repo.service.InternalRepositoryService;
import org.artifactory.request.ArtifactoryRequest;
import org.artifactory.request.DownloadRequestContext;
import org.artifactory.request.InternalRequestContext;
import org.artifactory.request.RemoteRequestException;
import org.artifactory.request.Request;
import org.artifactory.request.RequestContext;
import org.artifactory.request.RequestResponseHelper;
import org.artifactory.resource.ChecksumResource;
import org.artifactory.resource.ResourceStreamHandle;
import org.artifactory.resource.UnfoundRepoResource;
import org.artifactory.resource.UnfoundRepoResourceReason;
import org.artifactory.spring.InternalContextHelper;
import org.artifactory.spring.Reloadable;
import org.artifactory.traffic.InternalTrafficService;
import org.artifactory.util.HttpUtils;
import org.artifactory.version.CompoundVersionDetails;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.web.authentication.www.BasicAuthenticationEntryPoint;
import org.springframework.stereotype.Service;

import javax.jcr.RepositoryException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.util.concurrent.CountDownLatch;

/**
 * @author Yoav Landman
 */

@Service
@Reloadable(beanClass = InternalDownloadService.class,
        initAfter = {InternalRepositoryService.class})
public class DownloadServiceImpl implements InternalDownloadService {

    private static final Logger log = LoggerFactory.getLogger(DownloadServiceImpl.class);

    @Autowired
    private AuthorizationService authorizationService;

    @Autowired
    private BasicAuthenticationEntryPoint authenticationEntryPoint;

    @Autowired
    private InternalRepositoryService repositoryService;

    @Autowired
    private CentralConfigService centralConfig;

    @Autowired
    private InternalTrafficService trafficService;

    @Autowired
    private AddonsManager addonsManager;

    private RequestResponseHelper requestResponseHelper;

    public void init() {
        requestResponseHelper = new RequestResponseHelper(trafficService);
    }

    public void reload(CentralConfigDescriptor oldDescriptor) {
    }

    public void destroy() {
    }

    public void convert(CompoundVersionDetails source, CompoundVersionDetails target) {
    }

    /**
     * Expects requests that starts with the repo url prefix and contains an optional target local repository. E.g.:
     * <pre>http://localhost:8080/artifactory/repo/ant/ant-antlr/1.6.5/ant-antlr-1.6.5.jar</pre>
     * <pre>http://localhost:8080/artifactory/repo/org/codehaus/xdoclet/xdoclet/2.0.5-SNAPSHOT/xdoclet-2.0.5-SNAPSHOT.jar</pre>
     * or with a target local repo:
     * <pre>http://localhost:8080/artifactory/local-repo/ant/ant-antlr/1.6.5/ant-antlr-1.6.5.jar</pre>
     */
    public void process(ArtifactoryRequest request, ArtifactoryResponse response) throws IOException {
        if (log.isDebugEnabled()) {
            log.debug("Request: source=" + request.getClientAddress() + ", path=" + request.getPath() +
                    ", lastModified=" + centralConfig.format(request.getLastModified()) +
                    ", headOnly=" + request.isHeadOnly() + ", ifModifiedSince=" + request.getIfModifiedSince());
        }
        //Check that this is not a recursive call
        if (request.isRecursive()) {
            String msg = "Recursive call detected for '" + request + "'. Returning nothing.";
            response.sendError(HttpStatus.SC_NOT_FOUND, msg, log);
            return;
        }

        String intercept = addonsManager.interceptRequest();
        if (StringUtils.isNotBlank(intercept)) {
            response.sendError(HttpStatus.SC_FORBIDDEN, intercept, log);
            return;
        }

        try {
            String repoKey = request.getRepoKey();
            log.debug("Download request processing ({}) for repo '{}'.", request, repoKey);
            RepoResource resource;
            Repo repository = repositoryService.repositoryByKey(repoKey);

            DownloadRequestContext requestContext = new DownloadRequestContext(request);

            if (repository == null) {
                String message = "Failed to find the repository '" + repoKey + "' specified in the request.";
                log.warn(message);
                resource = new UnfoundRepoResource(request.getRepoPath(), message);
            } else {
                resource = callGetInfoInTransaction(repository, requestContext);
            }

            respond(requestContext, response, resource);

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
                    log.debug("Request for path '{}' succeeded.", request.getPath());
                } else {
                    Exception exception = response.getException();
                    if (exception != null) {
                        log.debug("Request for path '{}' failed: {}.",
                                new Object[]{request.getPath(), exception.getMessage()}, exception);
                    } else {
                        log.debug("Request for path '{}' failed with no exception.", request.getPath());
                    }
                }
            }
        }
    }

    public RepoResource getInfo(Repo repo, InternalRequestContext context) {
        return repo.getInfo(context);
    }

    public void releaseDownloadWaiters(CountDownLatch latch) {
        latch.countDown();
    }

    private void respond(InternalRequestContext requestContext, ArtifactoryResponse response, RepoResource resource)
            throws IOException {
        try {
            Request request = requestContext.getRequest();

            if (!resource.isFound()) {
                respondResourceNotFound(response, resource);
            } else if (request.isHeadOnly() && !request.isChecksum() && isRepoNotRemoteOrDoesntStoreLocally(resource)) {
                /**
                 * Send head response only if the file isn't a checksum. Also, if the repo is a remote, only respond
                 * like this if we don't store artifacts locally (so that the whole artifact won't be requested twice),
                 * otherwise download the artifact normally and return the full info for the head request
                 */
                requestResponseHelper.sendHeadResponse(response, resource);
            } else if (request.isNewerThan(resource.getLastModified())) {
                requestResponseHelper.sendNotModifiedResponse(response, resource);
            } else if (request.isChecksum()) {
                respondForChecksumRequest(request, response, resource);
            } else {
                respondFoundResource(requestContext, response, resource);
            }
        } catch (IOException e) {
            handleGenericIoException(response, resource, e);
        }
    }

    private boolean isRepoNotRemoteOrDoesntStoreLocally(RepoResource resource) {
        RemoteRepoDescriptor remoteRepoDescriptor = repositoryService.remoteRepoDescriptorByKey(
                resource.getRepoPath().getRepoKey());
        return (remoteRepoDescriptor == null) || !remoteRepoDescriptor.isStoreArtifactsLocally();
    }

    private void respondFoundResource(InternalRequestContext requestContext, ArtifactoryResponse response,
            RepoResource resource) throws IOException {
        //Get the actual repository the resource is in
        RepoPath responseRepoPath = resource.getResponseRepoPath();
        String repoKey = responseRepoPath.getRepoKey();

        //Send the resource file back (will update the cache for remote repositories)
        ResourceStreamHandle handle = getAlternateHandle(requestContext, response, responseRepoPath);
        if (response.isError()) {
            return;
        }

        Repo responseRepo = repositoryService.repositoryByKey(repoKey);

        try {
            if (handle == null) {
                //Only if we didn't already set an alternate response
                handle = repositoryService.getResourceStreamHandle(requestContext, responseRepo, resource);
            }
            AddonsManager addonsManager = InternalContextHelper.get().beanForType(AddonsManager.class);
            PluginsAddon pluginAddon = addonsManager.addonByType(PluginsAddon.class);
            pluginAddon.execPluginActions(BeforeDownloadAction.class, null, requestContext.getRequest(),
                    responseRepoPath);

            if (requestContext.getRequest().isHeadOnly()) {
                /**
                 * If we should response to a head, make sure repo is a remote and that stores locally (to save the
                 * double artifact downloads)
                 */
                requestResponseHelper.sendHeadResponse(response, resource);
            } else {
                //Streaming the file is done outside a tx, so there is a chance that the content will change!
                requestResponseHelper.sendBodyResponse(response, resource, handle);
            }
        } catch (RepoRejectException rre) {
            int status = rre.getErrorCode();
            if (status == HttpStatus.SC_FORBIDDEN && authorizationService.isAnonymous()) {
                // Transform a forbidden to unauthorized if received for an anonymous user
                response.sendAuthorizationRequired(rre.getMessage(), authenticationEntryPoint.getRealmName());
            } else {
                String msg = "Rejected artifact download request: " + rre.getMessage();
                response.sendError(status, msg, log);
            }
        } catch (RemoteRequestException rre) {
            response.sendError(rre.getRemoteReturnCode(), rre.getMessage(), log);
        } catch (BadPomException bpe) {
            response.sendError(HttpStatus.SC_CONFLICT, bpe.getMessage(), log);
        } catch (RepositoryException re) {
            response.sendError(HttpStatus.SC_INTERNAL_SERVER_ERROR, re.getMessage(), log);
        } finally {
            if (handle != null) {
                handle.close();
            }
        }
    }

    /**
     * Executes any subscribing user plugin routines and returns an alternate resource handle if given
     *
     * @param requestContext   Context
     * @param response         Response to return
     * @param responseRepoPath Actual repo path of the requested artifact
     * @return Stream handle if return by plugins. Null if not.
     */
    private ResourceStreamHandle getAlternateHandle(RequestContext requestContext, ArtifactoryResponse response,
            RepoPath responseRepoPath) throws IOException {
        //See if we need to return an alternate response

        AddonsManager addonsManager = InternalContextHelper.get().beanForType(AddonsManager.class);
        PluginsAddon pluginAddon = addonsManager.addonByType(PluginsAddon.class);
        ResponseCtx responseCtx = new ResponseCtx();
        pluginAddon.execPluginActions(AltResponseAction.class, responseCtx, requestContext.getRequest(),
                responseRepoPath);
        int status = responseCtx.getStatus();
        String message = responseCtx.getMessage();
        if (status != ResponseCtx.UNSET_STATUS) {
            if (HttpUtils.isSuccessfulResponseCode(status)) {
                response.setStatus(status);
                if (message != null) {
                    return new StringResourceStreamHandle(message);
                }
            } else {
                response.sendError(status, message, log);
                return null;
            }
        }
        InputStream is = responseCtx.getInputStream();
        if (is != null) {
            return new SimpleResourceStreamHandle(is, responseCtx.getSize());
        }
        return null;
    }

    private void respondResourceNotFound(ArtifactoryResponse response, RepoResource resource) throws IOException {
        String reason = "Resource not found";
        int status = HttpStatus.SC_NOT_FOUND;
        if (resource instanceof UnfoundRepoResourceReason) {
            UnfoundRepoResourceReason unfound = (UnfoundRepoResourceReason) resource;
            // use the reason and status from the resource unless it's authorization response and the
            // settings prohibit revealing this information
            boolean hideUnauthorizedResources =
                    centralConfig.getDescriptor().getSecurity().isHideUnauthorizedResources();
            if (!hideUnauthorizedResources || notAuthorizationStatus(unfound.getStatusCode())) {
                reason = unfound.getReason();
                status = unfound.getStatusCode();
            }
        }
        if (status == HttpStatus.SC_FORBIDDEN && authorizationService.isAnonymous()) {
            // Transform a forbidden to unauthorized if received for an anonymous user
            String realmName = authenticationEntryPoint.getRealmName();
            response.sendAuthorizationRequired(reason, realmName);
        } else {
            response.sendError(status, reason, log);
        }
    }

    private boolean notAuthorizationStatus(int status) {
        return status != HttpStatus.SC_UNAUTHORIZED && status != HttpStatus.SC_FORBIDDEN;
    }

    /**
     * This method handles the response to checksum requests (HEAD and GET). Even though this method is called only for
     * files that exist, some checksum policies might return null values, or even fail if the checksum algorithm is not
     * found. If for any reason we don't have the checksum we return http 404 to the client and let the client decide
     * how to proceed.
     */
    private void respondForChecksumRequest(Request request, ArtifactoryResponse response,
            RepoResource resource) throws IOException {

        RepoPath requestRepoPath = request.getRepoPath();
        if (request.isZipResourceRequest()) {
            requestRepoPath = InternalRepoPathFactory
                    .archiveResourceRepoPath(requestRepoPath, request.getZipResourcePath());
        }
        String requestChecksumFilePath = requestRepoPath.getPath();
        ChecksumType checksumType = ChecksumType.forFilePath(requestChecksumFilePath);
        if (checksumType == null) {
            response.sendError(HttpStatus.SC_NOT_FOUND, "Checksum not found: " + requestChecksumFilePath, log);
            return;
        }

        RepoPath responseRepoPath = resource.getResponseRepoPath();
        String repoKey = responseRepoPath.getRepoKey();
        String responsePath = responseRepoPath.getPath();
        Repo repository = repositoryService.repositoryByKey(repoKey);
        String checksum = repository.getChecksum(responsePath + checksumType.ext(), resource);
        if (checksum == null) {
            response.sendError(HttpStatus.SC_NOT_FOUND, "Checksum not found for " + responsePath, log);
            return;
        }

        if (request.isHeadOnly()) {
            // send head response using the checksum data
            ChecksumResource checksumResource = new ChecksumResource(resource, checksumType, checksum);
            requestResponseHelper.sendHeadResponse(response, checksumResource);
        } else {
            AddonsManager addonsManager = InternalContextHelper.get().beanForType(AddonsManager.class);
            PluginsAddon pluginAddon = addonsManager.addonByType(PluginsAddon.class);
            pluginAddon.execPluginActions(BeforeDownloadAction.class, null, request, responseRepoPath);
            // send the checksum as the response body, use the original repo path (the checksum path,
            // not the file) from the request
            requestResponseHelper.sendBodyResponse(response, requestRepoPath, checksum);
        }
    }

    private RepoResource callGetInfoInTransaction(Repo repo, InternalRequestContext context) {
        InternalDownloadService txMe = InternalContextHelper.get().beanForType(InternalDownloadService.class);
        return txMe.getInfo(repo, context);
    }

    private void handleGenericIoException(ArtifactoryResponse response, RepoResource res, IOException e)
            throws IOException {
        if (e instanceof InterruptedIOException) {
            String msg = this + ": Timed out when retrieving data for " + res.getRepoPath()
                    + " (" + e.getMessage() + ").";
            response.sendError(HttpStatus.SC_NOT_FOUND, msg, log);
        } else {
            throw e;
        }
    }
}
