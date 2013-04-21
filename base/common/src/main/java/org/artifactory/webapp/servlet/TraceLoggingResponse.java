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

import com.google.common.collect.Lists;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.NullOutputStream;
import org.apache.commons.io.output.NullWriter;
import org.artifactory.api.request.ArtifactoryResponse;
import org.artifactory.common.StatusHolder;
import org.artifactory.util.HttpUtils;
import org.joda.time.format.ISODateTimeFormat;
import org.slf4j.Logger;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.util.List;

/**
 * A response wrapper that maintains but ignores the activities of the request process and collects the request trace
 * messages for later response. To be used in conjunction with {@link org.artifactory.request.RequestTraceLogger}
 *
 * @author Noam Y. Tenne
 */
public class TraceLoggingResponse implements ArtifactoryResponse {

    private final String threadName;
    private final String time;

    private Exception exception;
    private long length;
    private int statusCode;
    private ArtifactoryResponse artifactoryResponse;
    private List<String> logAggregator = Lists.newArrayList();

    public TraceLoggingResponse(ArtifactoryResponse artifactoryResponse) {
        threadName = Thread.currentThread().getName();
        time = ISODateTimeFormat.dateTime().print(System.currentTimeMillis());
        this.artifactoryResponse = artifactoryResponse;
    }

    @Override
    public void setException(Exception exception) {
        this.exception = exception;
    }

    @Override
    public boolean isError() {
        return ((statusCode > 0) && !HttpUtils.isSuccessfulResponseCode(statusCode)) || (exception != null);
    }

    @Override
    public void setLastModified(long lastModified) {
    }

    @Override
    public void setEtag(String etag) {
    }

    @Override
    public void setSha1(String sha1) {
    }

    @Override
    public void setMd5(String md5) {
    }

    @Override
    public void setContentLength(long length) {
        this.length = length;
    }

    @Override
    public long getContentLength() {
        return length;
    }

    @Override
    public boolean isContentLengthSet() {
        return length >= 0;
    }

    @Override
    public void setContentType(String contentType) {
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
        return new NullOutputStream();
    }

    @Override
    public Writer getWriter() throws IOException {
        return new NullWriter();
    }

    @Override
    public void sendInternalError(Exception exception, Logger logger) throws IOException {
        this.exception = exception;
    }

    @Override
    public void sendError(int statusCode, String reason, Logger logger) throws IOException {
        this.statusCode = statusCode;
    }

    @Override
    public void sendError(StatusHolder statusHolder) throws IOException {
        this.statusCode = statusHolder.getStatusCode();
    }

    @Override
    public void sendStream(InputStream is) throws IOException {
        if (statusCode <= 0) {
            statusCode = HttpStatus.SC_OK;
        }
    }

    @Override
    public void sendSuccess() {
        this.statusCode = HttpStatus.SC_OK;
    }

    @Override
    public int getStatus() {
        return statusCode;
    }

    @Override
    public void setStatus(int statusCode) {
        this.statusCode = statusCode;
    }

    @Override
    public void setHeader(String header, String value) {
    }

    @Override
    public boolean isCommitted() {
        return false;
    }

    @Override
    public boolean isSuccessful() {
        return ((statusCode <= 0) || HttpUtils.isSuccessfulResponseCode(statusCode)) && (exception == null);
    }

    @Override
    public void flush() {
    }

    @Override
    public Exception getException() {
        return exception;
    }

    @Override
    public void sendAuthorizationRequired(String message, String realm) throws IOException {
        statusCode = HttpStatus.SC_UNAUTHORIZED;
    }

    /**
     * Appends a request trace log message
     *
     * @param message Message to log
     */
    public void log(String message) {
        logAggregator.add(message);
    }

    /**
     * Writes the request info and messages to the response
     *
     * @param requestId   Request trace context ID
     * @param methodName  HTTP method name
     * @param username    Authenticated user name
     * @param requestPath Request repo path id
     * @throws IOException
     */
    public void sendResponse(String requestId, String methodName, String username, String requestPath)
            throws IOException {
        Writer writer = null;
        try {
            artifactoryResponse.setContentType(MediaType.TEXT_PLAIN.getType());
            writer = artifactoryResponse.getWriter();
            writer.append("Request ID: ").append(requestId).append("\n");
            writer.append("Repo Path ID: ").append(requestPath).append("\n");
            writer.append("Method Name: ").append(methodName).append("\n");
            writer.append("User: ").append(username).append("\n");
            writer.append("Time: ").append(time).append("\n");
            writer.append("Thread: ").append(threadName).append("\n");
            writer.flush();
            writer.append("Steps: ").append("\n");
            IOUtils.writeLines(logAggregator, null, writer);
            writer.flush();
            artifactoryResponse.sendSuccess();
        } finally {
            if (writer != null) {
                writer.close();
            }
        }
    }
}
