/*
 * This file is part of Artifactory.
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

import org.apache.commons.lang.StringUtils;
import org.artifactory.security.AuthenticationHelper;
import org.artifactory.security.HttpAuthenticationDetails;
import org.artifactory.traffic.RequestLogger;
import org.springframework.security.Authentication;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.IOException;

/**
 * A dedicated filter for the Reqeust Logger that sits after the ArtifactoryFilter
 *
 * @author Noam Tenne
 */
public class RequestFilter implements Filter {

    public void init(FilterConfig filterConfig) throws ServletException {
    }

    public void doFilter(final ServletRequest req, final ServletResponse resp, final FilterChain chain)
            throws IOException, ServletException {
        long start = System.currentTimeMillis();
        //Wrap the response
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) resp;
        CapturingHttpServletResponseWrapper responseWrapper = new CapturingHttpServletResponseWrapper(response);
        chain.doFilter(req, responseWrapper);

        Authentication authentication = AuthenticationHelper.getAuthentication();
        String remoteAddress = new HttpAuthenticationDetails(request).getRemoteAddress();
        String username = authentication == null ? "non_authenticated_user" : authentication.getName();
        String servletPath = RequestUtils.getServletPathFromRequest(request);
        String method = request.getMethod();
        int contentLength = 0;
        if ("get".equalsIgnoreCase(method)) {
            contentLength = responseWrapper.getContentLength();
        }
        if (("put".equalsIgnoreCase(method)) || ("post".equalsIgnoreCase(method))) {
            contentLength = request.getContentLength();
        }
        RequestLogger.request(remoteAddress, username, method, servletPath, request.getProtocol(),
                responseWrapper.getStatus(), contentLength, System.currentTimeMillis() - start);
    }

    public void destroy() {
    }

    /**
     * A custom response wrapper the helps capture the return code and the content length
     */
    private static class CapturingHttpServletResponseWrapper extends HttpServletResponseWrapper {
        private int status;
        private int contentLength;

        private String CONTENT_LENGTH_HEADER = "Content-Length";

        /**
         * Constructs a response adaptor wrapping the given response.
         *
         * @throws IllegalArgumentException if the response is null
         */
        public CapturingHttpServletResponseWrapper(HttpServletResponse response) {
            super(response);
            status = 200;
        }

        public int getStatus() {
            return status;
        }

        public int getContentLength() {
            return contentLength;
        }

        @Override
        public void addHeader(String name, String value) {
            super.addHeader(name, value);
            captureString(name, value);
        }

        @Override
        public void addIntHeader(String name, int value) {
            super.addIntHeader(name, value);
            captureInt(name, value);
        }

        @Override
        public void setHeader(String name, String value) {
            super.setHeader(name, value);
            captureString(name, value);
        }

        @Override
        public void setIntHeader(String name, int value) {
            super.setIntHeader(name, value);
            captureInt(name, value);
        }

        @Override
        public void setContentLength(int len) {
            contentLength = len;
            super.setContentLength(len);
        }

        @Override
        public void setStatus(int sc) {
            status = sc;
            super.setStatus(sc);
        }

        @Override
        public void setStatus(int sc, String sm) {
            status = sc;
            super.setStatus(sc, sm);
        }

        @Override
        public void sendError(int sc) throws IOException {
            status = sc;
            super.sendError(sc);
        }

        @Override
        public void sendError(int sc, String msg) throws IOException {
            status = sc;
            super.sendError(sc, msg);
        }

        private void captureString(String name, String value) {
            if (name.equals(CONTENT_LENGTH_HEADER) && StringUtils.isNumeric(value)) {
                contentLength = Integer.parseInt(value);
            }
        }

        private void captureInt(String name, int value) {
            captureString(name, value + "");
        }
    }
}