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
package org.artifactory.webapp.servlet;

import org.apache.log4j.Logger;
import org.artifactory.api.context.ArtifactoryContext;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.api.repo.RepoPath;
import org.artifactory.api.repo.exception.FileExpectedException;
import org.artifactory.api.request.ArtifactoryRequest;
import org.artifactory.api.request.ArtifactoryResponse;
import org.artifactory.api.request.DownloadService;
import org.artifactory.api.request.UploadService;
import org.artifactory.api.webdav.WebdavService;
import org.artifactory.utils.PathUtils;
import org.artifactory.webapp.request.HttpArtifactoryRequest;
import org.artifactory.webapp.request.HttpArtifactoryResponse;
import org.artifactory.webapp.wicket.browse.SimpleRepoBrowserPage;
import org.springframework.web.context.WebApplicationContext;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

public class RepoFilter implements Filter {
    @SuppressWarnings({"UnusedDeclaration"})
    private static final org.apache.log4j.Logger LOGGER = Logger.getLogger(RepoFilter.class);

    public static final String WEBAPP_URL_PATH_PREFIX = "webapp";
    public static final String ATTR_ARTIFACTORY_REPOSITORY_PATH = "artifactory.repository_path";
    public static final String ATTR_ARTIFACTORY_REMOVED_REPOSITORY_PATH =
            "artifactory.removed_repository_path";

    public void init(FilterConfig filterConfig) throws ServletException {
        ServletContext servletContext = filterConfig.getServletContext();
        ArtifactoryContext context = (ArtifactoryContext) servletContext
                .getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE);
        String nonUIPathPrefixes = filterConfig.getInitParameter("nonUIPathPrefixes");
        String uiPathPrefixes = filterConfig.getInitParameter("UIPathPrefixes");
        List<String> nonUiPrefixes =
                PathUtils.delimitedListToStringList(nonUIPathPrefixes, ",", "\r\n\f\t ");
        RequestUtils.addNonUiPathPrefixes(nonUiPrefixes);
        List<String> uiPrefixes =
                PathUtils.delimitedListToStringList(uiPathPrefixes, ",", "\r\n\f\t ");
        uiPrefixes.add(WEBAPP_URL_PATH_PREFIX);
        RequestUtils.addUiPathPrefixes(uiPrefixes);
        List<String> repoKeys = context.getRepositoryService().getAllRepoKeys();
        RequestUtils.addRepoPathPrefixes(repoKeys);
    }

    public void destroy() {
    }

    public void doFilter(ServletRequest req, ServletResponse resp,
            FilterChain chain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) resp;
        final String servletPath = request.getServletPath();
        String method = request.getMethod();
        if (emptyOrRoot(servletPath) && "get".equalsIgnoreCase(method)) {
            //We were called with an empty path - redirect to the app main page
            response.sendRedirect("./" + WEBAPP_URL_PATH_PREFIX);
            return;
        }
        execute(chain, request, response, servletPath, method);
    }

    private static boolean emptyOrRoot(String path) {
        return path == null || "/".equals(path) || path.length() == 0;
    }

    @SuppressWarnings({"OverlyComplexMethod"})
    private void execute(FilterChain chain, final HttpServletRequest request,
            HttpServletResponse response, String servletPath,
            String method) throws IOException, ServletException {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Entering request " + requestDebugString(request));
        }
        if (RequestUtils.isRepoRequest(servletPath)) {
            //Handle upload and download requests
            ArtifactoryRequest artifactoryRequest = new HttpArtifactoryRequest(request);
            ArtifactoryResponse artifactoryResponse = new HttpArtifactoryResponse(response);
            if ("get".equalsIgnoreCase(method) && servletPath.endsWith("/")) {
                //Directory request
                boolean webdav = artifactoryRequest.isWebdav();
                if (webdav) {
                    //Return a webdav folder get response
                    String name = artifactoryRequest.getName();
                    response.getWriter().write(name);
                    return;
                }
                //Dispatch repository directory browsing request
                RequestDispatcher dispatcher = request.getRequestDispatcher(
                        "/" + WEBAPP_URL_PATH_PREFIX + "/" + SimpleRepoBrowserPage.PATH);
                //Remove the forwarding URL (repo+path) as this is used by wicket to build
                //a relative path, which does not make sense in this case
                final boolean wicketRequest = RequestUtils.isWicketRequest(request);
                HttpServletRequestWrapper requestWrapper =
                        new InnerRequestWrapper(request, wicketRequest);
                //Expose the artifactory repository path as a request attribute
                RepoPath repoPath = artifactoryRequest.getRepoPath();
                request.setAttribute(ATTR_ARTIFACTORY_REPOSITORY_PATH, repoPath);
                dispatcher.forward(requestWrapper, response);
            } else if ("get".equalsIgnoreCase(method) || "head".equalsIgnoreCase(method)) {
                //We expect either a url with the repo prefix and an optional repo-key@repo
                try {
                    getDownloadEngine().process(artifactoryRequest, artifactoryResponse);
                } catch (FileExpectedException e) {
                    //Dispatch a new directory browsing request
                    RepoPath repoPath = e.getRepoPath();
                    response.sendRedirect(RequestUtils.getServletContextUrl(request) +
                            "/" + repoPath.getRepoKey() + "/" + repoPath.getPath() +
                            (repoPath.getPath().length() > 0 ? "/" : ""));
                }
            } else if ("put".equalsIgnoreCase(method)) {
                //We expect a url with the repo prefix and a mandatory repo-key@repo
                try {
                    getUploadEngine().process(artifactoryRequest, artifactoryResponse);
                } catch (Exception e) {
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("Upload request of " + artifactoryRequest.getRepoPath() +
                                " failed due to " + e.getMessage());
                    }
                    artifactoryResponse.sendInternalError(e, LOGGER);
                }
            } else if ("propfind".equalsIgnoreCase(method)) {
                getWebdavService().handlePropfind(artifactoryRequest, artifactoryResponse);
            } else if ("mkcol".equalsIgnoreCase(method)) {
                getWebdavService().handleMkcol(artifactoryRequest, artifactoryResponse);
            } else if ("delete".equalsIgnoreCase(method)) {
                getWebdavService().handleDelete(artifactoryRequest, artifactoryResponse);
            } else if ("options".equalsIgnoreCase(method)) {
                getWebdavService().handleOptions(artifactoryResponse);
            } else {
                response.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
                if (LOGGER.isInfoEnabled()) {
                    LOGGER.info("Received unsupported request method: " + method +
                            " from: " + request.getRemoteAddr() + ".");
                }
            }
        } else if (!response.isCommitted()) {
            chain.doFilter(request, response);
        }
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Exiting request " + requestDebugString(request));
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
        String str = request.hashCode() + ": " + request.getServletPath() +
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
            RepoPath removedRepoPath = (RepoPath) getAttribute(
                    ATTR_ARTIFACTORY_REMOVED_REPOSITORY_PATH);
            if (wicketRequest) {
                //All wicket request that come after direct repository
                //browsing need to have the repo+path stripped
                return "/" + WEBAPP_URL_PATH_PREFIX + "/";
            } else if (removedRepoPath != null) {
                //After login redirection
                return "/" + removedRepoPath.getRepoKey() + "/" +
                        removedRepoPath.getPath();
            } else {
                return super.getServletPath();
            }
        }
    }
}