package org.artifactory.webapp.servlet;

import org.apache.log4j.Logger;
import org.artifactory.engine.DownloadEngine;
import org.artifactory.engine.UploadEngine;
import org.artifactory.jcr.JcrHelper;
import org.artifactory.repo.CentralConfig;
import org.artifactory.request.ArtifactoryRequest;
import org.artifactory.request.ArtifactoryResponse;
import org.artifactory.request.HttpArtifactoryRequest;
import org.artifactory.request.HttpArtifactoryResponse;

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
        CentralConfig cc = getCc();
        downloadEngine = new DownloadEngine(cc);
        uploadEngine = new UploadEngine(cc);
    }

    /*
    TODO: [by ]
    - Undeploy!
    - Use wicket servlet filter
    - Scheduled repo cleanup by pattern
    - Control the log level
    - Do not blackout the whole client-side repo on xfer failure
    - Scheduled export to FS
    - Clean retrieval expiries from UI
    - Repo groups + support in URL
    */
    public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) resp;
        final String servletPath = request.getServletPath();
        if (emptyOrRoot(servletPath)) {
            //We were called with an empty path
            response.sendRedirect("./webapp");
        } else {
            CentralConfig cc = getCc();
            JcrHelper jcr = cc.getJcr();
            jcr.bindSession();
            try {
                if (isRepoRequest(servletPath)) {
                    //Handle upload and download requests
                    String method = request.getMethod();
                    String prefix = getPathPrefix(servletPath);
                    if ("put".equalsIgnoreCase(method)) {
                        //We expect a url with the repo prefix and a mandatory repo-key@repo
                        handleUploadRequest(request, response, prefix);
                    } else {
                        //We expect either a url with the repo prefix and an optional repo-key@repo
                        handleDownloadRequest(request, response, prefix);
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
            LOGGER.error(e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
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