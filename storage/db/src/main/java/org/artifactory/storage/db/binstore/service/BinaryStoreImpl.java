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

package org.artifactory.storage.db.binstore.service;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.MapMaker;
import com.google.common.collect.Sets;
import org.artifactory.api.common.MultiStatusHolder;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.api.storage.BinariesInfo;
import org.artifactory.api.storage.StorageUnit;
import org.artifactory.binstore.BinaryInfo;
import org.artifactory.checksum.ChecksumType;
import org.artifactory.common.ArtifactoryHome;
import org.artifactory.storage.BinaryInsertRetryException;
import org.artifactory.storage.StorageException;
import org.artifactory.storage.StorageProperties;
import org.artifactory.storage.binstore.BinaryStoreInputStream;
import org.artifactory.storage.binstore.GarbageCollectorInfo;
import org.artifactory.storage.binstore.service.FileBinaryProvider;
import org.artifactory.storage.binstore.service.InternalBinaryStore;
import org.artifactory.storage.binstore.service.ProviderConnectMode;
import org.artifactory.storage.db.DbService;
import org.artifactory.storage.db.binstore.dao.BinariesDao;
import org.artifactory.storage.db.binstore.entity.BinaryData;
import org.artifactory.storage.db.binstore.model.BinaryInfoImpl;
import org.artifactory.storage.db.util.JdbcHelper;
import org.artifactory.storage.db.util.blob.BlobWrapperFactory;
import org.artifactory.storage.fs.service.ArchiveEntriesService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

import static org.artifactory.storage.StorageProperties.BinaryProviderType;

/**
 * The main binary store of Artifactory that delegates to the BinaryProvider chain.
 *
 * @author Yossi Shaul
 */
@Service
public class BinaryStoreImpl implements InternalBinaryStore {
    private static final Logger log = LoggerFactory.getLogger(BinaryStoreImpl.class);

    @Autowired
    private BinariesDao binariesDao;

    @Autowired
    private ArchiveEntriesService archiveEntriesService;

    @Autowired
    private JdbcHelper jdbcHelper;

    @Autowired
    private DbService dbService;

    @Autowired
    private StorageProperties storageProperties;

    @Autowired
    private BlobWrapperFactory blobsFactory;

    private UsageTrackingBinaryProvider firstBinaryProvider;

    private FileBinaryProvider fileBinaryProvider;

    /**
     * Map of delete protected sha1 checksums to the number of protections (active readers + writer count for each binary)
     */
    private ConcurrentMap<String, AtomicInteger> deleteProtectedBinaries;

    @PostConstruct
    public void initialize() {
        LinkedList<BinaryProviderBase> binaryProviders = Lists.newLinkedList();
        // Always starts with read tracker
        deleteProtectedBinaries = new MapMaker().makeMap();
        firstBinaryProvider = new UsageTrackingBinaryProvider(this);
        binaryProviders.add(firstBinaryProvider);
        // Init of bin providers depending on constant values
        BinaryProviderType binaryProviderName = storageProperties.getBinariesStorageType();
        switch (binaryProviderName) {
            case filesystem:
                fileBinaryProvider = new FileBinaryProviderImpl(ArtifactoryHome.get().getHaAwareDataDir(),
                        storageProperties);
                binaryProviders.add((BinaryProviderBase) fileBinaryProvider);
                break;
            case cachedFS:
                if (storageProperties.getBinaryProviderCacheMaxSize() > 0) {
                    fileBinaryProvider = new FileCacheBinaryProviderImpl(ArtifactoryHome.get().getDataDir(),
                            storageProperties);
                    binaryProviders.add((BinaryProviderBase) fileBinaryProvider);
                } else {
                    throw new IllegalStateException("Binary provider typed cachedFS cannot have a zero cached size!");
                }
                binaryProviders.add(new FileBinaryProviderImpl(ArtifactoryHome.get().getDataDir(), storageProperties));
                break;
            case fullDb:
                if (storageProperties.getBinaryProviderCacheMaxSize() > 0) {
                    fileBinaryProvider = new FileCacheBinaryProviderImpl(ArtifactoryHome.get().getHaAwareDataDir(),
                            storageProperties);
                    binaryProviders.add((BinaryProviderBase) fileBinaryProvider);
                }
                binaryProviders.add(new BlobBinaryProviderImpl(jdbcHelper, dbService.getDatabaseType(), blobsFactory));
                break;
            default:
                throw new IllegalStateException("Binary provider name " + binaryProviderName + " not supported!");
        }
        if (storageProperties.getBinaryProviderExternalDir() != null) {
            if (storageProperties.getBinaryProviderExternalMode() != null) {
                binaryProviders.add(new ExternalWrapperBinaryProviderImpl(getBinariesDir(),
                        storageProperties.getBinaryProviderExternalMode()));
            }
            binaryProviders.add(
                    new ExternalFileBinaryProviderImpl(new File(storageProperties.getBinaryProviderExternalDir())));
        }
        setBinaryProvidersContext(binaryProviders);
    }

    @Override
    public void addExternalFilestore(File externalDir, ProviderConnectMode connectMode) {
        if (connectMode != ProviderConnectMode.PASS_THROUGH) {
            addBinaryProvider(new ExternalWrapperBinaryProviderImpl(getBinariesDir(), connectMode));
        }
        addBinaryProvider(new ExternalFileBinaryProviderImpl(externalDir));
    }

    @Override
    public void disconnectExternalFilestore(File externalDir, ProviderConnectMode disconnectMode,
            MultiStatusHolder statusHolder) {
        // First search for the external binary store to disconnect
        ExternalFileBinaryProviderImpl externalFilestore = null;
        BinaryProviderBase bp = getFirstBinaryProvider();
        while (bp != null) {
            if (bp instanceof ExternalFileBinaryProviderImpl
                    && ((ExternalFileBinaryProviderImpl) bp).getBinariesDir().getAbsolutePath()
                    .equals(externalDir.getAbsolutePath())) {
                externalFilestore = (ExternalFileBinaryProviderImpl) bp;
                break;
            }
            bp = bp.next();
        }
        if (externalFilestore == null) {
            statusHolder.error("Could not find any external filestore" +
                    " pointing to " + externalDir.getAbsolutePath(), log);
            return;
        }

        // Then look for wrapper if exists
        ExternalWrapperBinaryProviderImpl wrapper = null;
        bp = getFirstBinaryProvider();
        while (bp != null) {
            if (bp instanceof ExternalWrapperBinaryProviderImpl) {
                if (((ExternalWrapperBinaryProviderImpl) bp).nextFileProvider() == externalFilestore) {
                    wrapper = (ExternalWrapperBinaryProviderImpl) bp;
                    break;
                }
            }
            bp = bp.next();
        }
        if (wrapper != null) {
            wrapper.setConnectMode(disconnectMode);
        } else {
            wrapper = new ExternalWrapperBinaryProviderImpl(getBinariesDir(), disconnectMode);
            wrapper.setContext(new BinaryProviderContextImpl(this, externalFilestore));
        }

        // Now run fetch all on wrapper
        try {
            statusHolder.status("Disconnecting " + externalDir.getAbsolutePath()
                    + " using mode " + disconnectMode.propName, log);
            Collection<BinaryData> all = binariesDao.findAll();
            long sizeMoved = 0L;
            int total = all.size();
            int checked = 0;
            int done = 0;
            statusHolder.status("Found " + total + " files to disconnect!", log);
            for (BinaryData data : all) {
                try {
                    String sha1 = data.getSha1();
                    if (wrapper.connect(sha1)) {
                        statusHolder.debug("Activated " + disconnectMode.propName + " on " + sha1, log);
                        done++;
                        sizeMoved += data.getLength();
                    } else {
                        statusHolder.debug("File " + sha1 + " checked", log);
                    }
                    checked++;
                } catch (Exception e) {
                    statusHolder.error("Problem connecting checksum " + data, e, log);
                }
                if (checked % 200 == 0) {
                    statusHolder.status("Checked " + checked + "/" + total +
                            " files and disconnected " + done +
                            " total size " + disconnectMode.propName +
                            " is " + StorageUnit.toReadableString(sizeMoved), log);
                }
            }
            statusHolder.status("Checked " + checked + " files out of " + total +
                    " files and disconnected " + done +
                    " total size " + disconnectMode.propName +
                    " is " + StorageUnit.toReadableString(sizeMoved), log);
        } catch (SQLException e) {
            statusHolder.error("Could fetch all binary data from binary store", e, log);
        }
    }

    void addBinaryProvider(BinaryProviderBase binaryProvider) {
        LinkedList<BinaryProviderBase> binaryProviders = Lists.newLinkedList();
        BinaryProviderBase bp = getFirstBinaryProvider();
        while (bp != null) {
            binaryProviders.add(bp);
            bp = bp.next();
        }
        binaryProviders.removeLast();
        binaryProviders.add(binaryProvider);
        setBinaryProvidersContext(binaryProviders);
    }

    private void setBinaryProvidersContext(LinkedList<BinaryProviderBase> binaryProviders) {
        if (!(binaryProviders.getFirst() instanceof UsageTrackingBinaryProvider)) {
            throw new IllegalStateException("The first binary provider should be read tracking!");
        }
        // Make sure the last one is the empty binary provider
        if (!(binaryProviders.getLast() instanceof EmptyBinaryProvider)) {
            binaryProviders.add(new EmptyBinaryProvider());
        }
        FileBinaryProvider foundFileBinaryProvider = null;
        BinaryProviderBase previous = null;
        for (BinaryProviderBase binaryProvider : binaryProviders) {
            if (previous != null) {
                // Set next to previous
                previous.setContext(new BinaryProviderContextImpl(this, binaryProvider));
            }
            if (foundFileBinaryProvider == null && previous instanceof FileBinaryProvider) {
                foundFileBinaryProvider = (FileBinaryProvider) previous;
            }
            previous = binaryProvider;
        }
        fileBinaryProvider = foundFileBinaryProvider;
        firstBinaryProvider = (UsageTrackingBinaryProvider) binaryProviders.getFirst();
    }

    private BinaryProviderBase getFirstBinaryProvider() {
        return firstBinaryProvider;
    }

    @Nullable
    public FileBinaryProvider getFileBinaryProvider() {
        return fileBinaryProvider;
    }

    @Override
    public File getBinariesDir() {
        if (getFileBinaryProvider() != null) {
            return getFileBinaryProvider().getBinariesDir();
        } else {
            return null;
        }
    }

    @Override
    @Nullable
    public BinaryInfo addBinaryRecord(String sha1, String md5, long length) {
        try {
            BinaryData result = binariesDao.load(sha1);
            if (result == null) {
                // It does not exists in the DB
                // Let's check if in bin provider
                if (getFirstBinaryProvider().exists(sha1, length)) {
                    // Good let's use it
                    return insertRecordInDb(new BinaryData(sha1, md5, length));
                }
                return null;
            }
            return convertToBinaryInfo(result);
        } catch (SQLException e) {
            throw new StorageException("Could not reserved entry '" + sha1 + "'", e);
        }
    }

    @Override
    @Nonnull
    public BinaryInfo addBinary(InputStream in) throws IOException {
        BinaryInfo result = null;
        if (in instanceof BinaryStoreInputStream) {
            String sha1 = ((BinaryStoreInputStream) in).getSha1();
            log.debug("Trying to use useBinary instead of addBinary for {}", sha1);
            try {
                BinaryData binData = binariesDao.load(sha1);
                if (binData != null) {
                    result = convertToBinaryInfo(binData);
                }
            } catch (SQLException e) {
                throw new StorageException("Could check for sha1 " + sha1 + " existence!", e);
            }
        }
        if (result == null) {
            BinaryInfo bi = getFirstBinaryProvider().addStream(in);
            log.trace("Inserted binary {} to file store", bi.getSha1());
            // From here we managed to create a binary record on the binary provider
            // So, failing on the insert in DB (because saving the file took to long)
            // can be re-tried based on the sha1
            try {
                result = insertRecordInDb(convertToBinaryData(bi));
            } catch (StorageException e) {
                throw new BinaryInsertRetryException(bi, e);
            }
        }
        return result;
    }

    private BinaryInfo convertToBinaryInfo(BinaryData bd) {
        return new BinaryInfoImpl(bd.getSha1(), bd.getMd5(), bd.getLength());
    }

    private BinaryData convertToBinaryData(BinaryInfo bi) {
        return new BinaryData(bi.getSha1(), bi.getMd5(), bi.getLength());
    }

    @Override
    public InputStream getBinary(String sha1) {
        return getFirstBinaryProvider().getStream(sha1);
    }

    @Override
    public BinaryInfo findBinary(String sha1) {
        try {
            BinaryData result = binariesDao.load(sha1);
            if (result != null) {
                return convertToBinaryInfo(result);
            }
        } catch (SQLException e) {
            throw new StorageException("Storage error loading checksum '" + sha1 + "'", e);
        }
        return null;
    }

    @Nonnull
    @Override
    public Set<BinaryInfo> findBinaries(@Nullable Collection<String> checksums) {
        Set<BinaryInfo> results = Sets.newHashSet();
        if (checksums == null || checksums.isEmpty()) {
            return results;
        }
        try {
            for (ChecksumType checksumType : ChecksumType.BASE_CHECKSUM_TYPES) {
                Collection<String> validChecksums = extractValid(checksumType, checksums);
                if (!validChecksums.isEmpty()) {
                    Collection<BinaryData> found = binariesDao.search(checksumType, validChecksums);
                    for (BinaryData data : found) {
                        results.add(convertToBinaryInfo(data));
                    }
                }
            }
        } catch (SQLException e) {
            throw new StorageException("Could not search for checksums " + checksums, e);
        }
        return results;
    }

    private Collection<String> extractValid(ChecksumType checksumType, Collection<String> checksums) {
        Collection<String> results = Sets.newHashSet();
        for (String checksum : checksums) {
            if (checksumType.isValid(checksum)) {
                results.add(checksum);
            }
        }
        return results;
    }

    @Override
    public GarbageCollectorInfo garbageCollect() {
        final GarbageCollectorInfo result = new GarbageCollectorInfo();
        Collection<BinaryData> binsToDelete;
        try {
            BinariesInfo countAndSize = binariesDao.getCountAndTotalSize();
            result.initialCount = countAndSize.getBinariesCount();
            result.initialSize = countAndSize.getBinariesSize();
            binsToDelete = binariesDao.findPotentialDeletion();
        } catch (SQLException e) {
            throw new StorageException("Could not find potential Binaries to delete!", e);
        }
        result.stopScanTimestamp = System.currentTimeMillis();
        result.candidatesForDeletion = binsToDelete.size();
        for (BinaryData bd : binsToDelete) {
            log.trace("Candidate for deletion: {}", bd);
            dbService.invokeInTransaction(new BinaryCleaner(bd, result));
        }

        if (result.checksumsCleaned > 0) {
            InternalBinaryStore txMe = ContextHelper.get().beanForType(InternalBinaryStore.class);
            result.archivePathsCleaned = txMe.deleteUnusedArchivePaths();
            result.archiveNamesCleaned = txMe.deleteUnusedArchiveNames();
        }

        result.gcEndTime = System.currentTimeMillis();

        try {
            BinariesInfo countAndSize = binariesDao.getCountAndTotalSize();
            result.printCollectionInfo(countAndSize.getBinariesSize());
        } catch (SQLException e) {
            log.error("Could not list files due to " + e.getMessage());
        }
        return result;
    }

    Set<String> isInStore(Set<String> sha1List) {
        try {
            return Sets.newHashSet(
                    Iterables.transform(binariesDao.search(ChecksumType.sha1, sha1List),
                            new Function<BinaryData, String>() {
                                @Override
                                public String apply(@Nullable BinaryData input) {
                                    return input == null ? "" : input.getSha1();
                                }
                            }
                    )
            );
        } catch (SQLException e) {
            throw new StorageException("Could search for checksum list!", e);
        }
    }


    /**
     * Deletes binary row and all dependent rows from the database
     *
     * @param sha1ToDelete Checksum to delete
     * @return True if deleted. False if not found or error
     */
    private boolean deleteEntry(String sha1ToDelete) {
        boolean hadArchiveEntries;
        try {
            hadArchiveEntries = archiveEntriesService.deleteArchiveEntries(sha1ToDelete);
        } catch (Exception e) {
            log.error("Failed to delete archive entries for " + sha1ToDelete, e);
            return false;
        }
        try {
            boolean entryDeleted = binariesDao.deleteEntry(sha1ToDelete) == 1;
            if (!entryDeleted && hadArchiveEntries) {
                log.error("Binary entry " + sha1ToDelete + " had archive entries that are deleted," +
                        " but the binary line was not deleted! Re indexing of archive needed.");
            }
            return entryDeleted;
        } catch (SQLException e) {
            log.error("Could execute delete from binary store of " + sha1ToDelete, e);
        }
        return false;
    }

    @Override
    public int deleteUnusedArchivePaths() {
        try {
            log.debug("Deleting unused archive paths");
            return archiveEntriesService.deleteUnusedPathIds();
        } catch (StorageException e) {
            log.error("Failed to delete unique paths: {}", e.getMessage());
            log.debug("Failed to delete unique paths", e);
            return 0;
        }
    }

    @Override
    public int deleteUnusedArchiveNames() {
        try {
            log.debug("Deleting unused archive names");
            return archiveEntriesService.deleteUnusedNameIds();
        } catch (StorageException e) {
            log.error("Failed to delete unique archive names: {}", e.getMessage());
            log.debug("Failed to delete unique archive paths", e);
            return 0;
        }
    }

    @Override
    public int incrementNoDeleteLock(String sha1) {
        AtomicInteger previous = deleteProtectedBinaries.putIfAbsent(sha1, new AtomicInteger(1));
        if (previous == null) {
            return 1;
        } else {
            return previous.incrementAndGet();
        }
    }

    @Override
    public void decrementNoDeleteLock(String sha1) {
        AtomicInteger usageCount = deleteProtectedBinaries.get(sha1);
        if (usageCount != null) {
            usageCount.decrementAndGet();
        }
    }

    @Override
    public Collection<BinaryInfo> findAllBinaries() {
        try {
            Collection<BinaryData> allBinaries = binariesDao.findAll();
            List<BinaryInfo> result = new ArrayList<>(allBinaries.size());
            for (BinaryData bd : allBinaries) {
                result.add(convertToBinaryInfo(bd));
            }
            return result;
        } catch (SQLException e) {
            throw new StorageException("Could not retrieve all binary entries", e);
        }
    }

    @Nonnull
    private BinaryInfo insertRecordInDb(BinaryData dataRecord) throws StorageException {
        if (!dataRecord.isValid()) {
            throw new StorageException("Cannot insert invalid binary record: " + dataRecord);
        }
        String sha1 = dataRecord.getSha1();
        try {
            boolean binaryExists = binariesDao.exists(sha1);
            if (!binaryExists) {
                createDataRecord(dataRecord, sha1);
            }
            // Always reselect from DB before returning
            BinaryData justInserted = binariesDao.load(sha1);
            if (justInserted == null) {
                throw new StorageException("Could not find just inserted binary record: " + dataRecord);
            }
            return convertToBinaryInfo(justInserted);
        } catch (SQLException e) {
            throw new StorageException("Failed to insert new binary record: " + e.getMessage(), e);
        }
    }

    private void createDataRecord(BinaryData dataRecord, String sha1) throws SQLException {
        // insert a new binary record to the db
        try {
            binariesDao.create(dataRecord);
        } catch (SQLException e) {
            if (isDuplicatedEntryException(e)) {
                log.debug("Simultaneous insert of binary {} detected, binary will be checked.", sha1, e);
                throw new BinaryInsertRetryException(convertToBinaryInfo(dataRecord), e);
            } else {
                throw e;
            }
        }
    }

    private boolean isDuplicatedEntryException(SQLException exception) {
        String message = exception.getMessage();
        return message.contains("duplicate key") // Derby message
                || message.contains("Duplicate entry") // MySQL message
                || message.contains("unique constraint"); // Oracle message
    }

    /**
     * @return Number of binaries and total size stored in the binary store
     */
    @Override
    public BinariesInfo getBinariesInfo() {
        try {
            return binariesDao.getCountAndTotalSize();
        } catch (SQLException e) {
            throw new StorageException("Could not calculate total size due to " + e.getMessage(), e);
        }
    }

    @Override
    public long getStorageSize() {
        return getBinariesInfo().getBinariesSize();
    }

    @Override
    public void ping() {
        File binariesDir = getBinariesDir();
        if (binariesDir != null && !binariesDir.canWrite()) {
            throw new StorageException("Cannot write to " + binariesDir.getAbsolutePath());
        }
        try {
            if (binariesDao.exists("does not exists")) {
                throw new StorageException("Select entry fails");
            }
        } catch (SQLException e) {
            throw new StorageException("Accessing Binary Store DB failed with " + e.getMessage(), e);
        }
    }

    @Override
    public void prune(MultiStatusHolder statusHolder) {
        if (fileBinaryProvider != null) {
            fileBinaryProvider.prune(statusHolder);
        } else {
            statusHolder.warn("Filesystem storage is not used. Skipping prune", log);
        }
    }

    /**
     * @param sha1 sha1 checksum of the binary to check
     * @return True if the given binary is currently used by a reader (e.g., open stream) or writer
     */
    public boolean isActivelyUsed(String sha1) {
        AtomicInteger usageCounter = deleteProtectedBinaries.get(sha1);
        return usageCounter != null && usageCounter.get() > 0;
    }

    /**
     * Deletes a single binary from the database and filesystem if not in use.
     */
    private class BinaryCleaner implements Callable<Void> {
        private final GarbageCollectorInfo result;
        private final BinaryData bd;

        public BinaryCleaner(BinaryData bd, GarbageCollectorInfo result) {
            this.result = result;
            this.bd = bd;
        }

        @Override
        public Void call() throws Exception {
            String sha1 = bd.getSha1();
            deleteProtectedBinaries.putIfAbsent(sha1, new AtomicInteger(0));
            AtomicInteger usageCounter = deleteProtectedBinaries.get(sha1);
            if (usageCounter.compareAndSet(0, -30)) {
                try {
                    if (deleteEntry(sha1)) {
                        log.trace("Deleted {} record from binaries table", sha1);
                        result.checksumsCleaned++;
                        if (getFirstBinaryProvider().delete(sha1)) {
                            log.trace("Deleted {} binary", sha1);
                            result.binariesCleaned++;
                            result.totalSizeCleaned += bd.getLength();
                        } else {
                            log.error("Could not delete binary '{}'", sha1);
                        }
                    }
                } finally {
                    // remove delete protection (even if delete was not successful)
                    deleteProtectedBinaries.remove(sha1);
                }
            } else {
                log.info("Binary {} is being read! Not deleting.", sha1);
            }
            return null;
        }
    }
}
