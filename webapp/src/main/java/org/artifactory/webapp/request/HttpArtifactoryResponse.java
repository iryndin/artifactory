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
package org.artifactory.webapp.request;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.log4j.Logger;
import org.artifactory.request.ArtifactoryResponseBase;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;

public class HttpArtifactoryResponse extends ArtifactoryResponseBase {
    private static final Logger LOGGER = Logger.getLogger(HttpArtifactoryResponse.class);

    private final HttpServletResponse response;

    public HttpArtifactoryResponse(HttpServletResponse response) {
        this.response = response;
    }

    public void setLastModified(long lastModified) {
        response.setDateHeader("Last-Modified", lastModified);
    }

    @Override
    public void sendErrorInternal(int statusCode) throws IOException {
        try {
            response.sendError(statusCode);
        } catch (IOException e) {
            throw e;
        } catch (IllegalStateException e) {
            LOGGER.warn("Failed to send http error (" + e.getMessage() + ").");
        } catch (Throwable t) {
            LOGGER.warn("Failed to send http error (" + t.getMessage() + ").", t);
        }
    }

    public void setContentLength(int length) {
        response.setContentLength(length);
    }

    public OutputStream getOutputStream() throws IOException {
        return response.getOutputStream();
    }

    public PrintWriter getWriter() throws IOException {
        return response.getWriter();
    }

    public void setStatus(int status) {
        response.setStatus(status);
    }

    public void setHeader(String header, String value) {
        response.setHeader(header, value);
    }

    public void sendOk() {
        setStatus(HttpStatus.SC_OK);
        flush();
    }

    public void flush() {
        try {
            response.flushBuffer();
        } catch (IOException e) {
            LOGGER.warn("Failed to commit http response (" + e.getMessage() + ").", e);
        }
    }

    public void setContentType(String contentType) {
        response.setContentType(contentType);
    }

    public boolean isCommitted() {
        return response.isCommitted();
    }
}