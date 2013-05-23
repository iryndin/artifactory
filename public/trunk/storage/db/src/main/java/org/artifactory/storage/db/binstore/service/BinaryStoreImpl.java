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
import com.google.common.collect.Sets;
import org.artifactory.api.common.MultiStatusHolder;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.api.storage.StorageUnit;
import org.artifactory.binstore.BinaryInfo;
import org.artifactory.checksum.ChecksumType;
import org.artifactory.common.ArtifactoryHome;
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
import org.artifactory.storage.fs.service.ArchiveEntriesService;
import org.artifactory.util.Pair;
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

import static org.artifactory.storage.StorageProperties.BinaryStorageType;

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

    private ReadTrackingBinaryProvider firstBinaryProvider;

    private FileBinaryProvider fileBinaryProvider;

    @PostConstruct
    public void initialize() {
        LinkedList<BinaryProviderBase> binaryProviders = Lists.newLinkedList();
        // Always starts with read tracker
        firstBinaryProvider = new ReadTrackingBinaryProvider();
        binaryProviders.add(firstBinaryProvider);
        // Init of bin providers depending on constant values
        BinaryStorageType binaryProviderName = storageProperties.getBinariesStorageType();
        switch (binaryProviderName) {
            case filesystem:
                fileBinaryProvider = new FileBinaryProviderImpl(ArtifactoryHome.get().getDataDir(), storageProperties);
                binaryProviders.add((BinaryProviderBase) fileBinaryProvider);
                break;
            case fullDb:
                if (storageProperties.getBinaryProviderCacheMaxSize() > 0) {
                    fileBinaryProvider = new FileCacheBinaryProviderImpl(ArtifactoryHome.get().getDataDir(),
                            storageProperties);
                    binaryProviders.add((BinaryProviderBase) fileBinaryProvider);
                    binaryProviders.add(new BlobBinaryProviderImpl(jdbcHelper, dbService.getDatabaseType()));
                } else {
                    binaryProviders.add(new BlobBinaryProviderImpl(jdbcHelper, dbService.getDatabaseType()));
                }
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
            statusHolder.setError("Could not find any external filestore" +
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
            statusHolder.setStatus("Disconnecting " + externalDir.getAbsolutePath()
                    + " using mode " + disconnectMode.propName, log);
            Collection<BinaryData> all = binariesDao.findAll();
            long sizeMoved = 0L;
            int total = all.size();
            int checked = 0;
            int done = 0;
            statusHolder.setStatus("Found " + total + " files to disconnect!", log);
            for (BinaryData data : all) {
                try {
                    String sha1 = data.getSha1();
                    if (wrapper.connect(sha1)) {
                        statusHolder.setDebug("Activated " + disconnectMode.propName + " on " + sha1, log);
                        done++;
                        sizeMoved += data.getLength();
                    } else {
                        statusHolder.setDebug("File " + sha1 + " checked", log);
                    }
                    checked++;
                } catch (Exception e) {
                    statusHolder.setError("Problem connecting checksum " + data, e, log);
                }
                if (checked % 200 == 0) {
                    statusHolder.setStatus("Checked " + checked + "/" + total +
                            " files and disconnected " + done +
                            " total size " + disconnectMode.propName +
                            " is " + StorageUnit.toReadableString(sizeMoved), log);
                }
            }
            statusHolder.setStatus("Checked " + checked + " files out of " + total +
                    " files and disconnected " + done +
                    " total size " + disconnectMode.propName +
                    " is " + StorageUnit.toReadableString(sizeMoved), log);
        } catch (SQLException e) {
            statusHolder.setError("Could fetch all binary data from binary store", e, log);
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
        if (!(binaryProviders.getFirst() instanceof ReadTrackingBinaryProvider)) {
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
        firstBinaryProvider = (ReadTrackingBinaryProvider) binaryProviders.getFirst();
    }

    private BinaryProviderBase getFirstBinaryProvider() {
        return firstBinaryProvider;
    }

    public FileBinaryProvider getFileBinaryProvider() {
        return fileBinaryProvider;
    }

    public ReadTrackingBinaryProvider getReadTrackingBinaryProvider() {
        return firstBinaryProvider;
    }

    @Override
    public File getBinariesDir() {
        return getFileBinaryProvider().getBinariesDir();
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

    private BinaryInfo convertToBinaryInfo(BinaryData bd) {
        return new BinaryInfoImpl(bd.getSha1(), bd.getMd5(), bd.getLength());
    }

    private BinaryData convertToBinaryData(BinaryInfo bi) {
        return new BinaryData(bi.getSha1(), bi.getMd5(), bi.getLength());
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
            result = insertRecordInDb(convertToBinaryData(bi));
        }
        return result;
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
            for (ChecksumType checksumType : ChecksumType.values()) {
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
        InternalBinaryStore txMe = ContextHelper.get().beanForType(InternalBinaryStore.class);
        GarbageCollectorInfo result = new GarbageCollectorInfo();
        Collection<BinaryData> binsToDelete;
        try {
            Pair<Long, Long> countAndSize = binariesDao.getCountAndTotalSize();
            result.initialCount = countAndSize.getFirst();
            result.initialSize = countAndSize.getSecond();
            binsToDelete = binariesDao.findPotentialDeletion();
        } catch (SQLException e) {
            throw new StorageException("Could not find potential Binaries to delete!", e);
        }
        result.stopScanTimestamp = System.currentTimeMillis();
        result.candidatesForDeletion = binsToDelete.size();
        Set<String> binDataFailedToDelete = Sets.newHashSet();
        for (BinaryData bd : binsToDelete) {
            String sha1 = bd.getSha1();
            if (isUsedByReader(sha1)) {
                // Do not delete used file
                binDataFailedToDelete.add(sha1);
            } else {
                // delete immediately
                if (txMe.deleteEntry(sha1)) {
                    result.checksumsCleaned++;
                } else {
                    binDataFailedToDelete.add(sha1);
                }
            }
        }

        Set<String> notDeleted;
        if (!binDataFailedToDelete.isEmpty()) {
            notDeleted = isInStore(binDataFailedToDelete);
        } else {
            notDeleted = Sets.newHashSet();
        }
        for (BinaryData bd : binsToDelete) {
            String sha1 = bd.getSha1();
            if (!notDeleted.contains(sha1)) {
                if (isUsedByReader(sha1)) {
                    log.info("Ready to be deleted file '" + sha1 + "', is still being read! Not deleting.");
                    notDeleted.add(sha1);
                    result.candidatesForDeletion++;
                } else {
                    if (getFirstBinaryProvider().delete(sha1)) {
                        result.binariesCleaned++;
                        result.totalSizeCleaned += bd.getLength();
                    } else {
                        log.error("Could not delete binary '" + sha1 + "'");
                        notDeleted.add(sha1);
                        result.candidatesForDeletion++;
                    }
                }
            }
        }
        if (result.checksumsCleaned > 0) {
            result.archivePathsCleaned = txMe.deleteUnusedArchivePaths();
            result.archiveNamesCleaned = txMe.deleteUnusedArchiveNames();
        }

        result.gcEndTime = System.currentTimeMillis();

        try {
            Pair<Long, Long> countAndSize = binariesDao.getCountAndTotalSize();
            result.printCollectionInfo(countAndSize.getSecond());
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
                            }));
        } catch (SQLException e) {
            throw new StorageException("Could search for checksum list!", e);
        }
    }

    @Override
    public boolean deleteEntry(String sha1ToDelete) {
        if (!ChecksumType.sha1.isValid(sha1ToDelete)) {
            log.warn("Got invalid sha1 " + sha1ToDelete + " to delete!");
            return false;
        }
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
            return archiveEntriesService.deleteUnusedNameIds();
        } catch (StorageException e) {
            log.error("Failed to delete unique archive names: {}", e.getMessage());
            log.debug("Failed to delete unique archive paths", e);
            return 0;
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
                // insert a new binary record to the db
                try {
                    binariesDao.create(dataRecord);
                } catch (SQLException e) {
                    // TORE: Error handling should provide clean way to check for duplicate key errors
                    // Check if it's duplicate key
                    String message = e.getMessage();
                    if (message.contains("duplicate key") // Derby message
                            || message.contains("Duplicate entry") // MySQL message
                            || message.contains("unique constraint") // Oracle message
                            ) {
                        log.info("Duplicate insertion of same checksum " + sha1);
                    } else {
                        throw e;
                    }
                }
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

    @Override
    public long getStorageSize() {
        try {
            return binariesDao.getCountAndTotalSize().getSecond();
        } catch (SQLException e) {
            throw new StorageException("Could not calculate total size due to " + e.getMessage(), e);
        }
    }

    @Override
    public void ping() {
        if (!getBinariesDir().canWrite()) {
            throw new StorageException("Cannot write to " + getBinariesDir().getAbsolutePath());
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
        getFileBinaryProvider().prune(statusHolder);
    }

    public boolean isUsedByReader(String sha1) {
        return getReadTrackingBinaryProvider().isUsedByReader(sha1);
    }

}
