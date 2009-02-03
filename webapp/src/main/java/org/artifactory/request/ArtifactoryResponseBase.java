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
package org.artifactory.request;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.artifactory.api.request.ArtifactoryResponse;
import org.artifactory.utils.LoggingUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public abstract class ArtifactoryResponseBase implements ArtifactoryResponse {
    private static final Logger LOGGER =
            LogManager.getLogger(ArtifactoryResponseBase.class);

    private Success success = Success.unset;
    private Exception exception = null;

    public void sendStream(InputStream is) throws IOException {
        OutputStream os = getOutputStream();
        if (success == Success.unset) {
            success = Success.success;
        }
        try {
            IOUtils.copy(is, os);
            sendOk();
        } catch (Exception e) {
            exception = e;
            sendInternalError(e, LOGGER);
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
        int statusCode = HttpStatus.SC_INTERNAL_SERVER_ERROR;
        String reason = exception.getMessage();
        logger.error(makeDebugMessage(statusCode, reason), exception);
        success = Success.failure;
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
        success = Success.failure;
        sendErrorInternal(statusCode, reason);
    }

    public boolean isSuccessful() {
        return success == Success.success;
    }

    public Exception getException() {
        return exception;
    }

    public void setException(Exception exception) {
        this.success = Success.failure;
        this.exception = exception;
    }

    protected abstract void sendErrorInternal(int code, String reason) throws IOException;

    private static String makeDebugMessage(int statusCode, String reason) {
        StringBuilder builder = new StringBuilder("Sending HTTP error code ").append(statusCode);
        if (reason != null) {
            builder.append(":").append(reason);
        }
        return builder.toString();
    }
}