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

package org.artifactory.webapp.servlet;

import org.artifactory.api.context.ArtifactoryContext;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.api.repo.RepoPath;
import org.artifactory.api.repo.exception.FileExpectedException;
import org.artifactory.api.request.ArtifactoryRequest;
import org.artifactory.api.request.ArtifactoryResponse;
import org.artifactory.api.request.DownloadService;
import org.artifactory.api.request.UploadService;
import org.artifactory.api.webdav.WebdavService;
import org.artifactory.log.LoggerFactory;
import org.artifactory.security.HttpAuthenticationDetails;
import org.artifactory.util.HttpUtils;
import org.artifactory.util.PathUtils;
import org.artifactory.webapp.wicket.page.browse.simplebrowser.SimpleRepoBrowserPage;
import org.slf4j.Logger;

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
    public static final String ATTR_ARTIFACTORY_REMOVED_REPOSITORY_PATH =
            "artifactory.removed_repository_path";

    @Override
    public void initLater(FilterConfig filterConfig) throws ServletException {
        String nonUiPathPrefixes = filterConfig.getInitParameter("nonUiPathPrefixes");
        String uiPathPrefixes = filterConfig.getInitParameter("UiPathPrefixes");
        List<String> nonUiPrefixes = PathUtils.delimitedListToStringList(nonUiPathPrefixes, ",");
        RequestUtils.setNonUiPathPrefixes(nonUiPrefixes);
        List<String> uiPrefixes = PathUtils.delimitedListToStringList(uiPathPrefixes, ",");
        uiPrefixes.add(RequestUtils.WEBAPP_URL_PATH_PREFIX);
        RequestUtils.setUiPathPrefixes(uiPrefixes);
    }

    public void destroy() {
    }

    public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) resp;
        final String servletPath = RequestUtils.getServletPathFromRequest(request);
        execute(chain, request, response, servletPath);
    }

    @SuppressWarnings({"OverlyComplexMethod", "StringEquality"})
    private void execute(FilterChain chain, final HttpServletRequest request, HttpServletResponse response,
            String servletPath) throws IOException, ServletException {
        if (log.isDebugEnabled()) {
            log.debug("Entering request {}.", requestDebugString(request));
        }
        String method = request.getMethod().toLowerCase().intern();
        if (RequestUtils.isRepoRequest(servletPath)) {
            //Handle upload and download requests
            ArtifactoryRequest artifactoryRequest = new HttpArtifactoryRequest(request);
            ArtifactoryResponse artifactoryResponse = new HttpArtifactoryResponse(response);
            if ("get" == method && servletPath.endsWith("/")) {
                log.debug("Serving a directory get request.");
                //Directory request
                boolean webdav = artifactoryRequest.isWebdav();
                if (webdav) {
                    log.debug("Serving a webdav directory request.");
                    //Return a webdav folder get response
                    String name = artifactoryRequest.getName();
                    response.getWriter().write(name);
                    return;
                }
                //Dispatch repository directory browsing request
                log.debug("Forwarding internally to a directory browsing request.");
                RequestDispatcher dispatcher = request.getRequestDispatcher(
                        "/" + RequestUtils.WEBAPP_URL_PATH_PREFIX + "/" + SimpleRepoBrowserPage.PATH);
                //Remove the forwarding URL (repo+path) as this is used by wicket to build
                //a relative path, which does not make sense in this case
                final boolean wicketRequest = RequestUtils.isWicketRequest(request);
                HttpServletRequestWrapper requestWrapper = new InnerRequestWrapper(request, wicketRequest);
                //Expose the artifactory repository path as a request attribute
                RepoPath repoPath = artifactoryRequest.getRepoPath();
                request.setAttribute(ATTR_ARTIFACTORY_REPOSITORY_PATH, repoPath);
                dispatcher.forward(requestWrapper, response);
            } else if ("get" == method || "head" == method) {
                //We expect either a url with the repo prefix and an optional repo-key@repo
                try {
                    log.debug("Serving a download or info request.");
                    getDownloadEngine().process(artifactoryRequest, artifactoryResponse);
                } catch (FileExpectedException e) {
                    //For get send redirect, for head return found
                    if ("get" == method) {
                        log.debug("Redirecting a directory browsing request.");
                        //Dispatch a new directory browsing request
                        RepoPath repoPath = e.getRepoPath();
                        response.sendRedirect(HttpUtils.getServletContextUrl(request) +
                                "/" + repoPath.getRepoKey() + "/" + repoPath.getPath() +
                                (repoPath.getPath().length() > 0 ? "/" : ""));
                    } else {
                        log.debug("Serving a directory head request.");
                    }
                }
            } else if ("put" == method) {
                //We expect a url with the repo prefix and a mandatory repo-key@repo
                try {
                    log.debug("Serving an upload request.");
                    getUploadEngine().process(artifactoryRequest, artifactoryResponse);
                } catch (Exception e) {
                    log.debug("Upload request of {} failed due to {}",
                            artifactoryRequest.getRepoPath(), e.getMessage());
                    artifactoryResponse.sendInternalError(e, log);
                }
            } else if ("propfind" == method) {
                getWebdavService().handlePropfind(artifactoryRequest, artifactoryResponse);
            } else if ("mkcol" == method) {
                getWebdavService().handleMkcol(artifactoryRequest, artifactoryResponse);
            } else if ("delete" == method) {
                getWebdavService().handleDelete(artifactoryRequest, artifactoryResponse);
            } else if ("options" == method) {
                getWebdavService().handleOptions(artifactoryResponse);
            } else if ("move" == method) {
                getWebdavService().handleMove(artifactoryRequest, artifactoryResponse);
            } else {
                response.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
                log.info("Received unsupported request method: {} from:{ }.", method, request.getRemoteAddr());
            }

            //int userLastAccessUpdatesResolutionSecs = ConstantValues.userLastAccessUpdatesResolutionSecs.getInt();
            //if (userLastAccessUpdatesResolutionSecs > 0) {
            //    boolean authenticatedUser = authorizationService.isAuthenticated();
            //    boolean anonymousUser = authorizationService.isAnonymous();
            //    if (authenticatedUser && !anonymousUser) {
            //        String currentUser = authorizationService.currentUsername();
            //        //Update the user's current login info in the database
            //        String remoteAddress = HttpUtils.getRemoteClientAddress(request);
            //        securityService.updateUserLastAccess(currentUser, remoteAddress, System.currentTimeMillis(),
            //                userLastAccessUpdatesResolutionSecs * 1000L);
            //    }
            //}
        } else if (!response.isCommitted()) {
            // Webdav request not on repository, return 405
            if (RequestUtils.isWebdavRequest(request)) {
                response.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
                if (log.isDebugEnabled()) {
                    log.debug("Received webdav request on " + servletPath + " which is not a repository!\n" +
                            "Returning " + HttpServletResponse.SC_METHOD_NOT_ALLOWED);
                }
            } else {
                chain.doFilter(request, response);
            }
        }
        if (log.isDebugEnabled()) {
            log.debug("Exiting request " + requestDebugString(request));
        }
    }

    private ArtifactoryContext getContext() {
        return ContextHelper.get();
    }

    private WebdavService getWebdavService() {
        return getContext().beanForType(WebdavService.class);
    }

    private DownloadService getDownloadEngine() {
        return getContext().beanForType(DownloadService.class);
    }

    private UploadService getUploadEngine() {
        return getContext().beanForType(UploadService.class);
    }

    private static String requestDebugString(HttpServletRequest request) {
        String queryString = request.getQueryString();
        String str = request.getMethod() + " (" + new HttpAuthenticationDetails(request).getRemoteAddress() + ") " +
                RequestUtils.getServletPathFromRequest(request) +
                (queryString != null ? queryString : "");
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
                return "/" + RequestUtils.WEBAPP_URL_PATH_PREFIX + "/";
            } else if (removedRepoPath != null) {
                //After login redirection
                return "/" + removedRepoPath.getRepoKey() + "/" + removedRepoPath.getPath();
            } else {
                return super.getServletPath();
            }
        }
    }
}