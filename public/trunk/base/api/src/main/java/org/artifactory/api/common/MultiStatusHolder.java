/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2012 JFrog Ltd.
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

import com.google.common.collect.Lists;
import org.artifactory.common.StatusEntry;
import org.artifactory.common.StatusEntryLevel;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * @author freds
 * @date Sep 25, 2008
 */
public class MultiStatusHolder extends BasicStatusHolder {
    // save up to 500 messages. if exhausted, we manually drop the oldest element
    private final LinkedBlockingQueue<StatusEntry> statusEntries = new LinkedBlockingQueue<>(500);
    // save up to 2000 errors messages. if exhausted, we manually drop the oldest element
    private final LinkedBlockingQueue<StatusEntry> errorEntries = new LinkedBlockingQueue<>(2000);

    protected void addStatusEntry(StatusEntry entry) {
        super.addStatusEntry(entry);
        // we don't really want to block if we reached the limit. remove the last element until offer is accepted
        while (!statusEntries.offer(entry)) {
            statusEntries.poll();
        }
        if (entry.isError()) {
            while (!errorEntries.offer(entry)) {
                errorEntries.poll();
            }
        }
    }

    @Override
    public void reset() {
        super.reset();
        statusEntries.clear();
        errorEntries.clear();
    }

    public boolean hasErrors() {
        return isError();
    }

    public boolean hasWarnings() {
        for (StatusEntry entry : statusEntries) {
            if (StatusEntryLevel.WARNING.equals(entry.getLevel())) {
                return true;
            }
        }
        return false;
    }

    public List<StatusEntry> getAllEntries() {
        return Lists.newArrayList(statusEntries.iterator());
    }

    public List<StatusEntry> getErrors() {
        return getEntries(StatusEntryLevel.ERROR);
    }

    public List<StatusEntry> getWarnings() {
        return getEntries(StatusEntryLevel.WARNING);
    }

    public List<StatusEntry> getEntries(StatusEntryLevel level) {
        List<StatusEntry> result = new ArrayList<>();
        if (level == StatusEntryLevel.ERROR) {
            result.addAll(errorEntries);
        } else {
            for (StatusEntry entry : statusEntries) {
                if (level.equals(entry.getLevel())) {
                    result.add(entry);
                }
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
        for (StatusEntry statusEntry : toMerge.getAllEntries()) {
            addStatusEntry(statusEntry);
        }
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
        addStatusEntry(toMerge.getStatusEntry());
        if (toMerge.isError()) {
            setLastError(toMerge.getLastError());
        }
    }
}
