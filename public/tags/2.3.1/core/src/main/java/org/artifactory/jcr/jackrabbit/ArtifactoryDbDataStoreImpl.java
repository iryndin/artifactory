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

package org.artifactory.jcr.jackrabbit;

import edu.emory.mathcs.backport.java.util.concurrent.ConcurrentSkipListMap;
import org.apache.commons.io.FileUtils;
import org.apache.jackrabbit.core.data.DataIdentifier;
import org.apache.jackrabbit.core.data.DataStoreException;
import org.apache.jackrabbit.core.data.db.TempFileInputStream;
import org.apache.jackrabbit.core.util.db.DatabaseAware;
import org.apache.jackrabbit.core.util.db.DbUtility;
import org.artifactory.api.storage.StorageUnit;
import org.artifactory.common.ConstantValues;
import org.artifactory.log.LoggerFactory;
import org.artifactory.schedule.CachedThreadPoolTaskExecutor;
import org.artifactory.spring.InternalArtifactoryContext;
import org.artifactory.spring.InternalContextHelper;
import org.slf4j.Logger;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.InputStream;
import java.sql.ResultSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicLong;

import static org.artifactory.jcr.jackrabbit.ArtifactoryDbDataRecord.NOT_ACCESSED;

/**
 * @author freds
 */
public class ArtifactoryDbDataStoreImpl extends ArtifactoryBaseDataStore implements DatabaseAware {
    private static final Logger log = LoggerFactory.getLogger(ArtifactoryDbDataStoreImpl.class);

    // Parameter initialized from repo.xml
    private boolean cacheBlobs = true;

    // Parameter initialized from repo.xml
    private String blobsCacheMaxSize = "1g";

    // Parameter initialized from repo.xml
    private String blobsCacheDir = null;

    // Calculated from cacheDir string or default value ${rep.home}/cache
    private File cacheFolder = null;

    // Max cache size in bytes parsed from cacheMaxSize
    private long maxCacheSize = -1;

    boolean slowdownScanning;
    long slowdownScanningMillis = ConstantValues.gcScanSleepBetweenIterationsMillis.getLong();
    /**
     * This LRU cache holds all the files cached in the filesystem and removes files whenever it reaches the max size.
     */
    private FilesLRUCache filesystemLRUCache;

    @Override
    public void init(String homeDir) throws DataStoreException {
        initRootStoreDir(homeDir);
        initMaxCacheSize();
        filesystemLRUCache = new FilesLRUCache(maxCacheSize);
        super.init(homeDir);
    }

    @Override
    protected void accessed(ArtifactoryDbDataRecord record, long previousAccessTime) {
        filesystemLRUCache.replace(record, previousAccessTime);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public File getOrCreateFile(final DataIdentifier identifier, final long expectedLength) throws DataStoreException {
        final String id = identifier.toString();
        final ArtifactoryDbDataRecord record = getFromAllEntries(id);
        if (record == null) {
            throw new DataStoreException("Cannot load file from Blobs if DB Record does not exists in Map!");
        }
        return record.guardedActionOnFile(new Callable<File>() {
            public File call() throws Exception {
                final File result = getFile(identifier);
                if (result.exists()) {
                    if (result.length() == expectedLength) {
                        return result;
                    } else if (!result.delete()) {
                        throw new DataStoreException("Failed to delete file before writing blobs into it: "
                                + result.getAbsolutePath());
                    }
                }

                ResultSet rs = null;
                try {
                    // SELECT LENGTH, DATA FROM DATASTORE WHERE ID = ?
                    rs = conHelper.select(selectDataSQL, new Object[]{id});
                    if (!rs.next()) {
                        throw new DataStoreRecordNotFoundException("Record not found: " + id);
                    }
                    InputStream in = new BufferedInputStream(rs.getBinaryStream(2));
                    File parentFile = result.getParentFile();
                    if (!parentFile.exists()) {
                        if (!parentFile.mkdirs()) {
                            throw new DataStoreException("Cannot create folder: " + parentFile.getAbsolutePath());
                        }
                    }
                    TempFileInputStream.writeToFileAndClose(in, result);
                    return result;
                } catch (Exception e) {
                    result.delete();
                    throw convert("Can not read identifier " + id, e);
                } finally {
                    DbUtility.close(rs);
                }
            }
        });
    }

    @Override
    public long scanDataStore(long startScanNanos) {
        long result = super.scanDataStore(startScanNanos);

        if (cacheBlobs && !filesystemLRUCache.initialized) {
            // initialize cache files LRU cache if blob cache is enabled
            scanAllCachedFiles();
        }
        return result;
    }

    private void scanAllCachedFiles() {
        log.info("Starting scanning all cached files present in " + cacheFolder.getAbsolutePath());
        //find . -type f -printf %f:%s\\n
        long start = System.currentTimeMillis();

        File[] firstLevel = cacheFolder.listFiles();
        // In case the cache folder does not contain files, it returns null
        if (firstLevel == null) {
            log.warn("No files found in cache folder: " + cacheFolder.getAbsolutePath());
            return;
        }
        for (File first : firstLevel) {
            File[] secondLevel = first.listFiles();
            for (File second : secondLevel) {
                //Only yield if scanning takes longer than x seconds
                slowDownIfLongScan(start);
                File[] thirdLevel = second.listFiles();
                for (File third : thirdLevel) {
                    String[] files = third.list();
                    for (String fileName : files) {
                        File file = new File(third, fileName);
                        ArtifactoryDbDataRecord record = getFromAllEntries(fileName);
                        if (record == null) {
                            log.warn("Cached file " + file.getAbsolutePath() +
                                    " does not exists in DB! Deleting it");
                            file.delete();
                        } else {
                            if (file.length() != record.length) {
                                log.warn("Cached file " + file.getAbsolutePath() +
                                        " does not have the correct length! Deleting it");
                                // Putting the start scan in the future remove the protection of accessed during scan
                                record.deleteCacheFile(Long.MAX_VALUE);
                            } else {
                                // mark the record as accessed, this action will also update the lru cache
                                record.markAccessed();
                                if (filesystemLRUCache.cachedFilesCount.get() % 500 == 0) {
                                    log.info(
                                            "Scanned " + filesystemLRUCache.cachedFilesCount.get() +
                                                    " cached files in " +
                                                    (System.currentTimeMillis() - start) + "ms");
                                }
                            }
                        }
                    }
                }
            }
        }
        filesystemLRUCache.initialized();
        log.info("Finished scanning {} cached files in {} ms",
                filesystemLRUCache.cachedFilesCount.get(), (System.currentTimeMillis() - start));
    }

    @Override
    protected boolean validState(ArtifactoryDbDataRecord record) {
        return true;
    }

    private void slowDownIfLongScan(long start) {
        if (!slowdownScanning &&
                System.currentTimeMillis() - start > ConstantValues.gcScanStartSleepingThresholdMillis.getLong()) {
            slowdownScanning = true;
            log.debug("Slowing down serial cache scanning.");
        }
        if (slowdownScanning) {
            try {
                Thread.sleep(slowdownScanningMillis);
            } catch (InterruptedException e) {
                log.debug("Interrupted while scanning datastore cache.");
            }
        }
    }

    private void initRootStoreDir(String homeDir) throws DataStoreException {
        String fsDir = getBlobsCacheDir();
        if (fsDir != null && fsDir.length() > 0) {
            cacheFolder = new File(fsDir);
        } else {
            cacheFolder = new File(homeDir, "cache");
        }
        if (!cacheFolder.exists()) {
            if (!cacheFolder.mkdirs()) {
                throw new DataStoreException("Could not create file store folder " + cacheFolder.getAbsolutePath());
            }
        }
    }

    private void initMaxCacheSize() throws DataStoreException {
        if (!cacheBlobs) {
            // No limit here since NO files should be there
            maxCacheSize = -1;
            throw new DataStoreException("DB Data Store with no cache files is not supported yet!");
        }
        String maxSize = getBlobsCacheMaxSize();
        if (maxSize == null || maxSize.length() < 2) {
            throw new DataStoreException("Maximum size of all cached files is mandatory!\n" +
                    "The format is Xg Xm or Xk for X in Gb Mb and Kb respectively");
        }
        maxSize = maxSize.toLowerCase();
        char unit = maxSize.charAt(maxSize.length() - 1);
        long value = Long.parseLong(maxSize.substring(0, maxSize.length() - 1));
        switch (unit) {
            case 'g':
                maxCacheSize = value * 1024l * 1024l * 1024l;
                break;
            case 'm':
                maxCacheSize = value * 1024l * 1024l;
                break;
            case 'k':
                maxCacheSize = value * 1024l;
                break;
            default:
                throw new DataStoreException("Maximum size of all cached files '" +
                        maxSize + "' has an invalid format!\n" +
                        "The format is Xg Xm or Xk for X in Gb Mb and Kb respectively");
        }
    }

    @Override
    public File getBinariesFolder() {
        return cacheFolder;
    }

    @Override
    protected boolean saveBinariesAsBlobs() {
        return true;
    }

    public boolean isCacheBlobs() {
        return cacheBlobs;
    }

    public void setCacheBlobs(boolean cacheBlobs) {
        this.cacheBlobs = cacheBlobs;
    }

    public String getBlobsCacheMaxSize() {
        return blobsCacheMaxSize;
    }

    public void setBlobsCacheMaxSize(String blobsCacheMaxSize) {
        this.blobsCacheMaxSize = blobsCacheMaxSize;
    }

    public String getBlobsCacheDir() {
        return blobsCacheDir;
    }

    public void setBlobsCacheDir(String blobsCacheDir) {
        this.blobsCacheDir = blobsCacheDir;
    }

    public void exportData(String destDir) throws Exception {
        super.scanDataStore(System.currentTimeMillis());
        Collection<ArtifactoryDbDataRecord> entries = getAllEntries();
        File binariesFolder = getBinariesFolder();
        String binariesFolderAbsolutePath = binariesFolder.getAbsolutePath();
        for (ArtifactoryDbDataRecord entry : entries) {
            File src = null;
            try {
                src = getOrCreateFile(entry.getIdentifier(), entry.getLength());
            } catch (DataStoreException e) {
                String msg = "Bad identifier: " + entry.getIdentifier();
                log.error(msg);
                if (log.isDebugEnabled()) {
                    log.debug(msg, e);
                }
                continue;
            }
            String absPath = src.getAbsolutePath();
            File dest;
            if (absPath.startsWith(binariesFolderAbsolutePath)) {
                String suffix = absPath.substring(binariesFolderAbsolutePath.length());
                dest = new File(destDir, suffix);
            } else {
                dest = new File(destDir, src.getName());
            }
            if (!src.getAbsolutePath().equals(dest.getAbsolutePath())) {
                FileUtils.copyFile(src, dest);
            }
        }
    }

    public static class FilesLRUCache {
        /**
         * Maximum cache size in bytes. The cache can still grow above this number, but it will trigger a cleanup.
         */
        private final long maxCacheSize;
        /**
         * A concurrent sorted map that holds all the records with cached files by last access time.
         */
        private ConcurrentSkipListMap/*<Long, ArtifactoryDbDataRecord>*/ cachedRecords;
        /**
         * Total size in bytes of the cached files.
         */
        private AtomicLong cachedFilesSize = new AtomicLong(0);
        /**
         * Count of the cached files.
         */
        private AtomicLong cachedFilesCount = new AtomicLong(0);
        /**
         * Cache cleanup semaphore. Allows only one thread to do a cleanup.
         */
        private Semaphore cleanLock = new Semaphore(1);
        /**
         * The cache is not initialized until all the cached records are scanned (short time after startup).
         */
        private boolean initialized;

        private FilesLRUCache(long maxCacheSize) {
            this.maxCacheSize = maxCacheSize;
            cachedRecords = new ConcurrentSkipListMap();
        }

        public void put(ArtifactoryDbDataRecord record) {
            if (record.getLastAccessTime() == NOT_ACCESSED) {
                return;
            }
            cachedRecords.put(record.getLastAccessTime(), record);
            cachedFilesSize.getAndAdd(record.length);
            cachedFilesCount.incrementAndGet();
            clean();
        }

        public void remove(long accessTime) {
            ArtifactoryDbDataRecord removed = (ArtifactoryDbDataRecord) cachedRecords.remove(accessTime);
            if (removed != null) {
                cachedFilesSize.getAndAdd(-removed.length);
                cachedFilesCount.decrementAndGet();
            }
        }

        /**
         * Called when the record last access time was modified. In such a case the record should be removed and
         * re-added because the last access time is the key of the lru cache. This is not atomic but it's ok for the
         * needs of this cache (the cache files deletion are protected elsewhere).
         *
         * @param record             The record for whom the last access time was changed
         * @param previousAccessTime The previous access time of this record (in nanoseconds)
         */
        public void replace(ArtifactoryDbDataRecord record, long previousAccessTime) {
            if (previousAccessTime != NOT_ACCESSED) {
                remove(previousAccessTime);
            }
            put(record);
        }

        public void initialized() {
            this.initialized = true;
            clean();
        }

        private void clean() {
            if (!initialized) {
                // don't attempt to clean until fully initialized
                return;
            }
            if (cachedFilesSize.get() <= maxCacheSize) {
                return;
            }

            // try to acquire the lock to do the clean. Only one cleanup is allowed. The lock will be released by
            // the cleaning thread
            if (!cleanLock.tryAcquire()) {
                // someone is already cleaning
                return;
            }

            InternalArtifactoryContext context = InternalContextHelper.get();
            CachedThreadPoolTaskExecutor executor = context.beanForType(CachedThreadPoolTaskExecutor.class);

            executor.submit(new Callable<Object>() {
                public Object call() throws Exception {
                    try {
                        // give a chance for open streams to close (also we don;t wan't to run this too often)
                        Thread.sleep(500);
                        long scanStartTime = System.nanoTime();
                        log.debug("The cache folder contains {} files for a total of {} which is above the " +
                                "maximum {} allowed. Deleting cached files.",
                                new Object[]{cachedFilesCount,
                                        StorageUnit.toReadableString(cachedFilesSize.get()),
                                        StorageUnit.toReadableString(maxCacheSize)});
                        long nbCachedFileBefore = cachedFilesCount.get();
                        long totalCacheSizeBefore = cachedFilesSize.get();
                        // We need to cleanup cache files until we get below a certain threshold
                        long lowerCacheSizeThreshold = (long) (maxCacheSize * 0.9);   // 10% below max
                        // iterate while there are more elements and the cache size is higher than the threshold
                        Iterator iterator = cachedRecords.entrySet().iterator();
                        while (cachedFilesSize.get() > lowerCacheSizeThreshold && iterator.hasNext()) {
                            Map.Entry oldest = (Map.Entry) iterator.next();
                            ArtifactoryDbDataRecord oldestRecord = (ArtifactoryDbDataRecord) oldest.getValue();
                            // attempt deletion only if the entry access time is older than the scan start time
                            if (oldestRecord.getLastAccessTime() < scanStartTime) {
                                log.trace("Attempting to delete cache file of record: {}",
                                        oldestRecord.getIdentifier());
                                if (oldestRecord.deleteCacheFile(scanStartTime)) {
                                    // remove only if file successfully deleted (the logic to allow/restrict deletion
                                    // is encapsulated in the record object)
                                    remove((Long) oldest.getKey());
                                }
                            }
                        }
                        log.debug("GC deleted {} cached files to save {}",
                                (nbCachedFileBefore - cachedFilesCount.get()),
                                StorageUnit.toReadableString(totalCacheSizeBefore - cachedFilesSize.get()));
                        return null;
                    } finally {
                        // release the clean lock
                        cleanLock.release();
                    }
                }
            });
        }
    }
}
