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
package org.artifactory.webapp.servlet;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.io.IOUtils;
import org.artifactory.api.request.ArtifactoryResponse;
import org.artifactory.util.ExceptionUtils;
import org.artifactory.util.LoggingUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public abstract class ArtifactoryResponseBase implements ArtifactoryResponse {
    private static final Logger log = LoggerFactory.getLogger(ArtifactoryResponseBase.class);

    private Status status = Status.UNSET;
    private Exception exception;

    public void sendStream(InputStream is) throws IOException {
        OutputStream os = getOutputStream();
        if (status == Status.UNSET) {
            status = Status.SUCCESS;
        }
        try {
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

    public void sendFile(File file) throws IOException {
        InputStream is = new FileInputStream(file);
        sendStream(is);
    }

    public void sendInternalError(Exception exception, Logger logger) throws IOException {
        Throwable ioException = ExceptionUtils.getCauseOfTypes(exception, IOException.class);
        int statusCode;
        String reason;
        if (ioException != null) {
            statusCode = HttpStatus.SC_NOT_FOUND;
            reason = ioException.getMessage();
            LoggingUtils.warnOrDebug(logger, makeDebugMessage(statusCode, reason));
        } else {
            statusCode = HttpStatus.SC_INTERNAL_SERVER_ERROR;
            reason = exception.getMessage();
            logger.error(makeDebugMessage(statusCode, reason), exception);
        }
        status = Status.FAILURE;
        sendErrorInternal(statusCode, reason);
    }

    public void sendError(int statusCode, String reason, Logger logger) throws IOException {
        String msg = makeDebugMessage(statusCode, reason);
        if (statusCode == HttpStatus.SC_NOT_FOUND || statusCode == HttpStatus.SC_NOT_MODIFIED) {
            if (logger.isDebugEnabled()) {
                logger.debug(msg);
            }
        } else {
            LoggingUtils.warnOrDebug(logger, msg);
        }
        status = Status.FAILURE;
        sendErrorInternal(statusCode, reason);
    }

    public boolean isSuccessful() {
        return status == Status.SUCCESS;
    }

    public Exception getException() {
        return exception;
    }

    public void setException(Exception exception) {
        this.status = Status.FAILURE;
        this.exception = exception;
    }

    protected abstract void sendErrorInternal(int code, String reason) throws IOException;

    private static String makeDebugMessage(int statusCode, String reason) {
        StringBuilder builder = new StringBuilder("Sending HTTP error code ").append(statusCode);
        if (reason != null) {
            builder.append(": ").append(reason);
        }
        return builder.toString();
    }
}