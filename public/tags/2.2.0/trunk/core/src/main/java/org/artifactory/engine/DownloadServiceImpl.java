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

package org.artifactory.engine;

import org.apache.commons.httpclient.HttpStatus;
import org.artifactory.api.config.CentralConfigService;
import org.artifactory.api.mime.ChecksumType;
import org.artifactory.api.repo.exception.RepoAccessException;
import org.artifactory.api.repo.exception.maven.BadPomException;
import org.artifactory.api.request.ArtifactoryRequest;
import org.artifactory.api.request.ArtifactoryResponse;
import org.artifactory.cache.InternalCacheService;
import org.artifactory.common.ResourceStreamHandle;
import org.artifactory.descriptor.config.CentralConfigDescriptor;
import org.artifactory.io.checksum.policy.ChecksumPolicyException;
import org.artifactory.log.LoggerFactory;
import org.artifactory.repo.Repo;
import org.artifactory.repo.context.DownloadRequestContext;
import org.artifactory.repo.context.RequestContext;
import org.artifactory.repo.service.InternalRepositoryService;
import org.artifactory.request.RemoteRequestException;
import org.artifactory.request.RequestResponseHelper;
import org.artifactory.resource.RepoResource;
import org.artifactory.resource.UnfoundRepoResource;
import org.artifactory.spring.InternalContextHelper;
import org.artifactory.spring.Reloadable;
import org.artifactory.spring.ReloadableBean;
import org.artifactory.traffic.InternalTrafficService;
import org.artifactory.util.PathUtils;
import org.artifactory.version.CompoundVersionDetails;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InterruptedIOException;

@Service
@Reloadable(beanClass = InternalDownloadService.class,
        initAfter = {InternalRepositoryService.class, InternalCacheService.class})
public class DownloadServiceImpl implements InternalDownloadService {

    private static final Logger log = LoggerFactory.getLogger(DownloadServiceImpl.class);

    @Autowired
    private InternalRepositoryService repositoryService;

    @Autowired
    private CentralConfigService centralConfig;

    @Autowired
    InternalTrafficService trafficService;

    private RequestResponseHelper requestResponseHelper;

    public void init() {
        requestResponseHelper = new RequestResponseHelper(trafficService);
    }

    @SuppressWarnings({"unchecked"})
    public Class<? extends ReloadableBean>[] initAfter() {
        return new Class[]{InternalRepositoryService.class, InternalCacheService.class};
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
            log.debug("Request: source=" + request.getSourceDescription() + ", path=" + request.getPath() +
                    ", lastModified=" + centralConfig.format(request.getLastModified()) +
                    ", headOnly=" + request.isHeadOnly() + ", ifModifiedSince=" + request.getIfModifiedSince());
        }
        //Check that this is not a recursive call
        if (request.isRecursive()) {
            String msg = "Recursive call detected for '" + request + "'. Returning nothing.";
            response.sendError(HttpStatus.SC_NOT_FOUND, msg, log);
            return;
        }

        try {
            String repoKey = request.getRepoKey();
            log.debug("Download request processing ({}) for repo '{}'.", request, repoKey);
            RepoResource resource;
            Repo repository = repositoryService.repositoryByKey(repoKey);
            if (repository == null) {
                String message = "Failed to find the repository '" + repoKey + "' specified in the request.";
                log.warn(message);
                resource = new UnfoundRepoResource(request.getRepoPath(), message);
            } else {
                resource = callGetInfoInTransaction(repository, new DownloadRequestContext(request));
            }

            respond(request, response, resource);

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

    public RepoResource getInfo(Repo repo, RequestContext context) {
        return repo.getInfo(context);
    }

    //Send the response back to the client

    private void respond(ArtifactoryRequest request, ArtifactoryResponse response, RepoResource resource)
            throws IOException {
        try {
            if (!resource.isFound()) {
                String reason;
                if (resource instanceof UnfoundRepoResource) {
                    reason = ((UnfoundRepoResource) resource).getReason();
                } else {
                    reason = "Resource not found";
                }
                response.sendError(HttpStatus.SC_NOT_FOUND, reason, log);
            } else if (request.isHeadOnly()) {
                //Send head response if that's what were asked for
                requestResponseHelper.sendHeadResponse(response, resource);
            } else if (request.isNewerThanResource(resource.getLastModified())) {
                requestResponseHelper.sendNotModifiedResponse(response, resource);
            } else {
                //Get the actual repository the resource is in
                String repoKey = resource.getResponseRepoPath().getRepoKey();
                Repo repository = repositoryService.repositoryByKey(repoKey);
                if (request.isChecksum()) {
                    respondForChecksumRequest(request, response, repository, resource);
                } else {
                    //Send the resource file back (will update the cache for remote repositories)
                    ResourceStreamHandle handle;
                    try {
                        handle = repositoryService.getResourceStreamHandle(repository, resource);
                        //Streaming the file is done outside a tx, so there is a chance that the content will change!
                        requestResponseHelper.sendBodyResponse(response, resource, handle);
                    } catch (RepoAccessException rae) {
                        String msg = "Rejected artifact download request: " + rae.getMessage();
                        response.sendError(HttpStatus.SC_FORBIDDEN, msg, log);
                    } catch (ChecksumPolicyException cpe) {
                        response.sendError(HttpStatus.SC_CONFLICT, cpe.getMessage(), log);
                    } catch (RemoteRequestException rre) {
                        response.sendError(rre.getRemoteReturnCode(), rre.getMessage(), log);
                    } catch (BadPomException bpe) {
                        response.sendError(HttpStatus.SC_BAD_REQUEST, bpe.getMessage(), log);
                    }
                }
            }
        } catch (IOException e) {
            handleGenericIoException(response, resource, e);
        }
    }

    /**
     * This method handles the response to checksum requests. Eventhough this method is called only for files that
     * exist, some checksum policies might return null values, or even fail if the checksum algorithm is not found. If
     * for any reason we don't have the checksum we return http 404 to the client and let the client decide how to
     * proceed.
     */
    private void respondForChecksumRequest(ArtifactoryRequest request, ArtifactoryResponse response, Repo repo,
            RepoResource res) throws IOException {

        String checksumFilePath = request.getPath();
        String extension = '.' + PathUtils.getExtension(checksumFilePath);
        ChecksumType checksumType = ChecksumType.forExtension(extension);
        if (checksumType == null) {
            respondUnfoundChecksum(response, "Checksum not found: " + checksumFilePath);
            return;
        }

        try {
            String checksum = repo.getChecksum(checksumFilePath, res);
            if (checksum != null) {
                // send the checksum as the response body, use the original repo path from the request
                requestResponseHelper.sendBodyResponse(response, request.getRepoPath(), checksum);
            } else {
                throw new IllegalArgumentException("Checksum not found");
            }
        } catch (IllegalArgumentException e) {
            respondUnfoundChecksum(response, e.getMessage());
        }
    }

    private void respondUnfoundChecksum(ArtifactoryResponse response, String message) throws IOException {
        response.sendError(HttpStatus.SC_NOT_FOUND, message, log);
    }

    private RepoResource callGetInfoInTransaction(Repo repo, RequestContext context) {
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
