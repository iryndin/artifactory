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

package org.artifactory.api.common;

import org.artifactory.common.StatusEntry;
import org.artifactory.common.StatusEntryLevel;
import org.artifactory.log.LoggerFactory;
import org.slf4j.Logger;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * @author freds
 * @date Sep 25, 2008
 */
public class MultiStatusHolder extends BasicStatusHolder {
    private static final Logger log = LoggerFactory.getLogger(MultiStatusHolder.class);

    private final BlockingQueue<StatusEntry> statusEntries = new LinkedBlockingQueue<StatusEntry>();
    private final BlockingQueue<File> callbacks = new LinkedBlockingQueue<File>();

    @Override
    protected StatusEntry addStatus(String statusMsg, int statusCode, Logger logger, boolean debug) {
        StatusEntry entry = super.addStatus(statusMsg, statusCode, logger, debug);
        addStatusEntry(entry, logger);
        return entry;
    }

    @Override
    protected StatusEntry addError(String statusMsg, int statusCode, Throwable throwable, Logger logger, boolean warn) {
        StatusEntry entry = super.addError(statusMsg, statusCode, throwable, logger, warn);
        addStatusEntry(entry, logger);
        return entry;
    }

    private void addStatusEntry(StatusEntry entry, Logger logger) {
        if (statusEntries.remainingCapacity() < 3) {
            // No more space, log and throw to garbage
            StatusEntry oldEntry = statusEntries.poll();
            // If active logging is on, it's already logged so ignored
            if (oldEntry != null && !isActivateLogging()) {
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
            log.error("No more space in list of file callbacks\nFile " + file + " removed from list.\n" +
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

    public boolean hasErrors() {
        return isError();
    }

    public boolean hasWarnings() {
        return !getWarnings().isEmpty();
    }

    public List<StatusEntry> getAllEntries() {
        return getEntries(null);
    }


    public List<StatusEntry> getErrors() {
        return getEntries(StatusEntryLevel.ERROR);
    }

    public List<StatusEntry> getWarnings() {
        return getEntries(StatusEntryLevel.WARNING);
    }

    @SuppressWarnings({"ToArrayCallWithZeroLengthArrayArgument"})
    public List<StatusEntry> getEntries(StatusEntryLevel level) {
        StatusEntry[] entries = statusEntries.toArray(new StatusEntry[0]);
        List<StatusEntry> result = new ArrayList<StatusEntry>();
        for (StatusEntry entry : entries) {
            if (level == null || level.equals(entry.getLevel())) {
                result.add(entry);
            }
        }
        return result;
    }

    /**
     * Merge this and the input statuses. Will append all entries from the input to this one. If the status to merge has
     * last error it will be used. This method is not thread safe, the two statuses are assumed to be inactive in the
     * time of merging.
     *
     * @param toMerge The multi status to merge into this.
     */
    public void merge(MultiStatusHolder toMerge) {
        statusEntries.addAll(toMerge.getAllEntries());
        if (toMerge.isError()) {
            setLastError(toMerge.getLastError());
        }
    }

    /**
     * Merge this and the input status. Will append entry from the input this one. If the status to merge has last error
     * it will be used. This method is not thread safe, the two statuses are assumed to be inactive in the time of
     * merging.
     *
     * @param toMerge The status to merge into this.
     */
    public void merge(BasicStatusHolder toMerge) {
        statusEntries.add(toMerge.getStatusEntry());
        if (toMerge.isError()) {
            setLastError(toMerge.getLastError());
        }
    }
}
