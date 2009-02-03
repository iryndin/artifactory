package org.artifactory.test;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.io.IOUtils;
import org.artifactory.api.request.ArtifactoryResponse;

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
    private long lastModified;
    private int contentLength;
    private String contentType;
    private int status;
    private Success success = Success.unset;
    private Exception exception = null;

    private Map<String, String> headers = new HashMap<String, String>();

    public void setException(Exception exception) {
        this.success = Success.failure;
        this.exception = exception;
    }

    public void sendError(int statusCode) throws IOException {
        success = Success.failure;
    }

    public void sendStream(InputStream is) throws IOException {
        OutputStream os = getOutputStream();
        if (success == Success.unset) {
            success = Success.success;
        }
        try {
            // TODO: do we need this?
            IOUtils.copy(is, os);
            sendOk();
        } catch (Exception e) {
            exception = e;
            sendError(HttpStatus.SC_INTERNAL_SERVER_ERROR);
        } finally {
            IOUtils.closeQuietly(os);
            IOUtils.closeQuietly(is);
        }
    }

    public void sendFile(File targetFile) throws IOException {
        InputStream is = new FileInputStream(targetFile);
        sendStream(is);
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
        return success == Success.success || status == HttpStatus.SC_OK;
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
