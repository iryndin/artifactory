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
package org.artifactory.engine;

/*
 * Copyright 2003-2004 The Apache Software Foundation.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.commons.httpclient.HttpStatus;
import org.artifactory.request.ArtifactoryResponseBase;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;

/**
 * @author Ben Walding
 */
public class MockArtifactoryResponse extends ArtifactoryResponseBase {
    private long lastModified;
    private int statusCode;
    private int contentLength;
    private String contentType;
    private ByteArrayOutputStream outputStream;
    @SuppressWarnings({"FieldCanBeLocal"})
    private boolean commited;

    public int getContentLength() {
        return contentLength;
    }

    public void setContentLength(int contentLength) {
        this.contentLength = contentLength;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public long getLastModified() {
        return lastModified;
    }

    public void setLastModified(long lastModified) {
        this.lastModified = lastModified;
    }

    public void sendErrorInternal(int statusCode) {
        setStatusCode(statusCode);
    }

    public OutputStream getOutputStream() {
        outputStream = new ByteArrayOutputStream();
        return outputStream;
    }

    public PrintWriter getWriter() throws IOException {
        return null;
    }

    /**
     * If no content body (eg. head only request), returns null. If empty content body, returns
     * empty string.
     *
     * @return
     */
    public String getContent() {
        if (outputStream == null) {
            return null;
        }

        return outputStream.toString();
    }

    public void sendOk() {
        setStatusCode(HttpStatus.SC_OK);
    }

    public void setStatus(int status) {
    }

    public void setHeader(String header, String value) {
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getContentType() {
        return contentType;
    }


    public void setCommited(boolean commited) {
        this.commited = commited;
    }

    public boolean isCommitted() {
        return commited;
    }

    public void flush() {
    }
}