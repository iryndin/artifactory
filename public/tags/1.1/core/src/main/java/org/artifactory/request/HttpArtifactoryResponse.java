package org.artifactory.request;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.log4j.Logger;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;

public class HttpArtifactoryResponse extends ArtifactoryResponseBase {
    private static final Logger LOGGER = Logger.getLogger(HttpArtifactoryResponse.class);

    private final HttpServletResponse response;

    public HttpArtifactoryResponse(HttpServletResponse response) {
        this.response = response;
    }

    public void setLastModified(long lastModified) {
        response.setDateHeader("Last-Modified", lastModified);
    }

    public void sendError(int statusCode) throws IOException {
        try {
            response.sendError(statusCode);
        } catch (IOException e) {
            throw e;
        } catch (Throwable t) {
            LOGGER.warn("Failed to send http error (" + t.getMessage() + "). ", t);
        }
    }

    public void setContentLength(int length) {
        response.setContentLength(length);
    }

    public OutputStream getOutputStream() throws IOException {
        return response.getOutputStream();
    }

    public void sendOk() {
        response.setStatus(HttpStatus.SC_OK);
        try {
            response.flushBuffer();
        } catch (IOException e) {
            LOGGER.warn("Failed to commit http response (" + e.getMessage() + ").", e);
        }
    }

    public void setContentType(String contentType) {
        response.setContentType(contentType);
    }

    public boolean isCommited() {
        return response.isCommitted();
    }
}