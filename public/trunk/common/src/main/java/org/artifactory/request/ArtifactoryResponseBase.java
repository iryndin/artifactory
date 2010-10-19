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

package org.artifactory.request;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.io.IOUtils;
import org.artifactory.api.request.ArtifactoryResponse;
import org.artifactory.common.StatusHolder;
import org.artifactory.log.LoggerFactory;
import org.artifactory.util.ExceptionUtils;
import org.artifactory.util.HttpUtils;
import org.artifactory.util.LoggingUtils;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public abstract class ArtifactoryResponseBase implements ArtifactoryResponse {
    private static final Logger log = LoggerFactory.getLogger(ArtifactoryResponseBase.class);

    private State state = State.UNSET;
    private int status = HttpStatus.SC_OK;
    private Exception exception;
    private long contentLength = -1;

    public void sendStream(InputStream is) throws IOException {
        OutputStream os = getOutputStream();
        if (state == State.UNSET) {
            state = State.SUCCESS;
        }
        try {
            long bytesCopied = IOUtils.copyLarge(is, os);
            if (bytesCopied == 0) {
                log.warn("Zero bytes sent to client.");
            } else {
                long expectedLength = getContentLength();
                if (expectedLength > 0 && bytesCopied != expectedLength) {
                    log.warn("Actual bytes sent to client ({}) are different than expected ({}).", bytesCopied,
                            expectedLength);
                } else {
                    log.debug("{} bytes sent to client.", bytesCopied);
                }
            }
            sendSuccess();
        } catch (Exception e) {
            exception = e;
            sendInternalError(e, log);
        } finally {
            IOUtils.closeQuietly(os);
            IOUtils.closeQuietly(is);
        }
    }

    public void setStatus(int status) {
        this.status = status;
        if (HttpUtils.isSuccessfulResponseCode(status) && state == State.UNSET) {
            state = State.SUCCESS;
        }
    }

    public int getStatus() {
        return status;
    }

    public void sendSuccess() {
        //Update the current status
        setStatus(status);
        if (isSuccessful()) {
            flush();
        } else {
            log.error("Could not send success. Exiting status: {}.", status);
            if (log.isDebugEnabled()) {
                log.debug("Could not send success.", new Throwable());
            }
        }
    }

    public void sendError(int statusCode, String reason, Logger logger) throws IOException {
        String msg = makeDebugMessage(statusCode, reason);
        if (statusCode == HttpStatus.SC_NOT_FOUND || statusCode == HttpStatus.SC_NOT_MODIFIED) {
            logger.debug(msg);
        } else {
            LoggingUtils.warnOrDebug(logger, msg);
        }
        state = State.FAILURE;
        this.status = statusCode;
        sendErrorInternal(statusCode, reason);
    }

    public void sendError(StatusHolder statusHolder) throws IOException {
        sendError(statusHolder.getStatusCode(), statusHolder.getStatusMsg(), log);
    }

    public void sendInternalError(Exception exception, Logger logger) throws IOException {
        Throwable ioException = ExceptionUtils.getCauseOfTypes(exception, IOException.class);
        String reason;
        if (ioException != null) {
            status = HttpStatus.SC_NOT_FOUND;
            reason = ioException.getMessage();
            logger.debug(makeDebugMessage(status, reason));
        } else {
            status = HttpStatus.SC_INTERNAL_SERVER_ERROR;
            reason = exception.getMessage();
            String message = makeDebugMessage(status, reason);
            logger.debug(makeDebugMessage(status, reason), exception);
            logger.error(message);
        }
        state = State.FAILURE;
        sendErrorInternal(status, reason);
    }

    public boolean isSuccessful() {
        return state == State.SUCCESS;
    }

    public Exception getException() {
        return exception;
    }

    public void setException(Exception exception) {
        this.state = State.FAILURE;
        this.exception = exception;
    }

    public long getContentLength() {
        return contentLength;
    }

    public boolean isContentLengthSet() {
        return contentLength != -1;
    }

    public void setContentLength(long length) {
        //Cache the content length locally
        this.contentLength = length;
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