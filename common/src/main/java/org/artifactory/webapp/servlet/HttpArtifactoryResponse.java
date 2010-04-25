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

import org.apache.commons.httpclient.HttpStatus;
import org.artifactory.log.LoggerFactory;
import org.artifactory.request.ArtifactoryResponseBase;
import org.slf4j.Logger;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;

public class HttpArtifactoryResponse extends ArtifactoryResponseBase {
    private static final Logger log = LoggerFactory.getLogger(HttpArtifactoryResponse.class);

    private final HttpServletResponse response;

    public HttpArtifactoryResponse(HttpServletResponse response) {
        this.response = response;
    }

    public void setLastModified(long lastModified) {
        response.setDateHeader("Last-Modified", lastModified);
    }

    public void setEtag(String etag) {
        if (etag != null) {
            response.setHeader("ETag", etag);
        } else {
            log.debug("Could not register a null etag with the response.");
        }
    }

    @Override
    protected void sendErrorInternal(int statusCode, String reason) throws IOException {
        if (response.isCommitted()) {
            log.debug("Cannot send error " + statusCode +
                    (reason != null ? " (" + reason + ")" : "") +
                    ": response already committed.");
            return;
        }
        try {
            if (reason != null) {
                //Send a description of the reason in the body
                response.sendError(statusCode, reason);
            } else {
                response.sendError(statusCode);
            }
        } catch (IOException e) {
            throw e;
        } catch (Throwable t) {
            log.warn("Failed to send http error (" + t.getMessage() + ").", t);
        }
    }

    public void sendAuthorizationRequired(String message, String realm) throws IOException {
        try {
            response.addHeader("WWW-Authenticate", "Basic realm=\"" + realm + "\"");
            response.sendError(HttpStatus.SC_UNAUTHORIZED, message);
        } catch (IOException e) {
            throw e;
        } catch (IllegalStateException e) {
            log.warn("Failed to send http error (" + e.getMessage() + ").", e);
        } catch (Throwable t) {
            log.warn("Failed to send http error (" + t.getMessage() + ").", t);
        }
    }

    @Override
    public void setContentLength(int length) {
        super.setContentLength(length);
        response.setContentLength(length);
    }

    public OutputStream getOutputStream() throws IOException {
        return response.getOutputStream();
    }

    public PrintWriter getWriter() throws IOException {
        return response.getWriter();
    }

    @Override
    public void setStatus(int status) {
        super.setStatus(status);
        response.setStatus(status);
    }

    public void setHeader(String header, String value) {
        response.setHeader(header, value);
    }

    public void sendOk() {
        flush();
        setStatus(HttpStatus.SC_OK);
    }

    public void flush() {
        try {
            response.flushBuffer();
        } catch (IOException e) {
            String message = "Failed to commit http response (" + e.getMessage() + ").";
            log.warn(message);
            log.debug(message, e);
        }
    }

    public void setContentType(String contentType) {
        response.setContentType(contentType);
    }

    public boolean isCommitted() {
        return response.isCommitted();
    }
}