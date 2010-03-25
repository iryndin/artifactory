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
    public int nbBereavedNodes;

    public long initialSize;
    public int initialCount;
    public long totalSizeFromBinaryProperties;
    public long totalBinaryPropertiesQueryTime;
    public long dataStoreQueryTime;
    public int nbElementsClean;
    public long totalSizeCleaned;
    public long endOfGC;

    /**
     * Prints a summary of the collected info to the log
     *
     * @param cleanUpStartTime Time the cleanup was started
     * @param dataStoreSize    The measured size of the datastore
     */
    public void printCollectionInfo(long cleanUpStartTime, long dataStoreSize) {
        endOfGC = System.currentTimeMillis();
        String cleanResult;

        if (nbElementsClean > 0) {
            cleanResult = "Deletion execution:      " + (endOfGC - cleanUpStartTime) + "ms\n" +
                    "Initial element count:   " + initialCount + "\n" +
                    "Initial size:            " + initialSize + " bytes\n" +
                    "Bereaved nodes:          " + nbBereavedNodes + "\n" +
                    "Elements cleaned:        " + nbElementsClean + "\n" +
                    "Total size cleaned:      " + totalSizeCleaned + "\n" +
                    "Current total size:      " + dataStoreSize;
        } else {
            cleanResult = "Total element count:   " + initialCount + "\n" +
                    "Bereaved nodes:          " + nbBereavedNodes + "\n" +
                    "No Elements cleaned\n" +
                    "Current total size:      " + dataStoreSize;
        }

        String msg = "Artifactory Jackrabbit's datastore garbage collector report:\n" +
                "Total execution:         " + (endOfGC - startScanTimestamp) + "ms\n" +
                "Data Store Query:        " + dataStoreQueryTime + "ms\n" +
                "Binary Properties Query: " + totalBinaryPropertiesQueryTime + "ms\n" +
                "Total Scanning:          " + (stopScanTimestamp - startScanTimestamp) + "ms\n" +
                cleanResult;
        if (nbElementsClean > 0) {
            log.info(msg);
        } else {
            log.debug(msg);
        }
    }
}