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
    private final StatusEntryLevel level;
    private final String statusMessage;
    private final Throwable exception;

    public StatusEntry(int statusCode, String statusMessage) {
        this(statusCode, StatusEntryLevel.INFO, statusMessage, null);
    }

    public StatusEntry(int statusCode, String statusMessage, Throwable exception) {
        this(statusCode, StatusEntryLevel.ERROR, statusMessage, exception);
    }

    public StatusEntry(int statusCode, StatusEntryLevel level, String statusMessage,
            Throwable exception) {
        if (level == null) {
            throw new IllegalArgumentException(
                    "Cannot create status entry '" + statusMessage + "' with null level");
        }
        this.statusCode = statusCode;
        this.level = level;
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

    public boolean isWarning() {
        return level.isWarning();
    }

    public boolean isError() {
        return level.isError() || exception != null;
    }

    public boolean isDebug() {
        return level.isDebug();
    }

    public String toString() {
        return "StatusMessage{" +
                "statusCode=" + statusCode +
                ", level=" + level.name() +
                ", statusMsg='" + statusMessage + '\'' +
                ", exception=" + exception +
                '}';
    }
}
