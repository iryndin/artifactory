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

import java.io.Serializable;

/**
 * @author freds
 * @date Sep 25, 2008
 */
public class StatusEntry implements Serializable {
    private final int statusCode;
    private final boolean error;
    private final String statusMessage;
    private final Throwable exception;

    public StatusEntry(int statusCode, String statusMessage) {
        this(statusCode, false, statusMessage, null);
    }

    public StatusEntry(int statusCode, String statusMessage, Throwable exception) {
        this(statusCode, true, statusMessage, exception);
    }

    public StatusEntry(int statusCode, boolean error, String statusMessage, Throwable exception) {
        this.statusCode = statusCode;
        this.error = error;
        this.statusMessage = statusMessage;
        this.exception = exception;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public Throwable getException() {
        return exception;
    }

    public boolean isError() {
        return error || exception != null;
    }

    public String toString() {
        return "StatusMessage{" +
                "statusCode=" + statusCode +
                ", error=" + error +
                ", statusMsg='" + statusMessage + '\'' +
                ", exception=" + exception +
                '}';
    }
}
