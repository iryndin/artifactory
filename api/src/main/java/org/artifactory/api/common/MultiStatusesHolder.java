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

import java.util.ArrayList;
import java.util.List;

/**
 * @author freds
 * @date Sep 25, 2008
 */
public class MultiStatusesHolder extends StatusHolder {
    private final List<StatusEntry> statusEntries = new ArrayList<StatusEntry>();
    private final List<Object> callbacks = new ArrayList<Object>();
    private StatusEntry lastError = null;

    @Override
    protected void addStatus(String statusMsg, int statusCode, Logger logger, boolean debug) {
        super.addStatus(statusMsg, statusCode, logger, debug);
        addStatusEntry();
    }

    @Override
    protected void addError(String statusMsg, int statusCode, Throwable throwable, Logger logger,
            boolean warn) {
        super.addError(statusMsg, statusCode, throwable, logger, warn);
        addStatusEntry();
    }

    private void addStatusEntry() {
        final StatusEntry entry = getStatusEntry();
        if (entry.isError()) {
            lastError = entry;
        }
        statusEntries.add(entry);
    }

    @Override
    public void setCallback(Object callback) {
        super.setCallback(callback);
        callbacks.add(callback);
    }

    @Override
    public boolean isError() {
        return lastError != null;
    }

    @Override
    public Throwable getException() {
        return lastError != null ? lastError.getException() : null;
    }

    @Override
    public int getStatusCode() {
        if (lastError != null) {
            return lastError.getStatusCode();
        }
        return super.getStatusCode();
    }

    public List<StatusEntry> getStatusEntries() {
        return statusEntries;
    }

    public List<Object> getCallbacks() {
        return callbacks;
    }

    @Override
    public void reset() {
        super.reset();
        statusEntries.clear();
        callbacks.clear();
        lastError = null;
    }
}
