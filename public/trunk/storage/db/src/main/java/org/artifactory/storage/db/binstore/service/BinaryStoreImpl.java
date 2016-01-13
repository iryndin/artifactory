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
import com.google.common.collect.*;
import org.artifactory.addon.AddonsManager;
import org.artifactory.addon.CoreAddons;
import org.artifactory.addon.ha.HaCommonAddon;
import org.artifactory.api.common.BasicStatusHolder;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.api.mail.MailService;
import org.artifactory.api.storage.BinariesInfo;
import org.artifactory.api.storage.StorageUnit;
import org.artifactory.binstore.BinaryInfo;
import org.artifactory.checksum.ChecksumType;
import org.artifactory.common.ArtifactoryHome;
import org.artifactory.common.ha.HaNodeProperties;
import org.artifactory.storage.BinaryInsertRetryException;
import org.artifactory.storage.StorageException;
import org.artifactory.storage.StorageProperties;
import org.artifactory.storage.binstore.BinaryStoreInputStream;
import org.artifactory.storage.binstore.GarbageCollectorInfo;
import org.artifactory.storage.binstore.service.*;
import org.artifactory.storage.binstore.service.annotation.BinaryProviderClassInfo;
import org.artifactory.storage.binstore.service.base.Balanceable;
import org.artifactory.storage.binstore.service.base.BinaryProviderBase;
import org.artifactory.storage.binstore.service.base.BinaryProviderVisitor;
import org.artifactory.storage.binstore.service.providers.ExternalWrapperBinaryProviderImpl;
import org.artifactory.storage.config.model.ChainMetaData;
import org.artifactory.storage.config.model.Param;
import org.artifactory.storage.config.model.ProviderMetaData;
import org.artifactory.storage.db.DbService;
import org.artifactory.storage.db.binstore.dao.BinariesDao;
import org.artifactory.storage.fs.service.ArchiveEntriesService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.stereotype.Service;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.artifactory.storage.binstore.service.BinaryProviderFactory.*;
import static org.artifactory.storage.binstore.service.base.MutableBinaryProviderInjector.getMutableBinaryProvider;
import static org.artifactory.storage.db.binstore.service.ConfigurableBinaryProviderManager.buildByConfig;
import static org.artifactory.storage.db.binstore.service.ConfigurableBinaryProviderManager.buildByStorageProperties;

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
    private DbService dbService;

    @Autowired
    private StorageProperties storageProperties;

    private UsageTrackingBinaryProvider firstBinaryProvider;

    // TODO get rid from this fileBinaryProvider
    private FileBinaryProvider fileBinaryProvider;

    /**
     * Map of delete protected sha1 checksums to the number of protections (active readers + writer count for each binary)
     */
    private ConcurrentMap<String, AtomicInteger> deleteProtectedBinaries;
    private Map<String, Class> binaryProvidersMap;
    private List<GarbageCollectorListener> garbageCollectorListeners;

    @PostConstruct
    public void initialize() {
        try {
            garbageCollectorListeners = new CopyOnWriteArrayList<>();
            binaryProvidersMap = loadProvidersMap();
            log.debug("Initializing the ConfigurableBinaryProviderManager");
            deleteProtectedBinaries = new MapMaker().makeMap();
            File haAwareEtcDir = ArtifactoryHome.get().getHaAwareEtcDir();
            ChainMetaData selectedChain;
            // If the new generation binary config exist the use it else use the old generation filestore
            File userConfigFile = new File(haAwareEtcDir, "binarystore.xml");
            if (userConfigFile.exists()) {
                // Create binary provider according to the The new generation config
                selectedChain = buildByConfig(userConfigFile);
            } else {
                // Create binary provider using to the The old generation properties
                selectedChain = buildByStorageProperties(storageProperties);
            }
            // Now that we have the chain create the binary providers
            firstBinaryProvider = (UsageTrackingBinaryProvider) buildProviders(selectedChain, this, storageProperties);
            fileBinaryProvider = searchForFileBinaryProvider(firstBinaryProvider);
            // Add External binary providers
            String mode = storageProperties.getBinaryProviderExternalMode();
            String externalDir = storageProperties.getBinaryProviderExternalDir();
            addBinaryProvider(
                    createExternalBinaryProviders(mode, externalDir, fileBinaryProvider, storageProperties, this));
        } catch (IOException e) {
            throw new RuntimeException(
                    "Failed to initialize binary providers. Reason io exception occurred during the config read process");
        }
    }

    @PreDestroy
    public void destroy() {
        notifyGCListenersOnDestroy();
    }

    @Override
    public Map<String, Class> getBinaryProvidersMap() {
        return binaryProvidersMap;
    }

    @Override
    public void addGCListener(GarbageCollectorListener garbageCollectorListener) {
        garbageCollectorListeners.add(garbageCollectorListener);
    }

    public static Map<String, Class> loadProvidersMap() {
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(BinaryProviderClassInfo.class));
        Map<String, Class> providersMap = Maps.newHashMap();
        for (BeanDefinition bd : scanner.findCandidateComponents("org.artifactory.storage.db.binstore")) {
            updateMap(providersMap, bd);
        }
        for (BeanDefinition bd : scanner.findCandidateComponents("org.artifactory.addon.filestore")) {
            updateMap(providersMap, bd);
        }
        for (BeanDefinition bd : scanner.findCandidateComponents("org.artifactory.storage.binstore")) {
            updateMap(providersMap, bd);
        }


        return providersMap;
    }

    private static void updateMap(Map<String, Class> providersMap, BeanDefinition bd) {
        try {
            String beanClassName = bd.getBeanClassName();
            Class<?> beanClass = Class.forName(beanClassName);
            BinaryProviderClassInfo annotation = beanClass.getAnnotation(BinaryProviderClassInfo.class);
            providersMap.put(annotation.nativeName(), beanClass);
        } catch (Exception e) {
            throw new RuntimeException("Failed to init Binary provider. Reason class not found.", e);
        }
    }


    private void addBinaryProvider(List<BinaryProviderBase> binaryProvidersToAdd) {
        BinaryProviderBase binaryProvider = (BinaryProviderBase) fileBinaryProvider;
        if (binaryProvider != null) {
            while (binaryProvider.getBinaryProvider() != null) {
                binaryProvider = binaryProvider.getBinaryProvider();
            }
            for (BinaryProviderBase toAdd : binaryProvidersToAdd) {
                MutableBinaryProvider mutableBinaryProvider = getMutableBinaryProvider(binaryProvider);
                mutableBinaryProvider.setBinaryProvider(toAdd);
                binaryProvider = toAdd;
            }
        }
    }

    @Override
    public void addExternalFilestore(File externalFileDir, ProviderConnectMode connectMode) {
        // The external binary provider works only if the file binary provider is not null
        if (getBinariesDir() == null) {
            return;
        }
        String mode = connectMode.propName;
        String externalDir = externalFileDir.getAbsolutePath();
        addBinaryProvider(
                createExternalBinaryProviders(mode, externalDir, fileBinaryProvider, storageProperties, this));
    }

    @Override
    public void disconnectExternalFilestore(File externalDir, ProviderConnectMode disconnectMode,
                                            BasicStatusHolder statusHolder) {
        // The external binary provider works only if the file binary provider is not null
        if (getBinariesDir() == null) {
            return;
        }
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
            ProviderMetaData providerMetaData = new ProviderMetaData("external-wrapper", "external-wrapper");
            providerMetaData.addParam(new Param("dir", getBinariesDir().getAbsolutePath()));
            providerMetaData.addParam(new Param("connectMode", disconnectMode.propName));
            wrapper = BinaryProviderFactory.createBinaryProvider(providerMetaData, storageProperties, this);
            MutableBinaryProvider mutableBinaryProvider = getMutableBinaryProvider(wrapper);
            mutableBinaryProvider.setBinaryProvider(externalFilestore);
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

    private BinaryProviderBase getFirstBinaryProvider() {
        return firstBinaryProvider;
    }

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
    public Map<FileBinaryProvider, File> getBinariesDirs() {
        Map<FileBinaryProvider, File> dirs = Maps.newHashMap();
        List<FileBinaryProvider> result=firstBinaryProvider.visit(new FileBinaryProviderSearcher());
        for (FileBinaryProvider provider : result) {
            dirs.put( provider, provider.getBinariesDir());
        }
        return dirs;
    }

    @Override
    @Nullable
    public BinaryInfo addBinaryRecord(String sha1, String md5, long length) {
        try {
            BinaryData result = binariesDao.load(sha1);
            if (result == null) {
                // It does not exists in the DB
                // Let's check if in bin provider
                if (getFirstBinaryProvider().exists(sha1)) {
                    // Good let's use it
                    return getTransactionalMe().insertRecordInDb(sha1, md5, length);
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
        BinaryInfo binaryInfo = null;
        if (in instanceof BinaryStoreInputStream) {
            // input stream is from existing binary
            binaryInfo = getTransactionalMe().safeGetBinaryInfo((BinaryStoreInputStream) in);
        }
        if (binaryInfo == null) {
            BinaryInfo bi = getFirstBinaryProvider().addStream(in);
            log.trace("Inserted binary {} to file store", bi.getSha1());
            // From here we managed to create a binary record on the binary provider
            // So, failing on the insert in DB (because saving the file took to long)
            // can be re-tried based on the sha1
            try {
                binaryInfo = getTransactionalMe().insertRecordInDb(bi.getSha1(), bi.getMd5(), bi.getLength());
            } catch (BinaryInsertRetryException e) {
                if (log.isDebugEnabled()) {
                    log.info("Retrying add binary after receiving exception", e);
                } else {
                    log.info("Retrying add binary after receiving exception: " + e.getMessage());
                }
                binaryInfo = addBinaryRecord(bi.getSha1(), bi.getMd5(), bi.getLength());
                if (binaryInfo == null) {
                    throw new StorageException("Failed to add binary record with SHA1 " + bi.getSha1() +
                            "during retry", e);
                }
            }
        }
        return binaryInfo;
    }

    private InternalBinaryStore getTransactionalMe() {
        return ContextHelper.get().beanForType(InternalBinaryStore.class);
    }

    @Override
    public BinaryInfo safeGetBinaryInfo(BinaryStoreInputStream in) {
        BinaryInfo result = null;
        String sha1 = in.getSha1();
        log.debug("Trying to use useBinary instead of addBinary for {}", sha1);
        try {
            BinaryData binData = binariesDao.load(sha1);
            if (binData != null) {
                result = convertToBinaryInfo(binData);
            }
        } catch (SQLException e) {
            throw new StorageException("Could check for sha1 " + sha1 + " existence!", e);
        }
        return result;
    }

    private BinaryInfo convertToBinaryInfo(BinaryData bd) {
        return new BinaryInfoImpl(bd.getSha1(), bd.getMd5(), bd.getLength());
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
                    results.addAll(found.stream().map(this::convertToBinaryInfo).collect(Collectors.toList()));
                }
            }
        } catch (SQLException e) {
            throw new StorageException("Could not search for checksums " + checksums, e);
        }
        return results;
    }

    private Collection<String> extractValid(ChecksumType checksumType, Collection<String> checksums) {
        Collection<String> results = Sets.newHashSet();
        results.addAll(checksums.stream().filter(checksumType::isValid).collect(Collectors.toList()));
        return results;
    }

    @Override
    public GarbageCollectorInfo garbageCollect() {
        notifyGCListenersOnStart();
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
        notifyGCListenersOnDelete(binsToDelete);
        if (result.candidatesForDeletion > 0) {
            log.info("Found {} candidates for deletion", result.candidatesForDeletion);
        }
        for (BinaryData bd : binsToDelete) {
            log.trace("Candidate for deletion: {}", bd);
            dbService.invokeInTransaction("BinaryCleaner#" + bd.getSha1(), new BinaryCleaner(bd, result));
        }

        if (result.checksumsCleaned > 0) {
            result.archivePathsCleaned = getTransactionalMe().deleteUnusedArchivePaths();
            result.archiveNamesCleaned = getTransactionalMe().deleteUnusedArchiveNames();
        }

        result.gcEndTime = System.currentTimeMillis();

        try {
            BinariesInfo countAndSize = binariesDao.getCountAndTotalSize();
            result.printCollectionInfo(countAndSize.getBinariesSize());
        } catch (SQLException e) {
            log.error("Could not list files due to " + e.getMessage());
        }
        notifyGCListenersOnFinished(result);
        return result;
    }

    private void notifyGCListenersOnStart() {
        garbageCollectorListeners.forEach(org.artifactory.storage.binstore.service.GarbageCollectorListener::start);
    }

    private void notifyGCListenersOnDelete(Collection<BinaryData> binsToDelete) {
        for (GarbageCollectorListener garbageCollectorListener : garbageCollectorListeners) {
            garbageCollectorListener.toDelete(binsToDelete);
        }
    }

    private void notifyGCListenersOnFinished(GarbageCollectorInfo result) {
        for (GarbageCollectorListener garbageCollectorListener : garbageCollectorListeners) {
            garbageCollectorListener.finished(result);
        }
    }

    private void notifyGCListenersOnDestroy() {
        garbageCollectorListeners.forEach(org.artifactory.storage.binstore.service.GarbageCollectorListener::destroy);
    }

    @Override
    public Set<String> isInStore(Set<String> sha1List) {
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

    @Override
    public Boolean getAndResetBinaryProvidersStatus(boolean promoteOtherNodes) {
        HaNodeProperties haNodeProperties = ArtifactoryHome.get().getHaNodeProperties();
        String serverId="node";
        Boolean result=false;
        if(promoteOtherNodes) {
            if (haNodeProperties != null) {
                serverId = haNodeProperties.getServerId();
            }
            AddonsManager addonsManager = ContextHelper.get().beanForType(AddonsManager.class);
            HaCommonAddon haCommonAddon = addonsManager.addonByType(HaCommonAddon.class);
            result= haCommonAddon.getAndResetBinaryProvidersStatus();
        }
        result= result ||firstBinaryProvider.visit(new StatusBinaryProviderVisitor(serverId)).size()>0;
        return result;
    }

    @Override
    public void notifyError(String id, String type) {
        AddonsManager addonsManager = ContextHelper.get().beanForType(AddonsManager.class);
        MailService mailService = ContextHelper.get().beanForType(MailService.class);
        CoreAddons coreAddons = addonsManager.addonByType(CoreAddons.class);
        List<String> adminEmails = coreAddons.getUsersForBackupNotifications();
        for (String adminEmail : adminEmails) {
            mailService.sendMail(new String[]{adminEmail}, "Critical binary provider error", "Critical Error occurred in Artifactory binary provider with id:" + id + " type:" + type + ", disabling single binary provider");
        }
    }

    @Override
    public void balanceProviders() {
        firstBinaryProvider.visit(new BalanceVisitor());
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
            result.addAll(allBinaries.stream().map(this::convertToBinaryInfo).collect(Collectors.toList()));
            return result;
        } catch (SQLException e) {
            throw new StorageException("Could not retrieve all binary entries", e);
        }
    }

    @Override
    @Nonnull
    public BinaryInfo insertRecordInDb(String sha1, String md5, long length) throws StorageException {
        BinaryData dataRecord = new BinaryData(sha1, md5, length);
        if (!dataRecord.isValid()) {
            throw new StorageException("Cannot insert invalid binary record: " + dataRecord);
        }
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
        FileBinaryProvider binaryProvider = fileBinaryProvider;
        if (binaryProvider != null) {
            if (!binaryProvider.isAccessible()) {
                throw new StorageException("Cannot access " +
                        binaryProvider.getBinariesDir().getAbsolutePath());
            }
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
    public void prune(BasicStatusHolder statusHolder) {
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
    @Override
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

    private class StatusBinaryProviderVisitor implements BinaryProviderVisitor<String> {
        private String serverId;

        public StatusBinaryProviderVisitor(String serverId) {
            this.serverId = serverId;
        }

        @Override
        public List<String> visit(BinaryProviderBase binaryProviderBase) {
            ArrayList<String> result = Lists.newArrayList();
            if (binaryProviderBase instanceof StateAwareBinaryProvider) {
                boolean active = ((StateAwareBinaryProvider) binaryProviderBase).isActive();
                if ( ! active) {
                    result.add(serverId + binaryProviderBase.getProviderMetaData().getId());
                    ((StateAwareBinaryProvider) binaryProviderBase).tryToActivate();
                }
            }
            return result;

        }
    }

    private class BalanceVisitor implements BinaryProviderVisitor<Object> {
        @Override
        public List<Object> visit(BinaryProviderBase binaryProviderBase) {
            if(binaryProviderBase instanceof Balanceable){
                ((Balanceable)binaryProviderBase).balance();
            }
            return Lists.newArrayList();
        }
    }


    private class FileBinaryProviderSearcher implements BinaryProviderVisitor<FileBinaryProvider>{
        @Override
        public List<FileBinaryProvider> visit(BinaryProviderBase binaryProviderBase) {
            ArrayList<FileBinaryProvider> result = Lists.newArrayList();
            if(binaryProviderBase instanceof FileBinaryProvider){
                result.add((FileBinaryProvider) binaryProviderBase);
            }
            return result;
        }
    }
}
