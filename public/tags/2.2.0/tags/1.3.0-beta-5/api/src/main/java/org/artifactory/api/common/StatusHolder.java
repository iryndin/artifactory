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

import org.apache.log4j.Logger;

import java.io.Serializable;

/**
 * Created by IntelliJ IDEA. User: yoav
 */
public class StatusHolder implements Serializable {
    @SuppressWarnings({"UNUSED_SYMBOL", "UnusedDeclaration"})
    private final static Logger LOGGER = Logger.getLogger(StatusHolder.class);

    protected static final String MSG_IDLE = "Idle.";
    protected static final int CODE_OK = 200;
    protected static final int CODE_INTERNAL_ERROR = 500;

    private boolean activateLogging;
    private StatusEntry statusEntry;
    private Object callback;

    public StatusHolder() {
        statusEntry = new StatusEntry(CODE_OK, false, MSG_IDLE, null);
        activateLogging = true;
    }

    public StatusEntry getStatusEntry() {
        return statusEntry;
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

    protected void addStatus(String statusMsg, int statusCode, Logger logger, boolean debug) {
        statusEntry = new StatusEntry(statusCode, statusMsg);
        if (logger == null) {
            logger = LOGGER;
        }
        if (activateLogging) {
            if (debug) {
                if (logger.isDebugEnabled()) {
                    logger.debug(statusMsg);
                }
            } else {
                if (logger.isInfoEnabled()) {
                    logger.info(statusMsg);
                }
            }
        }
    }

    public void setError(String statusMsg) {
        setError(statusMsg, CODE_INTERNAL_ERROR, null, null);
    }

    public void setError(String statusMsg, int statusCode) {
        setError(statusMsg, statusCode, null, null);
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

    protected void addError(String statusMsg, int statusCode, Throwable throwable, Logger logger,
            boolean warn) {
        statusEntry = new StatusEntry(statusCode, true, statusMsg, throwable);
        if (activateLogging) {
            if (logger == null) {
                logger = LOGGER;
            }
            if (warn) {
                logger.warn(statusMsg, throwable);
            } else if (logger.isDebugEnabled()) {
                logger.error(statusMsg, throwable);
            } else {
                if (throwable != null) {
                    logger.error(statusMsg + ": " + throwable.getMessage());
                } else {
                    logger.error(statusMsg);
                }
            }
        }
    }

    public String getStatusMsg() {
        return statusEntry.getStatusMessage();
    }

    public Object getCallback() {
        return callback;
    }

    public void setCallback(Object callback) {
        this.callback = callback;
    }

    public boolean isError() {
        return statusEntry.isError();
    }

    public Throwable getException() {
        return statusEntry.getException();
    }

    public int getStatusCode() {
        return statusEntry.getStatusCode();
    }

    public boolean isActivateLogging() {
        return activateLogging;
    }

    public void setActivateLogging(boolean activateLogging) {
        this.activateLogging = activateLogging;
    }

    public void reset() {
        statusEntry = new StatusEntry(CODE_OK, false, MSG_IDLE, null);
        activateLogging = true;
    }

    public String toString() {
        return "StatusHolder{" +
                "activateLogging=" + activateLogging +
                ", statusMessage=" + statusEntry +
                ", callback=" + callback +
                '}';
    }
}
