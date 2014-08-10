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

package org.artifactory.webapp.servlet;

import org.apache.http.HttpStatus;
import org.artifactory.api.context.ArtifactoryContext;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.api.repo.exception.FileExpectedException;
import org.artifactory.api.request.ArtifactoryResponse;
import org.artifactory.api.request.DownloadService;
import org.artifactory.api.request.UploadService;
import org.artifactory.api.rest.constant.ArtifactRestConstants;
import org.artifactory.api.webdav.WebdavService;
import org.artifactory.exception.CancelException;
import org.artifactory.mime.NamingUtils;
import org.artifactory.repo.RepoPath;
import org.artifactory.request.ArtifactoryRequest;
import org.artifactory.request.RepoRequests;
import org.artifactory.security.HttpAuthenticationDetails;
import org.artifactory.util.HttpUtils;
import org.artifactory.util.PathUtils;
import org.artifactory.webapp.wicket.page.browse.listing.ArtifactListPage;
import org.artifactory.webapp.wicket.page.browse.simplebrowser.SimpleRepoBrowserPage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

public class RepoFilter extends DelayedFilterBase {
    private static final Logger log = LoggerFactory.getLogger(RepoFilter.class);

    public static final String ATTR_ARTIFACTORY_REPOSITORY_PATH = "artifactory.repository_path";
    public static final String ATTR_ARTIFACTORY_REQUEST_PROPERTIES = "artifactory.request_properties";
    public static final String ATTR_ARTIFACTORY_REMOVED_REPOSITORY_PATH =
            "artifactory.removed_repository_path";

    @Override
    public void initLater(FilterConfig filterConfig) throws ServletException {
        String nonUiPathPrefixes = filterConfig.getInitParameter("nonUiPathPrefixes");
        String uiPathPrefixes = filterConfig.getInitParameter("UiPathPrefixes");
        List<String> nonUiPrefixes = PathUtils.delimitedListToStringList(nonUiPathPrefixes, ",");
        RequestUtils.setNonUiPathPrefixes(nonUiPrefixes);
        List<String> uiPrefixes = PathUtils.delimitedListToStringList(uiPathPrefixes, ",");
        uiPrefixes.add(HttpUtils.WEBAPP_URL_PATH_PREFIX);
        RequestUtils.setUiPathPrefixes(uiPrefixes);
    }

    @Override
    public void destroy() {
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) resp;
        final String servletPath = RequestUtils.getServletPathFromRequest(request);
        execute(chain, request, response, servletPath);
    }

    @SuppressWarnings("OverlyComplexMethod")
    private void execute(FilterChain chain, final HttpServletRequest request, HttpServletResponse response,
            String servletPath) throws IOException, ServletException {
        if (log.isDebugEnabled()) {
            log.debug("Entering request {}.", requestDebugString(request));
        }
        boolean repoRequest = servletPath != null && RequestUtils.isRepoRequest(request, true);
        if (repoRequest && servletPath.startsWith("/" + ArtifactoryRequest.LIST_BROWSING_PATH)
                && servletPath.endsWith("/")) {
            ArtifactoryRequest artifactoryRequest = new HttpArtifactoryRequest(request);
            doRepoListing(request, response, servletPath, artifactoryRequest);
            return;
        }

        String method = request.getMethod().toLowerCase().intern();

        if (repoRequest) {
            ArtifactoryRequest artifactoryRequest = new HttpArtifactoryRequest(request);
            //Handle upload and download requests
            ArtifactoryResponse artifactoryResponse = new HttpArtifactoryResponse(response);

            if ("get".equals(method) && artifactoryRequest.isDirectoryRequest()) {
                //Check that this is not a recursive call
                if (artifactoryRequest.isRecursive()) {
                    String msg = "Recursive call detected for '" + request + "'. Returning nothing.";
                    artifactoryResponse.sendError(HttpStatus.SC_NOT_FOUND, msg, log);
                    return;
                }
                // TODO: [by fsi] all the servlet path analysis was already done in the ArtifactoryRequest object
                // TODO: Needs to be reused here!
                log.debug("Serving a directory get request.");
                if (RequestUtils.isWebdavRequest(request)) {
                    doWebDavDirectory(response, artifactoryRequest);
                    return;
                }
                if (servletPath.startsWith("/" + ArtifactoryRequest.SIMPLE_BROWSING_PATH)) {
                    doSimpleRepoBrowse(request, response, artifactoryRequest);
                } else {
                    doRepoListing(request, response, servletPath, artifactoryRequest);
                }
                return;
            }

            try {
                initRequestContext(method, artifactoryRequest, artifactoryResponse);
                if ("get".equals(method) || "head".equals(method)) {

                    /**
                     * Do not check for this parameter when not performing a get/head request so that the container
                     * doesn't try to read the parameters and verify the size of the form in case of an upload
                     */
                    if (artifactoryRequest.getParameter(ArtifactRestConstants.TRACE_PARAM) != null) {
                        //Re-init the context with the trace logging response
                        artifactoryResponse = new TraceLoggingResponse(artifactoryResponse);
                        initRequestContext(method, artifactoryRequest, artifactoryResponse);
                    }

                    // TODO: Should we return 405 Method not allowed for head request on properties:
                    // TODO: For now the HEAD request will ignore this properties query param
                    if (artifactoryRequest.getParameter(ArtifactRestConstants.PROPERTIES_PARAM) != null) {
                        //Set the response to return only the properties of the item in json format
                        artifactoryResponse.setPropertiesMediaType(MediaType.APPLICATION_JSON.toString());
                    }
                    if (artifactoryRequest.getParameter(ArtifactRestConstants.PROPERTIES_XML_PARAM) != null) {
                        //Set the response to return only the properties of the item in json format
                        artifactoryResponse.setPropertiesMediaType(MediaType.APPLICATION_XML.toString());
                    }

                    doDownload(request, response, method, artifactoryRequest, artifactoryResponse);
                    return;
                }

                if ("put".equals(method)) {
                    doUpload(artifactoryRequest, artifactoryResponse);
                    return;
                }

                doWebDavMethod(request, response, method, artifactoryRequest, artifactoryResponse);
            } finally {
                RepoRequests.destroy();
            }

        } else if (!response.isCommitted()) {
            // Webdav request not on repository, return 403
            if (RequestUtils.isWebdavRequest(request)) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                if (log.isDebugEnabled()) {
                    log.debug("Received webdav request on " + servletPath + " which is not a repository!\n" +
                            "Returning " + HttpServletResponse.SC_FORBIDDEN);
                }
            } else {
                chain.doFilter(request, response);
            }
        }
        if (log.isDebugEnabled()) {
            log.debug("Exiting request " + requestDebugString(request));
        }
    }

    private void initRequestContext(String method, ArtifactoryRequest artifactoryRequest,
            ArtifactoryResponse artifactoryResponse) {
        RepoRequests.set(method, getContext().getAuthorizationService().currentUsername(),
                artifactoryRequest, artifactoryResponse);
    }

    private void doWebDavMethod(HttpServletRequest request, HttpServletResponse response, String method,
            ArtifactoryRequest artifactoryRequest, ArtifactoryResponse artifactoryResponse) throws IOException {
        if (!getWebdavService().handleRequest(method, artifactoryRequest, artifactoryResponse)) {
            response.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            response.setHeader("Allow", PathUtils.collectionToDelimitedString(getWebdavService().supportedMethods()));
            log.info("Received unsupported request method: {} from:{ }.", method, request.getRemoteAddr());
        }
    }

    private void doUpload(ArtifactoryRequest artifactoryRequest, ArtifactoryResponse artifactoryResponse)
            throws IOException {
        try {
            log.debug("Serving an upload request.");
            getUploadEngine().upload(artifactoryRequest, artifactoryResponse);
        } catch (CancelException e) {
            log.debug("Upload request has been canceled", e.getMessage());
            artifactoryResponse.sendInternalError(e, log);
        } catch (Exception e) {
            log.debug("Upload request of {} failed due to {}", artifactoryRequest.getRepoPath(), e);
            artifactoryResponse.sendInternalError(e, log);
        }
    }

    private void doDownload(HttpServletRequest request, HttpServletResponse response, String method,
            ArtifactoryRequest artifactoryRequest, ArtifactoryResponse artifactoryResponse) throws IOException {

        if (redirectLegacyMetadataRequest(request, response, artifactoryRequest)) {
            return;
        }

        try {
            RepoRequests.logToContext("Received request");
            getDownloadService().process(artifactoryRequest, artifactoryResponse);
        } catch (FileExpectedException e) {
            // If we try to get a file but encounter a folder and the request does not end with a '/' send a redirect
            // that adds the slash with the request with a 302 status code. In the next request if it is a head request,
            // then it is ok since the resource was found and avoid an infinite redirect situation, however if it is a
            // GET, then return a 404 since it is the incorrect resource to get (we mimic was apache servers are doing).
            // see RTFACT-2738 and RTFACT-3510
            if (!request.getServletPath().endsWith("/")) {
                String dirPath = request.getRequestURL().append("/").toString();
                RepoRequests.logToContext("Redirecting to the directory path '%s'", dirPath);
                response.sendRedirect(dirPath);
            } else if ("head".equals(method)) {
                RepoRequests.logToContext("Handling directory HEAD request ");
            } else {
                RepoRequests.logToContext("Expected file but received a directory - returning a %s response",
                        HttpServletResponse.SC_NOT_FOUND);
                artifactoryResponse.sendError(HttpServletResponse.SC_NOT_FOUND,
                        "Expected file response but received a directory response: " + e.getRepoPath(), log);
            }
        } catch (CancelException e) {
            RepoRequests.logToContext("Request has been canceled", e.getMessage(), e.getErrorCode());
            artifactoryResponse.sendError(e.getErrorCode(), "Download request has been canceled: " + e.getMessage(),
                    log);
            log.debug("Download request has been canceled" + e.getMessage(), e);
        } catch (Exception e) {
            RepoRequests.logToContext("Error handling request: %s - returning a %s response", e.getMessage(),
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            artifactoryResponse.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Could not process download request: " + e.getMessage(), log);
            log.debug("Could not process download request: " + e.getMessage(), e);
        }
    }

    private void doSimpleRepoBrowse(HttpServletRequest request, HttpServletResponse response,
            ArtifactoryRequest artifactoryRequest) throws ServletException, IOException {
        log.debug("Forwarding internally to a directory browsing request.");
        //Expose the artifactory repository path as a request attribute
        final RepoPath repoPath = artifactoryRequest.getRepoPath();
        //TODO: [by ys] the virtual repo should throw and exception if no item doesn't exist
        //if (checkForInvalidPath(response, repoPath)) {
        //    return;
        //}
        request.setAttribute(ATTR_ARTIFACTORY_REPOSITORY_PATH, repoPath);
        request.setAttribute(ATTR_ARTIFACTORY_REQUEST_PROPERTIES, artifactoryRequest.getProperties());

        //Remove the forwarding URL (repo+path) as this is used by wicket to build
        //a relative path, which does not make sense in this case
        final boolean wicketRequest = RequestUtils.isWicketRequest(request);
        HttpServletRequestWrapper requestWrapper = new InnerRequestWrapper(request, wicketRequest);

        RequestDispatcher dispatcher = request.getRequestDispatcher(
                "/" + HttpUtils.WEBAPP_URL_PATH_PREFIX + "/" + SimpleRepoBrowserPage.PATH);
        dispatcher.forward(requestWrapper, response);
    }

    private void doRepoListing(HttpServletRequest request, HttpServletResponse response, String servletPath,
            ArtifactoryRequest artifactoryRequest) throws ServletException, IOException {
        log.debug("Forwarding internally to an apache-style listing page.");
        if (!servletPath.endsWith("/")) {
            response.sendRedirect(HttpUtils.getServletContextUrl(request) + servletPath + "/");
            return;
        }
        /*
        if (checkForInvalidPath(response, repoPath)) {
            return;
        }
        */
        request.setAttribute(ATTR_ARTIFACTORY_REPOSITORY_PATH, artifactoryRequest.getRepoPath());
        request.setAttribute(ATTR_ARTIFACTORY_REQUEST_PROPERTIES, artifactoryRequest.getProperties());

        RequestDispatcher dispatcher =
                request.getRequestDispatcher("/" + HttpUtils.WEBAPP_URL_PATH_PREFIX + "/" + ArtifactListPage.PATH);
        dispatcher.forward(request, response);
    }

    private boolean doWebDavDirectory(HttpServletResponse response, ArtifactoryRequest artifactoryRequest)
            throws IOException {
        log.debug("Serving a webdav directory request.");
        //Return a webdav folder get response
        String name = artifactoryRequest.getName();
        response.getWriter().write(name);
        return true;
    }

    private ArtifactoryContext getContext() {
        return ContextHelper.get();
    }

    private WebdavService getWebdavService() {
        return getContext().beanForType(WebdavService.class);
    }

    private DownloadService getDownloadService() {
        return getContext().beanForType(DownloadService.class);
    }

    private UploadService getUploadEngine() {
        return getContext().beanForType(UploadService.class);
    }

    private static String requestDebugString(HttpServletRequest request) {
        String queryString = request.getQueryString();
        String str = request.getMethod() + " (" + new HttpAuthenticationDetails(request).getRemoteAddress() + ") " +
                RequestUtils.getServletPathFromRequest(request) + (queryString != null ? queryString : "");
        return str;
    }

    private static class InnerRequestWrapper extends HttpServletRequestWrapper {
        private final boolean wicketRequest;

        public InnerRequestWrapper(HttpServletRequest request, boolean wicketRequest) {
            super(request);
            this.wicketRequest = wicketRequest;
        }

        @Override
        public Object getAttribute(String name) {
            if ("javax.servlet.forward.servlet_path".equals(name)) {
                return null;
            } else {
                return super.getAttribute(name);
            }
        }

        @Override
        public String getContextPath() {
            return super.getContextPath();
        }

        @Override
        public String getServletPath() {
            RepoPath removedRepoPath = (RepoPath) getAttribute(ATTR_ARTIFACTORY_REMOVED_REPOSITORY_PATH);
            if (wicketRequest) {
                //All wicket request that come after direct repository
                //browsing need to have the repo+path stripped
                return "/" + HttpUtils.WEBAPP_URL_PATH_PREFIX + "/";
            } else if (removedRepoPath != null) {
                //After login redirection
                return "/" + removedRepoPath.getRepoKey() + "/" + removedRepoPath.getPath();
            } else {
                return super.getServletPath();
            }
        }
    }

    private boolean redirectLegacyMetadataRequest(HttpServletRequest request, HttpServletResponse response,
            ArtifactoryRequest artifactoryRequest) throws IOException {
        // redirect to the appropriate REST api for legacy metadata requests
        // for example '/path/to/item:properties' is redirected to 'api/storage/path/to/item?propertiesXml'
        String requestPath = artifactoryRequest.getPath();
        if (NamingUtils.isProperties(requestPath)) {
            RepoPath repoPath = artifactoryRequest.getRepoPath();
            log.debug("Deprecated metadata download detected: {}", request.getRequestURL());
            String location = HttpUtils.getServletContextUrl(request) +
                    "/api/storage/" + repoPath.getRepoKey() + "/" +
                    NamingUtils.stripMetadataFromPath(repoPath.getPath()) + "?" +
                    NamingUtils.getMetadataName(artifactoryRequest.getPath()) + "Xml";
            RepoRequests.logToContext("Redirecting to path '%s'", location);
            response.sendRedirect(HttpUtils.encodeQuery(location));
            return true;
        }
        return false;
    }
}