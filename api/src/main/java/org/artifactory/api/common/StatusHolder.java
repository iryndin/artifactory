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

    private static final String MSG_IDLE = "Idle.";
    private static final int CODE_OK = 200;
    private static final int CODE_INTERNAL_ERROR = 500;

    private String statusMsg;
    private boolean error;
    private Throwable throwable;
    private boolean logging;
    private int statusCode;
    private Object callback;

    public StatusHolder() {
        reset();
    }

    public StatusHolder(int statusCode, String statusMsg) {
        this.statusCode = statusCode;
        this.statusMsg = statusMsg;
        this.error = true;
    }

    public String getStatusMsg() {
        return statusMsg;
    }

    public void setStatus(String statusMsg) {
        setStatus(statusMsg, CODE_OK);
    }

    public void setStatus(String statusMsg, int statusCode) {
        this.statusMsg = statusMsg;
        if (logging && LOGGER.isInfoEnabled()) {
            LOGGER.info(statusMsg);
        }
        this.statusCode = statusCode;
    }

    public void setError(String status) {
        setError(status, CODE_INTERNAL_ERROR, null, null);
    }

    public void setError(String status, int statusCode) {
        setError(status, statusCode, null, null);
    }

    public void setError(String status, Throwable throwable) {
        setError(status, 0, throwable, null);
    }

    public void setError(String status, Throwable throwable, Logger logger) {
        setError(status, 0, throwable, logger);
    }

    public void setError(String status, int statusCode, Throwable throwable) {
        setError(status, 0, throwable, null);
    }

    public void setError(String status, int statusCode, Throwable throwable, Logger logger) {
        this.error = true;
        this.throwable = throwable;
        this.statusCode = statusCode;
        this.statusMsg = status;
        if (logging) {
            if (logger == null) {
                logger = LOGGER;
            }
            if (logger.isDebugEnabled()) {
                LOGGER.error(status, throwable);
            } else {
                if (throwable != null) {
                    LOGGER.error(status + ": " + throwable.getMessage());
                } else {
                    LOGGER.error(status);
                }
            }
        }
    }

    public void setStatus(String status, Throwable th) {
        this.statusMsg = status;
        if (logging) {
            LOGGER.warn(status, th);
        }
    }

    public Object getCallback() {
        return callback;
    }

    public void setCallback(Object callback) {
        this.callback = callback;
    }

    public boolean isError() {
        return error;
    }

    public Throwable getThrowable() {
        return throwable;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public boolean isLogging() {
        return logging;
    }

    public void setLogging(boolean logging) {
        this.logging = logging;
    }

    public void reset() {
        statusMsg = MSG_IDLE;
        error = false;
        throwable = null;
        logging = true;
        statusCode = CODE_OK;
    }

    @Override
    public String toString() {
        return "StatusHolder{" +
                "statusMsg='" + statusMsg + '\'' +
                ", error=" + error +
                ", throwable=" + throwable +
                ", logging=" + logging +
                ", statusCode=" + statusCode +
                ", callback=" + callback +
                '}';
    }
}
