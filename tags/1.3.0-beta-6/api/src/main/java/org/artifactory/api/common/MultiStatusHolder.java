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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * @author freds
 * @date Sep 25, 2008
 */
public class MultiStatusHolder extends StatusHolder {
    private static final Logger log = LoggerFactory.getLogger(MultiStatusHolder.class);

    private final BlockingQueue<StatusEntry> statusEntries =
            new LinkedBlockingQueue<StatusEntry>(500);
    private final BlockingQueue<File> callbacks = new LinkedBlockingQueue<File>(100);

    @Override
    protected StatusEntry addStatus(String statusMsg, int statusCode, Logger logger,
            boolean debug) {
        StatusEntry entry = super.addStatus(statusMsg, statusCode, logger, debug);
        addStatusEntry(entry, logger);
        return entry;
    }

    @Override
    protected StatusEntry addError(String statusMsg, int statusCode, Throwable throwable,
            Logger logger,
            boolean warn) {
        StatusEntry entry = super.addError(statusMsg, statusCode, throwable, logger, warn);
        addStatusEntry(entry, logger);
        return entry;
    }

    private void addStatusEntry(StatusEntry entry, Logger logger) {
        if (statusEntries.remainingCapacity() < 3) {
            // No more space, log and throw to garbage
            StatusEntry oldEntry = statusEntries.poll();
            if (isActivateLogging()) {
                // Already logged so ignored
            } else {
                logEntry(oldEntry, logger);
            }
        }
        statusEntries.add(entry);
    }

    @Override
    public void setCallback(File callback) {
        super.setCallback(callback);
        if (callbacks.remainingCapacity() < 3) {
            // No more space, log and throw to garbage
            File file = callbacks.poll();
            log.error("No more space in list of file callbacks\n" +
                    "File " + file + " removed from list.\n" +
                    "Need to increase consumer speed!");
        }
        callbacks.add(callback);
    }

    @Override
    public void reset() {
        super.reset();
        statusEntries.clear();
        callbacks.clear();
    }

    @SuppressWarnings({"ToArrayCallWithZeroLengthArrayArgument"})
    public List<StatusEntry> getWarnings() {
        StatusEntry[] entries = statusEntries.toArray(new StatusEntry[0]);
        ArrayList<StatusEntry> result = new ArrayList<StatusEntry>();
        for (StatusEntry entry : entries) {
            if (entry.isWarning()) {
                result.add(entry);
            }
        }
        return result;
    }
}
