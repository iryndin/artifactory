package org.artifactory.test.internal;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.io.IOUtils;
import org.artifactory.api.request.ArtifactoryResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Yossi Shaul
 */
class ArtifactoryResponseStub implements ArtifactoryResponse {
    private static final Logger log = LoggerFactory.getLogger(ArtifactoryResponseStub.class);

    private long lastModified;
    private int contentLength;
    private String contentType;
    private int status;
    private Status success = Status.UNSET;
    private Exception exception = null;
    private String reason = null;

    private Map<String, String> headers = new HashMap<String, String>();

    public void setException(Exception exception) {
        this.success = Status.FAILURE;
        this.exception = exception;
    }

    public void sendInternalError(Exception exception, Logger logger) throws IOException {
        logger.error(exception.getMessage(), exception);
        sendError(HttpStatus.SC_INTERNAL_SERVER_ERROR, exception.getMessage(), logger);
    }

    public void sendError(int statusCode, String reason, Logger logger) throws IOException {
        status = statusCode;
        success = Status.FAILURE;
        this.reason = reason;
    }

    public void sendAuthorizationRequired(String message, String realm) throws IOException {
        // TODO
    }

    public void sendStream(InputStream is) throws IOException {
        OutputStream os = getOutputStream();
        if (success == Status.UNSET) {
            success = Status.SUCCESS;
        }
        try {
            // TODO: do we need this?
            IOUtils.copy(is, os);
            sendOk();
        } catch (Exception e) {
            exception = e;
            sendInternalError(e, log);
        } finally {
            IOUtils.closeQuietly(os);
            IOUtils.closeQuietly(is);
        }
    }

    public void sendFile(File targetFile) throws IOException {
        InputStream is = new FileInputStream(targetFile);
        sendStream(is);
    }

    public String getReason() {
        return reason;
    }

    public Exception getException() {
        return exception;
    }

    public void setLastModified(long lastModified) {
        this.lastModified = lastModified;
    }

    public void setContentLength(int length) {
        contentLength = length;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public OutputStream getOutputStream() throws IOException {
        return new ByteArrayOutputStream();
    }

    public PrintWriter getWriter() throws IOException {
        return new PrintWriter(new StringWriter());
    }

    public void sendOk() {
        setStatus(HttpStatus.SC_OK);
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public void setHeader(String header, String value) {
        headers.put(header, value);
    }

    public boolean isCommitted() {
        return false;
    }

    public void flush() {

    }

    public boolean isSuccessful() {
        return success == Status.SUCCESS || status == HttpStatus.SC_OK;
    }

    public long getLastModified() {
        return lastModified;
    }

    public int getContentLength() {
        return contentLength;
    }

    public String getContentType() {
        return contentType;
    }

    public int getStatus() {
        return status;
    }

    public String getHeaderValue(String header) {
        return headers.get(header);
    }
}
