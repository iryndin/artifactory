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

import org.apache.commons.httpclient.HttpStatus;
import org.artifactory.api.context.ArtifactoryContext;
import org.artifactory.api.context.ArtifactoryContextThreadBinder;
import org.artifactory.api.request.ArtifactoryResponse;
import org.artifactory.common.ArtifactoryHome;
import org.artifactory.util.HttpUtils;
import org.artifactory.util.ResourceUtils;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class ArtifactoryFilter implements Filter {

    private FilterConfig filterConfig;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        this.filterConfig = filterConfig;
    }

    @Override
    public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain)
            throws IOException, ServletException {
        if (filterConfig.getServletContext()
                .getAttribute(DelayedInit.APPLICATION_CONTEXT_LOCK_KEY) != null) {
            response.setContentType("text/html");
            ((HttpServletResponse) response).setStatus(HttpStatus.SC_SERVICE_UNAVAILABLE);
            ServletOutputStream out = response.getOutputStream();
            ResourceUtils.copyResource("/startup.html", out, null, getClass());
            return;
        }
        try {
            bind();
            if (response instanceof HttpServletResponse) {
                HttpServletResponse httpResponse = (HttpServletResponse) response;
                if (!httpResponse.containsHeader("Server")) {
                    //Add the server header (curl -I http://localhost:8080/artifactory/)
                    httpResponse.setHeader("Server", HttpUtils.getArtifactoryUserAgent());
                }

                // set the Artifactory instance id header
                httpResponse.setHeader(ArtifactoryResponse.ARTIFACTORY_ID, HttpUtils.getHostId());
            }
            chain.doFilter(request, response);
        } finally {
            unbind();
        }
    }

    private void bind() {
        ServletContext servletContext = filterConfig.getServletContext();
        ArtifactoryContext context = RequestUtils.getArtifactoryContext(servletContext);
        ArtifactoryContextThreadBinder.bind(context);
        ArtifactoryHome.bind(context.getArtifactoryHome());
    }

    private void unbind() {
        ArtifactoryContextThreadBinder.unbind();
        ArtifactoryHome.unbind();
    }

    @Override
    public void destroy() {
        unbind();
    }
}