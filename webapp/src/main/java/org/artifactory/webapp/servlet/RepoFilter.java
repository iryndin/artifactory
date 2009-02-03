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
import org.artifactory.engine.DownloadEngine;
import org.artifactory.engine.UploadEngine;
import org.artifactory.jcr.JcrHelper;
import org.artifactory.repo.exception.FileExpectedException;
import org.artifactory.request.ArtifactoryRequest;
import org.artifactory.request.ArtifactoryResponse;
import org.artifactory.request.HttpArtifactoryRequest;
import org.artifactory.request.HttpArtifactoryResponse;
import org.artifactory.security.RepoPath;
import org.artifactory.spring.ArtifactoryContext;
import org.artifactory.webapp.webdav.WebdavHelper;
import org.artifactory.webapp.wicket.browse.SimpleRepoBrowserPage;
import org.artifactory.webapp.wicket.utils.ServletUtils;

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

public class RepoFilter extends ArtifactoryFilter {
    @SuppressWarnings({"UnusedDeclaration"})
    private static final org.apache.log4j.Logger LOGGER = Logger.getLogger(RepoFilter.class);

    private static DownloadEngine downloadEngine;
    private static UploadEngine uploadEngine;
    public static final String ATTR_ARTIFACTORY_REPOSITORY_PATH = "artifactory.repository_path";
    public static final String ATTR_ARTIFACTORY_REMOVED_REPOSITORY_PATH =
            "artifactory.removed_repository_path";

    @SuppressWarnings({"unchecked"})
    public void init(FilterConfig filterConfig) throws ServletException {
        super.init(filterConfig);
        ArtifactoryContext context = getContext();
        downloadEngine = new DownloadEngine(context);
        uploadEngine = new UploadEngine(context);
    }

    @SuppressWarnings({"CaughtExceptionImmediatelyRethrown", "OverlyComplexMethod"})
    public void doFilterInternal(ServletRequest req, ServletResponse resp, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) resp;
        final String servletPath = request.getServletPath();
        String method = request.getMethod();
        if (emptyOrRoot(servletPath) && "get".equalsIgnoreCase(method)) {
            //We were called with an empty path - redirect to the app main page
            response.sendRedirect("./" + WEBAPP_URL_PATH_PREFIX);
            return;
        }
        ArtifactoryContext context = getContext();
        JcrHelper jcr = context.getCentralConfig().getJcr();
        jcr.bindSession();
        try {
            if (isRepoRequest(servletPath)) {
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
                    final boolean wicketRequest = ServletUtils.isWicketRequest(request);
                    @SuppressWarnings("deprecation")
                    HttpServletRequestWrapper requestWrapper =
                            new HttpServletRequestWrapper(request) {
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
                            };
                    //Expose the artifactory repository path as a request attribute
                    RepoPath repoPath = artifactoryRequest.getRepoPath();
                    request.setAttribute(ATTR_ARTIFACTORY_REPOSITORY_PATH, repoPath);
                    dispatcher.forward(requestWrapper, response);
                } else if ("get".equalsIgnoreCase(method) || "head".equalsIgnoreCase(method)) {
                    //We expect either a url with the repo prefix and an optional repo-key@repo
                    try {
                        downloadEngine.process(artifactoryRequest, artifactoryResponse);
                    } catch (FileExpectedException e) {
                        //Dispatch a new directory browsing request
                        RepoPath repoPath = e.getRepoPath();
                        response.sendRedirect(ServletUtils.getServletContextUrl(request) +
                                "/" + repoPath.getRepoKey() + "/" + repoPath.getPath() + "/");
                    }
                } else if ("put".equalsIgnoreCase(method)) {
                    //We expect a url with the repo prefix and a mandatory repo-key@repo
                    try {
                        uploadEngine.process(artifactoryRequest, artifactoryResponse);
                    } catch (Exception e) {
                        LOGGER.error("Upload request failed", e);
                        artifactoryResponse.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    }
                } else if ("propfind".equalsIgnoreCase(method)) {
                    new WebdavHelper(artifactoryRequest, artifactoryResponse).handlePropfind();
                } else if ("mkcol".equalsIgnoreCase(method)) {
                    new WebdavHelper(artifactoryRequest, artifactoryResponse).handleMkcol();
                } else if ("delete".equalsIgnoreCase(method)) {
                    new WebdavHelper(artifactoryRequest, artifactoryResponse).handleDelete();
                } else if ("options".equalsIgnoreCase(method)) {
                    new WebdavHelper(artifactoryRequest, artifactoryResponse).handleOptions();
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
        } catch (IOException ioe) {
            jcr.unbindSession(false);
            throw ioe;
        } finally {
            jcr.unbindSession(true);
        }
    }

    private boolean emptyOrRoot(String path) {
        return path == null || path.equals("/") || path.length() == 0;
    }
}