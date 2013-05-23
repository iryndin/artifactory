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

package org.artifactory.engine;

import org.apache.commons.httpclient.HttpStatus;
import org.artifactory.addon.AddonsManager;
import org.artifactory.addon.plugin.PluginsAddon;
import org.artifactory.addon.plugin.ResponseCtx;
import org.artifactory.addon.plugin.download.AfterDownloadErrorAction;
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
import org.artifactory.mime.NamingUtils;
import org.artifactory.repo.InternalRepoPathFactory;
import org.artifactory.repo.Repo;
import org.artifactory.repo.RepoPath;
import org.artifactory.repo.service.InternalRepositoryService;
import org.artifactory.request.ArtifactoryRequest;
import org.artifactory.request.DownloadRequestContext;
import org.artifactory.request.InternalRequestContext;
import org.artifactory.request.RemoteRequestException;
import org.artifactory.request.RepoRequests;
import org.artifactory.request.Request;
import org.artifactory.request.RequestContext;
import org.artifactory.request.RequestResponseHelper;
import org.artifactory.resource.ChecksumResource;
import org.artifactory.resource.ResourceStreamHandle;
import org.artifactory.resource.UnfoundRepoResource;
import org.artifactory.resource.UnfoundRepoResourceReason;
import org.artifactory.spring.InternalContextHelper;
import org.artifactory.spring.Reloadable;
import org.artifactory.storage.StorageException;
import org.artifactory.traffic.TrafficService;
import org.artifactory.util.HttpUtils;
import org.artifactory.version.CompoundVersionDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.web.authentication.www.BasicAuthenticationEntryPoint;
import org.springframework.stereotype.Service;

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
    private TrafficService trafficService;

    @Autowired
    private AddonsManager addonsManager;

    private RequestResponseHelper requestResponseHelper;

    @Override
    public void init() {
        requestResponseHelper = new RequestResponseHelper(trafficService);
    }

    @Override
    public void reload(CentralConfigDescriptor oldDescriptor) {
    }

    @Override
    public void destroy() {
    }

    @Override
    public void convert(CompoundVersionDetails source, CompoundVersionDetails target) {
    }

    /**
     * Expects requests that starts with the repo url prefix and contains an optional target local repository. E.g.:
     * <pre>http://localhost:8080/artifactory/repo/ant/ant-antlr/1.6.5/ant-antlr-1.6.5.jar</pre>
     * <pre>http://localhost:8080/artifactory/repo/org/codehaus/xdoclet/xdoclet/2.0.5-SNAPSHOT/xdoclet-2.0.5-SNAPSHOT.jar</pre>
     * or with a target local repo:
     * <pre>http://localhost:8080/artifactory/local-repo/ant/ant-antlr/1.6.5/ant-antlr-1.6.5.jar</pre>
     */
    @Override
    public void process(ArtifactoryRequest request, ArtifactoryResponse response) throws IOException {
        RepoRequests.logToContext("Request source = %s, Last modified = %s, If modified since = %s, Thread name = %s",
                request.getClientAddress(), centralConfig.format(request.getLastModified()),
                request.getIfModifiedSince(), Thread.currentThread().getName());
        //Check that this is not a recursive call
        if (request.isRecursive()) {
            RepoRequests.logToContext("Exiting download process - recursive call detected");
            String msg = "Recursive call detected for '" + request + "'. Returning nothing.";
            response.sendError(HttpStatus.SC_NOT_FOUND, msg, log);
            return;
        }

        if (NamingUtils.isMetadata(request.getPath())) {
            // the repo filter redirects for the ":properties" paths. This is here to protect direct calls to this
            // method (mainly from
            response.sendError(HttpStatus.SC_CONFLICT, "Old metadata notation is not supported anymore: " +
                    request.getRepoPath(), log);
        }

        addonsManager.interceptResponse(response);
        if (responseWasIntercepted(response)) {
            RepoRequests.logToContext("Exiting download process - intercepted by addon manager");
            return;
        }

        try {
            String repoKey = request.getRepoKey();
            RepoResource resource;
            Repo repository = repositoryService.repositoryByKey(repoKey);

            DownloadRequestContext requestContext = new DownloadRequestContext(request);

            if (repository == null) {
                RepoRequests.logToContext("Exiting download process - failed to find the repository");
                resource = new UnfoundRepoResource(request.getRepoPath(), "Failed to find the repository '" + repoKey +
                        "' specified in the request.");
            } else {
                RepoRequests.logToContext("Retrieving info");
                resource = repository.getInfo(requestContext);
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
            if (response.isSuccessful()) {
                RepoRequests.logToContext("Request succeeded");
            } else {
                Exception exception = response.getException();
                if (exception != null) {
                    RepoRequests.logToContext("Request failed: %s", exception.getMessage());
                } else {
                    RepoRequests.logToContext("Request failed with no exception");
                }
            }
        }
    }

    @Override
    public void releaseDownloadWaiters(CountDownLatch latch) {
        latch.countDown();
    }

    private boolean responseWasIntercepted(ArtifactoryResponse response) {
        return response.isError();
    }

    private void respond(InternalRequestContext requestContext, ArtifactoryResponse response, RepoResource resource)
            throws IOException {
        try {
            Request request = requestContext.getRequest();

            boolean resourceFound = resource.isFound();
            boolean headRequest = request.isHeadOnly();
            boolean checksumRequest = request.isChecksum();
            boolean targetRepoIsNotRemoteOrDoesntStore = isRepoNotRemoteOrDoesntStoreLocally(resource);

            RepoRequests.logToContext("Requested resource is found = %s", resourceFound);
            RepoRequests.logToContext("Request is HEAD = %s", headRequest);
            RepoRequests.logToContext("Request is for a checksum = %s", checksumRequest);
            RepoRequests.logToContext("Target repository is not remote or doesn't store locally = %s",
                    targetRepoIsNotRemoteOrDoesntStore);
            boolean notModified = isNotModified(request, resource);
            RepoRequests.logToContext("Requested resource was not modified = %s", notModified);

            if (!resourceFound) {
                RepoRequests.logToContext("Responding with unfound resource");
                respondResourceNotFound(requestContext, response, resource);
            } else if (headRequest && !checksumRequest && targetRepoIsNotRemoteOrDoesntStore) {
                /**
                 * Send head response only if the file isn't a checksum. Also, if the repo is a remote, only respond
                 * like this if we don't store artifacts locally (so that the whole artifact won't be requested twice),
                 * otherwise download the artifact normally and return the full info for the head request
                 */
                RepoRequests.logToContext("Responding to HEAD request with status %s", response.getStatus());
                if (notModified) {
                    requestResponseHelper.sendNotModifiedResponse(response, resource);
                } else {
                    requestResponseHelper.sendHeadResponse(response, resource);
                }
            } else if (notModified) {
                requestResponseHelper.sendNotModifiedResponse(response, resource);
            } else if (checksumRequest) {
                RepoRequests.logToContext("Responding to checksum request");
                respondForChecksumRequest(request, response, resource);
            } else {
                RepoRequests.logToContext("Responding with found resource");
                respondFoundResource(requestContext, response, resource);
            }
        } catch (IOException e) {
            RepoRequests.logToContext("Error occurred while sending request response: %s", e.getMessage());
            handleGenericIoException(response, resource, e);
        }
    }

    private boolean isNotModified(Request request, RepoResource resource) {
        boolean hasIfModifiedSince = request.hasIfModifiedSince();
        boolean notModified = request.isNewerThan(resource.getLastModified());
        boolean hasIfNoneMatch = request.hasIfNoneMatch();
        boolean nonMatch = resource.getInfo() == null || request.isNoneMatch(resource.getInfo().getSha1());

        if (notModified && !(hasIfNoneMatch && nonMatch)) {
            RepoRequests.logToContext("Local resource isn't newer - sending a not modified response");
            return true;
        } else if (!nonMatch && hasIfNoneMatch && !hasIfModifiedSince) {
            RepoRequests.logToContext("Local resource's entity exists - sending a not modified response");
            return true;
        }
        return false;
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
            RepoRequests.logToContext("Alternative response reset status as error - returning");
            return;
        }

        Repo responseRepo = repositoryService.repositoryByKey(repoKey);

        try {
            if (handle == null) {
                RepoRequests.logToContext("Retrieving a content handle from target repo");
                //Only if we didn't already set an alternate response
                handle = repositoryService.getResourceStreamHandle(requestContext, responseRepo, resource);
            }
            AddonsManager addonsManager = InternalContextHelper.get().beanForType(AddonsManager.class);
            PluginsAddon pluginAddon = addonsManager.addonByType(PluginsAddon.class);
            pluginAddon.execPluginActions(BeforeDownloadAction.class, null, requestContext.getRequest(),
                    responseRepoPath);
            RepoRequests.logToContext("Executing any BeforeDownload user plugins that may exist");

            if (requestContext.getRequest().isHeadOnly()) {
                /**
                 * If we should response to a head, make sure repo is a remote and that stores locally (to save the
                 * double artifact downloads)
                 */
                RepoRequests.logToContext("Request was of type HEAD - responding with no content");
                requestResponseHelper.sendHeadResponse(response, resource);
            } else {
                RepoRequests.logToContext("Responding with selected content handle");
                //Streaming the file is done outside a tx, so there is a chance that the content will change!
                requestResponseHelper.sendBodyResponse(response, resource, handle);
            }
        } catch (RepoRejectException rre) {
            int status = rre.getErrorCode();
            log.debug("Repo rejection while downloading: " + rre.getMessage(), rre);
            if (status == HttpStatus.SC_FORBIDDEN && authorizationService.isAnonymous()) {
                RepoRequests.logToContext("Response status is '%s' and authenticated as anonymous - sending challenge",
                        status);

                // Transform a forbidden to unauthorized if received for an anonymous user
                response.sendAuthorizationRequired(rre.getMessage(), authenticationEntryPoint.getRealmName());
            } else {
                RepoRequests.logToContext("Error occurred while sending response - sending error instead: %s",
                        rre.getMessage());
                String msg = "Rejected artifact download request: " + rre.getMessage();
                sendError(requestContext, response, status, msg, log);
            }
        } catch (RemoteRequestException rre) {
            log.debug("Remote exception while downloading: " + rre.getMessage(), rre);
            RepoRequests.logToContext("Error occurred while sending response - sending error instead: %s",
                    rre.getMessage());
            sendError(requestContext, response, rre.getRemoteReturnCode(), rre.getMessage(), log);
        } catch (BadPomException bpe) {
            log.debug("Bad pom while downloading: " + bpe.getMessage(), bpe);
            RepoRequests.logToContext("Error occurred while sending response - sending error instead: %s",
                    bpe.getMessage());
            sendError(requestContext, response, HttpStatus.SC_CONFLICT, bpe.getMessage(), log);
        } catch (StorageException se) {
            log.debug("Exception while downloading: " + se.getMessage(), se);
            RepoRequests.logToContext("Error occurred while sending response - sending error instead: %s",
                    se.getMessage());
            sendError(requestContext, response, HttpStatus.SC_INTERNAL_SERVER_ERROR, se.getMessage(), log);
        } finally {
            if (handle != null) {
                handle.close();
            }
        }
    }

    private void sendError(InternalRequestContext requestContext, ArtifactoryResponse response, int status,
            String reason, Logger log) throws IOException {
        RepoRequests.logToContext("Sending error with status %s and message '%s'", status, reason);
        PluginsAddon pluginAddon = addonsManager.addonByType(PluginsAddon.class);
        ResponseCtx responseCtx = new ResponseCtx();
        responseCtx.setMessage(reason);
        responseCtx.setStatus(status);
        pluginAddon.execPluginActions(AfterDownloadErrorAction.class, responseCtx, requestContext.getRequest());
        RepoRequests.logToContext("Executing any AfterDownloadErrorAction user plugins that may exist");

        status = responseCtx.getStatus();
        String message = responseCtx.getMessage();

        if (HttpUtils.isSuccessfulResponseCode(status)) {//plugin changed the status, it's not error anymore
            RepoRequests.logToContext("Response code was modified to %s by the user plugins", status);
            response.setStatus(status);
            if (responseCtx.getInputStream() == null) {// no content, so only message (if set) and status
                RepoRequests.logToContext("Received no response content from the user plugins");
                //message changed in the plugin, need to write it as response
                if (reason != null && !reason.equals(message)) {
                    RepoRequests.logToContext("Response message was modified to '%s' by the user plugins", message);
                    response.getWriter().write(message);
                }
                RepoRequests.logToContext("Sending successful response");
                response.sendSuccess();
            } else {//yay, content from plugin!
                RepoRequests.logToContext("Received a response content stream from the user plugins - sending");
                if (responseCtx.hasSize()) {
                    response.setContentLength(responseCtx.getSize());
                }
                response.sendStream(responseCtx.getInputStream());
            }
        } else { //still error, proceed as usual
            RepoRequests.logToContext("Response code wasn't modified by the user plugins");
            if (!message.equals(reason)) {
                RepoRequests.logToContext("Response message was modified to '%s' by the user plugins", message);
            }
            reason = message; // in case user changed the reason in the plugin
            if (status == HttpStatus.SC_FORBIDDEN && authorizationService.isAnonymous()) {
                RepoRequests.logToContext("Response status is '%s' and authenticated as anonymous - sending challenge",
                        status);
                // Transform a forbidden to unauthorized if received for an anonymous user
                String realmName = authenticationEntryPoint.getRealmName();
                response.sendAuthorizationRequired(reason, realmName);
            } else {
                RepoRequests.logToContext("Sending response with the status '%s' and the message '%s'", status,
                        message);
                response.sendError(status, reason, log);
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

        RepoRequests.logToContext("Executing any AltResponse user plugins that may exist");
        AddonsManager addonsManager = InternalContextHelper.get().beanForType(AddonsManager.class);
        PluginsAddon pluginAddon = addonsManager.addonByType(PluginsAddon.class);
        ResponseCtx responseCtx = new ResponseCtx();
        pluginAddon.execPluginActions(AltResponseAction.class, responseCtx, requestContext.getRequest(),
                responseRepoPath);
        int status = responseCtx.getStatus();
        String message = responseCtx.getMessage();
        RepoRequests.logToContext("Alternative response status is set to %s and message to '%s'", status, message);
        if (status != ResponseCtx.UNSET_STATUS) {
            if (HttpUtils.isSuccessfulResponseCode(status)) {
                RepoRequests.logToContext("Setting response status to %s", status);
                response.setStatus(status);
                if (message != null) {
                    RepoRequests.logToContext("Found non-null alternative response message - " +
                            "returning as content handle");
                    return new StringResourceStreamHandle(message);
                }
            } else {
                RepoRequests.logToContext("Sending error response with alternative status and message");
                response.sendError(status, message, log);
                return null;
            }
        }
        InputStream is = responseCtx.getInputStream();
        if (is != null) {
            RepoRequests.logToContext("Found non-null alternative response content stream - " +
                    "returning as content handle");
            return new SimpleResourceStreamHandle(is, responseCtx.getSize());
        }
        RepoRequests.logToContext("Found no alternative content handles");
        return null;
    }

    private void respondResourceNotFound(InternalRequestContext requestContext, ArtifactoryResponse response,
            RepoResource resource) throws IOException {
        String reason = "Resource not found";
        int status = HttpStatus.SC_NOT_FOUND;
        RepoRequests.logToContext("Setting default response status to '%s' reason to '%s'", status, reason);
        if (resource instanceof UnfoundRepoResourceReason) {
            RepoRequests.logToContext("Response is an instance of UnfoundRepoResourceReason");
            UnfoundRepoResourceReason unfound = (UnfoundRepoResourceReason) resource;
            // use the reason and status from the resource unless it's authorization response and the
            // settings prohibit revealing this information
            boolean hideUnauthorizedResources =
                    centralConfig.getDescriptor().getSecurity().isHideUnauthorizedResources();
            boolean originalStatusNotAuthorization = notAuthorizationStatus(unfound.getStatusCode());
            RepoRequests.logToContext("Configured to hide un-authorized resources = %s",
                    Boolean.toString(hideUnauthorizedResources));
            RepoRequests.logToContext("Original response status is auth related = %s",
                    Boolean.toString(!originalStatusNotAuthorization));
            if (!hideUnauthorizedResources || originalStatusNotAuthorization) {
                reason = unfound.getReason();
                status = unfound.getStatusCode();
                RepoRequests.logToContext("Using original response status of '%s' and message '%s'", status, reason);
            }
        }
        if (status == HttpStatus.SC_FORBIDDEN && authorizationService.isAnonymous()) {
            RepoRequests.logToContext("Response status is '%s' and authenticated as anonymous - sending challenge",
                    status);
            // Transform a forbidden to unauthorized if received for an anonymous user
            String realmName = authenticationEntryPoint.getRealmName();
            response.sendAuthorizationRequired(reason, realmName);
        } else {
            sendError(requestContext, response, status, reason, log);
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
            RepoRequests.logToContext("Requested resource is located within an archive");
            requestRepoPath = InternalRepoPathFactory
                    .archiveResourceRepoPath(requestRepoPath, request.getZipResourcePath());
        }
        String requestChecksumFilePath = requestRepoPath.getPath();
        ChecksumType checksumType = ChecksumType.forFilePath(requestChecksumFilePath);
        if (checksumType == null) {
            RepoRequests.logToContext("Unable to detect the type of the requested checksum - responding with status %s",
                    HttpStatus.SC_NOT_FOUND);
            response.sendError(HttpStatus.SC_NOT_FOUND, "Checksum not found: " + requestChecksumFilePath, log);
            return;
        }

        RepoPath responseRepoPath = resource.getResponseRepoPath();
        String repoKey = responseRepoPath.getRepoKey();
        String responsePath = responseRepoPath.getPath();
        Repo repository = repositoryService.repositoryByKey(repoKey);
        String checksum = repository.getChecksum(responsePath + checksumType.ext(), resource);
        if (checksum == null) {
            RepoRequests.logToContext("Unable to find the the requested checksum - responding with status %s",
                    HttpStatus.SC_NOT_FOUND);
            response.sendError(HttpStatus.SC_NOT_FOUND, "Checksum not found for " + responsePath, log);
            return;
        }

        if (request.isHeadOnly()) {
            RepoRequests.logToContext("Sending checksum HEAD response");
            // send head response using the checksum data
            ChecksumResource checksumResource = new ChecksumResource(resource, checksumType, checksum);
            requestResponseHelper.sendHeadResponse(response, checksumResource);
        } else {
            AddonsManager addonsManager = InternalContextHelper.get().beanForType(AddonsManager.class);
            PluginsAddon pluginAddon = addonsManager.addonByType(PluginsAddon.class);
            pluginAddon.execPluginActions(BeforeDownloadAction.class, null, request, responseRepoPath);
            RepoRequests.logToContext("Executing any BeforeDownloadAction user plugins that may exist");
            // send the checksum as the response body, use the original repo path (the checksum path,
            // not the file) from the request
            RepoRequests.logToContext("Sending checksum response with status %s", response.getStatus());
            requestResponseHelper.sendBodyResponse(response, requestRepoPath, checksum);
        }
    }

    private void handleGenericIoException(ArtifactoryResponse response, RepoResource res, IOException e)
            throws IOException {
        if (e instanceof InterruptedIOException) {
            String msg = this + ": Timed out when retrieving data for " + res.getRepoPath()
                    + " (" + e.getMessage() + ").";
            RepoRequests.logToContext("Setting response status to %s - %s", HttpStatus.SC_NOT_FOUND, msg);
            response.sendError(HttpStatus.SC_NOT_FOUND, msg, log);
        } else {
            throw e;
        }
    }
}
