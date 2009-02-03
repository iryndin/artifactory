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
package org.artifactory.api.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.Serializable;

/**
 * Created by IntelliJ IDEA. User: yoav
 */
public class StatusHolder implements Serializable {
    private static final Logger log = LoggerFactory.getLogger(StatusHolder.class);

    protected static final String MSG_IDLE = "Idle.";
    public static final int CODE_OK = 200;
    public static final int CODE_INTERNAL_ERROR = 500;

    private boolean activateLogging;
    private StatusEntry statusEntry;
    private File callback;
    private StatusEntry lastError = null;
    private boolean failFast = false;
    private boolean verbose = false;

    public StatusHolder() {
        statusEntry = new StatusEntry(CODE_OK, StatusEntryLevel.DEBUG, MSG_IDLE, null);
        activateLogging = true;
    }

    public void setFailFast(boolean failFast) {
        this.failFast = failFast;
    }

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    public boolean isFailFast() {
        return failFast;
    }

    public boolean isVerbose() {
        return verbose;
    }

    public StatusEntry getStatusEntry() {
        return statusEntry;
    }

    public StatusEntry getLastError() {
        return lastError;
    }

    public final void setDebug(String statusMsg, Logger logger) {
        addStatus(statusMsg, CODE_OK, logger, true);
    }

    public final void setStatus(String statusMsg, Logger logger) {
        setStatus(CODE_OK, statusMsg, logger);
    }

    public final void setStatus(int statusCode, String statusMsg, Logger logger) {
        addStatus(statusMsg, statusCode, logger, false);
    }

    protected StatusEntry addStatus(String statusMsg, int statusCode, Logger logger,
            boolean debug) {
        StatusEntry result;
        if (debug) {
            result = new StatusEntry(statusCode, StatusEntryLevel.DEBUG, statusMsg, null);
        } else {
            result = new StatusEntry(statusCode, statusMsg);
        }
        if (activateLogging) {
            logEntry(result, logger);
        }
        statusEntry = result;
        return result;
    }

    public void setError(String statusMsg, Logger logger) {
        setError(statusMsg, CODE_INTERNAL_ERROR, null, logger);
    }

    public void setError(String statusMsg, int statusCode, Logger logger) {
        setError(statusMsg, statusCode, null, logger);
    }

    public void setError(String status, Throwable throwable, Logger logger) {
        setError(status, CODE_INTERNAL_ERROR, throwable, logger);
    }

    public void setError(String status, int statusCode, Throwable throwable) {
        setError(status, statusCode, throwable, null);
    }

    public void setError(String statusMsg, int statusCode, Throwable throwable, Logger logger) {
        addError(statusMsg, statusCode, throwable, logger, false);
    }

    public void setWarning(String statusMsg, Logger logger) {
        addError(statusMsg, CODE_INTERNAL_ERROR, null, logger, true);
    }

    protected StatusEntry addError(String statusMsg, int statusCode, Throwable throwable, Logger logger, boolean warn) {
        StatusEntry result;
        if (warn) {
            result = new StatusEntry(statusCode, StatusEntryLevel.WARNING, statusMsg, throwable);
        } else {
            result = new StatusEntry(statusCode, StatusEntryLevel.ERROR, statusMsg, throwable);
            lastError = result;
        }
        if (isActivateLogging()) {
            logEntry(result, logger);
        }
        statusEntry = result;
        if (!warn && isFailFast()) {
            if (throwable != null) {
                if (throwable instanceof RuntimeException) {
                    throw (RuntimeException) throwable;
                } else if (throwable instanceof Error) {
                    throw (Error) throwable;
                } else {
                    throw new RuntimeException("Fail fast exception for " + statusEntry.getStatusMessage(), throwable);
                }
            } else {
                throw new RuntimeException("Fail fast exception for " + statusEntry.getStatusMessage());
            }
        }
        return result;
    }

    @SuppressWarnings({"OverlyComplexMethod"})
    protected void logEntry(StatusEntry entry, Logger logger) {
        boolean isExternalLogActive = (logger != null);
        String statusMessage = entry.getStatusMessage();
        if (entry.isWarning()) {
            log.warn(statusMessage);
            if (isExternalLogActive && logger.isWarnEnabled()) {
                logger.warn(statusMessage);
            }
        } else if (entry.isError()) {
            Throwable throwable = entry.getException();
            if (isVerbose()) {
                log.error(statusMessage, throwable);
            } else {
                log.error(statusMessage);
            }
            if (isExternalLogActive && logger.isErrorEnabled()) {
                if (isVerbose()) {
                    logger.error(statusMessage, throwable);
                } else {
                    logger.error(statusMessage);
                }
            }
        } else if (entry.isDebug()) {
            if (isVerbose()) {
                log.debug(statusMessage);
            }
            if (isExternalLogActive && logger.isDebugEnabled()) {
                logger.debug(statusMessage);
            }
        } else {
            log.info(statusMessage);
            if (isExternalLogActive && logger.isInfoEnabled()) {
                logger.info(statusMessage);
            }
        }
    }

    public String getStatusMsg() {
        if (lastError != null) {
            return lastError.getStatusMessage();
        }
        return statusEntry.getStatusMessage();
    }

    public File getCallback() {
        return callback;
    }

    public void setCallback(File callback) {
        this.callback = callback;
    }

    public boolean isError() {
        return lastError != null;
    }

    public Throwable getException() {
        if (lastError != null) {
            return lastError.getException();
        }
        return statusEntry.getException();
    }

    public int getStatusCode() {
        if (lastError != null) {
            return lastError.getStatusCode();
        }
        return statusEntry.getStatusCode();
    }

    public boolean isActivateLogging() {
        return activateLogging;
    }

    public void setActivateLogging(boolean activateLogging) {
        this.activateLogging = activateLogging;
    }

    public void reset() {
        lastError = null;
        statusEntry = new StatusEntry(CODE_OK, StatusEntryLevel.DEBUG, MSG_IDLE, null);
        activateLogging = true;
    }

    @Override
    public String toString() {
        return "StatusHolder{" +
                "activateLogging=" + activateLogging +
                ", statusMessage=" + statusEntry +
                ", callback=" + callback +
                '}';
    }
}
