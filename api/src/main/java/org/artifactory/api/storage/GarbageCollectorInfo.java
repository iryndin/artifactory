package org.artifactory.api.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

        cleanResult = "Deletion execution:      " + (endOfGC - cleanUpStartTime) + "ms\n" +
                "Initial element count:   " + initialCount + "\n" +
                "Initial size:            " + initialSize + " bytes\n" +
                "Bereaved nodes:          " + nbBereavedNodes + "\n" +
                "Elements cleaned:        " + nbElementsClean + "\n" +
                "Total size cleaned:      " + totalSizeCleaned + "\n" +
                "Current total size:      " + dataStoreSize;

        log.info("Artifactory Jackrabbit's datastore garbage collector report:\n" +
                "Total execution:         " + (endOfGC - startScanTimestamp) + "ms\n" +
                "Data Store Query:        " + dataStoreQueryTime + "ms\n" +
                "Binary Properties Query: " + totalBinaryPropertiesQueryTime + "ms\n" +
                "Total Scanning:          " + (stopScanTimestamp - startScanTimestamp) + "ms\n" +
                cleanResult
        );
    }
}