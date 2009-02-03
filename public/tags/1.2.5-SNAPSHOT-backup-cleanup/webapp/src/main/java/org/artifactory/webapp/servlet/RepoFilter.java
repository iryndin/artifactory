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
import org.artifactory.request.ArtifactoryRequest;
import org.artifactory.request.ArtifactoryResponse;
import org.artifactory.request.HttpArtifactoryRequest;
import org.artifactory.request.HttpArtifactoryResponse;
import org.artifactory.spring.ArtifactoryContext;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class RepoFilter extends ArtifactoryFilter {
    @SuppressWarnings({"UnusedDeclaration"})
    private static final org.apache.log4j.Logger LOGGER = Logger.getLogger(RepoFilter.class);

    private static DownloadEngine downloadEngine;
    private static UploadEngine uploadEngine;

    @SuppressWarnings({"unchecked"})
    public void init(FilterConfig filterConfig) throws ServletException {
        super.init(filterConfig);
        ArtifactoryContext context = getContext();
        downloadEngine = new DownloadEngine(context);
        uploadEngine = new UploadEngine(context);
    }

    public void doFilterInternal(ServletRequest req, ServletResponse resp, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) resp;
        final String servletPath = request.getServletPath();
        String method = request.getMethod();
        if (emptyOrRoot(servletPath) && "get".equalsIgnoreCase(method)) {
            //We were called with an empty path - redirect to the app main page
            response.sendRedirect("./" + WEBAPP_URL_PATH_PREFIX);
        } else {
            ArtifactoryContext context = getContext();
            JcrHelper jcr = context.getCentralConfig().getJcr();
            jcr.bindSession();
            try {
                if (isRepoRequest(servletPath)) {
                    //Handle upload and download requests
                    String prefix = getPathPrefix(servletPath);
                    if ("get".equalsIgnoreCase(method) || "head".equalsIgnoreCase(method)) {
                        //We expect either a url with the repo prefix and an optional repo-key@repo
                        handleDownloadRequest(request, response, prefix);
                    } else if ("put".equalsIgnoreCase(method)) {
                        //We expect a url with the repo prefix and a mandatory repo-key@repo
                        handleUploadRequest(request, response, prefix);
                    } else if ("mkcol".equalsIgnoreCase(method)) {
                        //Fake dummy group hierarchy (collection) create responses for webdav
                        response.setStatus(HttpServletResponse.SC_CREATED);
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
            } catch (RuntimeException rte) {
                jcr.unbindSession(false);
                throw rte;
            }
            jcr.unbindSession(true);
        }
    }

    private void handleUploadRequest(
            HttpServletRequest request, HttpServletResponse response, String prefix)
            throws IOException {
        ArtifactoryRequest artifactoryRequest = new HttpArtifactoryRequest(request, prefix);
        ArtifactoryResponse artifactoryResponse = new HttpArtifactoryResponse(response);
        try {
            uploadEngine.process(artifactoryRequest, artifactoryResponse);
        } catch (Exception e) {
            LOGGER.error("Upload request failed", e);
            artifactoryResponse.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    private void handleDownloadRequest(
            HttpServletRequest request, HttpServletResponse response, String prefix)
            throws IOException {
        ArtifactoryRequest artifactoryRequest = new HttpArtifactoryRequest(request, prefix);
        ArtifactoryResponse artifactoryResponse = new HttpArtifactoryResponse(response);
        downloadEngine.process(artifactoryRequest, artifactoryResponse);
    }

    private boolean emptyOrRoot(String path) {
        return path == null || path.equals("/") || path.length() == 0;
    }
}