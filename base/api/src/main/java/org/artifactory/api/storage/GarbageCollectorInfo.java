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

package org.artifactory.api.storage;

import org.artifactory.log.LoggerFactory;
import org.slf4j.Logger;

/**
 * Holds the garbage collection information
 *
 * @author Noam Y. Tenne
 */
public class GarbageCollectorInfo {

    private static final Logger log = LoggerFactory.getLogger(GarbageCollectorInfo.class);

    public long startScanTimestamp;
    public long stopScanTimestamp;

    /**
     * Counter of bereaved nodes used all the time even if fix consistency is false
     */
    public int bereavedNodesCount;

    public long initialSize;
    public int initialCount;
    public long totalSizeFromBinaryProperties;
    public long totalBinaryPropertiesQueryTime;
    public long dataStoreQueryTime;
    public int cleanedElementsCount;

    public int uniqueChecksumsCleaned; //The number of binary node rows that were cleaned-up
    public int timelineRowsCleaned; //The number of binary node rows that were cleaned-up
    public long totalSizeCleaned;
    public long gcEnd;

    /**
     * Prints a summary of the collected info to the log
     *
     * @param cleanUpStartTime Time the cleanup was started
     * @param dataStoreSize    The measured size of the datastore
     */
    public void printV1CollectionInfo(long cleanUpStartTime, long dataStoreSize) {
        gcEnd = System.currentTimeMillis();
        String cleanResult;

        if (cleanedElementsCount > 0) {
            cleanResult = "Deletion execution:      " + (gcEnd - cleanUpStartTime) + "ms\n" +
                    "Initial element count:   " + initialCount + "\n" +
                    "Bereaved nodes:          " + bereavedNodesCount + "\n" +
                    "Elements cleaned:        " + cleanedElementsCount + "\n" +
                    "Initial size:            " + StorageUnit.toReadableString(initialSize) + "\n" +
                    "Total size cleaned:      " + StorageUnit.toReadableString(totalSizeCleaned) + "\n" +
                    "Current total size:      " + StorageUnit.toReadableString(dataStoreSize);
        } else {
            cleanResult = "Total element count:     " + initialCount + "\n" +
                    "Bereaved nodes:          " + bereavedNodesCount + "\n" +
                    "No Elements cleaned\n" +
                    "Current total size:      " + dataStoreSize;
        }

        String msg = "Artifactory Jackrabbit's datastore garbage collector report:\n" +
                "Total execution:         " + (gcEnd - startScanTimestamp) + "ms\n" +
                "Data Store Query:        " + dataStoreQueryTime + "ms\n" +
                "Binary Properties Query: " + totalBinaryPropertiesQueryTime + "ms\n" +
                "Total Scanning:          " + (stopScanTimestamp - startScanTimestamp) + "ms\n" +
                cleanResult;
        if (cleanedElementsCount > 0) {
            log.info(msg);
        } else {
            log.debug(msg);
        }
    }

    /**
     * Prints a summary of the collected info to the log
     *
     * @param dataStoreSize The measured size of the datastore
     */
    public void printV2CollectionInfo(long dataStoreSize) {
        StringBuilder msg = new StringBuilder("\nStorage garbage collector report:\n").append(
                "Total execution (ms):     ").append(gcEnd - startScanTimestamp).append("\n").append(
                "Unique checksums cleaned: ").append(uniqueChecksumsCleaned).append("\n").append(
                "Timeline rows cleaned:    ").append(timelineRowsCleaned).append("\n").append(
                "Total size cleaned:       ").append(StorageUnit.toReadableString(totalSizeCleaned));
        if (dataStoreSize >= 0) {
            msg.append("\n").append("Current total size:       ").append(StorageUnit.toReadableString(dataStoreSize));
        }
        log.info(msg.toString());
    }
}