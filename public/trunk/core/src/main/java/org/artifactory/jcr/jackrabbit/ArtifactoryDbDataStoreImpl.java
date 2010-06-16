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

import org.apache.jackrabbit.core.data.DataIdentifier;
import org.apache.jackrabbit.core.data.DataStoreException;
import org.apache.jackrabbit.core.data.db.TempFileInputStream;
import org.apache.jackrabbit.core.util.db.DatabaseAware;
import org.apache.jackrabbit.core.util.db.DbUtility;
import org.artifactory.common.ConstantValues;
import org.artifactory.log.LoggerFactory;
import org.slf4j.Logger;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.InputStream;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Callable;

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

    // Used by the GC to accumulate cache files found, -1 at init time, then 0 before scan and real value after scan
    // -1 will generate a full folder list to mark the cached files (markAccessed)
    private long totalCacheSize = -1;
    private int nbCachedFile = 0;

    boolean slowdownScanning;
    long slowdownScanningMillis = ConstantValues.gcScanSleepBetweenIterationsMillis.getLong();

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
                if (result.exists() && result.length() == expectedLength) {
                    return result;
                }
                if (result.exists()) {
                    if (!result.delete()) {
                        throw new DataStoreException("Could not delete file before writing blobs into it");
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
        boolean firstScan = (totalCacheSize == -1);
        // Reinit the value before scan
        totalCacheSize = 0;
        nbCachedFile = 0;
        long result = super.scanDataStore(startScanNanos);
        if (cacheBlobs) {
            if (firstScan) {
                scanAllCachedFiles();
                // For first scan, putting the start scan in the future remove the protection of accessed during scan
                startScanNanos = Long.MAX_VALUE;
            }
            log.debug(
                    "The cache of the data store contains " + nbCachedFile +
                            " files for a total of " + totalCacheSize + " bytes");
            if (totalCacheSize > maxCacheSize) {
                log.info("The cache folder contains " + nbCachedFile + " files for a total of " + totalCacheSize +
                        " bytes which is above the maximum " + maxCacheSize + " allowed. Deleting cached files.");
                int nbCachedFileBefore = nbCachedFile;
                long totalCacheSizeBefore = totalCacheSize;
                // We need to cleanup cache files until we get below max
                List<ArtifactoryDbDataRecord> cached = getLruOrderedCachedRecord();
                for (ArtifactoryDbDataRecord record : cached) {
                    if (record.deleteFile(startScanNanos)) {
                        totalCacheSize -= record.length;
                        nbCachedFile--;
                        if (totalCacheSize <= maxCacheSize) {
                            break;
                        }
                    }
                }
                log.info("GC deleted " + (nbCachedFileBefore - nbCachedFile) + " cached files to save " +
                        (totalCacheSizeBefore - totalCacheSize) + " bytes");
            }
        }
        return result;
    }

    private List<ArtifactoryDbDataRecord> getLruOrderedCachedRecord() {
        List<ArtifactoryDbDataRecord> cached = new ArrayList<ArtifactoryDbDataRecord>(nbCachedFile);
        for (ArtifactoryDbDataRecord record : getAllEntries()) {
            if (record.getLastAccessTime() != Long.MIN_VALUE) {
                cached.add(record);
            }
        }
        Collections.sort(cached, new Comparator<ArtifactoryDbDataRecord>() {
            public int compare(ArtifactoryDbDataRecord o1, ArtifactoryDbDataRecord o2) {
                long diffTime = o1.getLastAccessTime() - o2.getLastAccessTime();
                if (diffTime > 0) {
                    return 1;
                } else if (diffTime < 0) {
                    return -1;
                }
                return 0;
            }
        });
        return cached;
    }

    private void scanAllCachedFiles() {
        log.info("Starting scanning all cached files present in " + cacheFolder.getAbsolutePath());
        totalCacheSize = 0;
        nbCachedFile = 0;
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
                                record.deleteFile(Long.MAX_VALUE);
                            } else {
                                record.markAccessed();
                                totalCacheSize += record.length;
                                nbCachedFile++;
                                if (nbCachedFile % 500 == 0) {
                                    log.info("Scanned " + nbCachedFile + " cached files in " +
                                            (System.currentTimeMillis() - start) + "ms");
                                }
                            }
                        }
                    }
                }
            }
        }
        log.info("Finished scanning " + nbCachedFile + " cached files in " +
                (System.currentTimeMillis() - start) + "ms");
    }

    @Override
    protected boolean validState(ArtifactoryDbDataRecord record) {
        if (cacheBlobs && (record.getLastAccessTime() != Long.MIN_VALUE)) {
            // If it was accessed it has a cache file
            totalCacheSize += record.length;
            nbCachedFile++;
        }
        return true;
    }

    @Override
    public void init(String homeDir) throws DataStoreException {
        // TODO: Log an info at the end here about the state/variable of the Data Store
        initRootStoreDir(homeDir);
        initMaxCacheSize();
        super.init(homeDir);
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
            //return;
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

}
