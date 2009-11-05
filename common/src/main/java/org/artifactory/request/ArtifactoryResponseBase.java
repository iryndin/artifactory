/*
 * This file is part of Artifactory.
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
import org.artifactory.api.common.StatusHolder;
import org.artifactory.api.request.ArtifactoryResponse;
import org.artifactory.log.LoggerFactory;
import org.artifactory.util.ExceptionUtils;
import org.artifactory.util.LoggingUtils;
import org.slf4j.Logger;

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
            int bytesCopied = IOUtils.copy(is, os);
            if (bytesCopied == 0) {
                log.warn("Zero bytes sent to client.");
            } else {
                int expectedLength = getContentLength();
                if (expectedLength > 0 && bytesCopied != expectedLength) {
                    log.warn("Actual bytes sent to client ({}) are different than expected ({}).", bytesCopied,
                            expectedLength);
                } else {
                    log.debug("{} bytes sent to client.", bytesCopied);
                }
            }
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
            logger.debug(makeDebugMessage(statusCode, reason));
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

    public void sendError(StatusHolder statusHolder) throws IOException {
        sendError(statusHolder.getStatusCode(), statusHolder.getStatusMsg(), log);
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