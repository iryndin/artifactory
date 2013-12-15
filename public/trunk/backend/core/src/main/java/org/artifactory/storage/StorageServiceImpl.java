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

package org.artifactory.storage;

import com.google.common.base.Predicate;
import com.google.common.collect.Lists;
import org.apache.commons.io.FileUtils;
import org.artifactory.addon.AddonsManager;
import org.artifactory.addon.CoreAddons;
import org.artifactory.api.common.MultiStatusHolder;
import org.artifactory.api.config.CentralConfigService;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.api.storage.StorageQuotaInfo;
import org.artifactory.api.storage.StorageUnit;
import org.artifactory.common.ArtifactoryHome;
import org.artifactory.common.ConstantValues;
import org.artifactory.descriptor.config.CentralConfigDescriptor;
import org.artifactory.descriptor.gc.GcConfigDescriptor;
import org.artifactory.descriptor.quota.QuotaConfigDescriptor;
import org.artifactory.mbean.MBeanRegistrationService;
import org.artifactory.schedule.BaseTaskServiceDescriptorHandler;
import org.artifactory.schedule.Task;
import org.artifactory.schedule.TaskBase;
import org.artifactory.schedule.TaskService;
import org.artifactory.schedule.TaskUtils;
import org.artifactory.spring.InternalContextHelper;
import org.artifactory.spring.Reloadable;
import org.artifactory.storage.binstore.service.BinaryStoreGarbageCollectorJob;
import org.artifactory.storage.binstore.service.InternalBinaryStore;
import org.artifactory.storage.db.DbService;
import org.artifactory.storage.db.DbType;
import org.artifactory.storage.mbean.ManagedStorage;
import org.artifactory.version.CompoundVersionDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author yoavl
 */
@Service
@Reloadable(beanClass = InternalStorageService.class, initAfter = TaskService.class)
public class StorageServiceImpl implements InternalStorageService {
    private static final Logger log = LoggerFactory.getLogger(StorageServiceImpl.class);

    @Autowired
    private CentralConfigService centralConfigService;

    @Autowired
    private DbService dbService;

    @Autowired
    private InternalBinaryStore binaryStore;

    @Autowired
    private TaskService taskService;

    private boolean derbyUsed;

    @Override
    public void compress(MultiStatusHolder statusHolder) {
        if (!derbyUsed) {
            statusHolder.error("Compress command is not supported on current database type.", log);
            return;
        }

        logStorageSizes();
        dbService.compressDerbyDb(statusHolder);
        logStorageSizes();
    }

    @Override
    //TODO: [by YS] change to the new directories
    public void logStorageSizes() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n-----Derby storage sizes-----\n");
        ArtifactoryHome artifactoryHome = ContextHelper.get().getArtifactoryHome();
        File dataDir = artifactoryHome.getHaAwareDataDir();
        // print the size of derby directories (derby is the new name, db and store for old installations)
        File[] dirs = {new File(dataDir, "derby"), new File(dataDir, "db"), binaryStore.getBinariesDir()};
        for (File dir : dirs) {
            if (dir.exists()) {
                long sizeOfDirectory = FileUtils.sizeOfDirectory(dir);
                String sizeOfDirectoryGb = StorageUnit.toReadableString(sizeOfDirectory);
                sb.append(dir.getName()).append("=").append(sizeOfDirectory).append(" bytes ").append(" (").append(
                        (sizeOfDirectoryGb)).append(")").append("\n");
            }
        }
        sb.append("-----------------------");
        log.info(sb.toString());
    }

    @Override
    public void ping() {
        binaryStore.ping();
    }

    @Override
    public StorageQuotaInfo getStorageQuotaInfo(long fileContentLength) {
        CentralConfigDescriptor descriptor = centralConfigService.getDescriptor();
        QuotaConfigDescriptor quotaConfig = descriptor.getQuotaConfig();
        if (quotaConfig == null) {
            return null;
        }
        if (!quotaConfig.isEnabled()) {
            return null;
        }

        File binariesFolder = binaryStore.getBinariesDir();
        return new StorageQuotaInfo(binariesFolder, quotaConfig.getDiskSpaceLimitPercentage(),
                quotaConfig.getDiskSpaceWarningPercentage(), fileContentLength);
    }

    @Override
    public void callManualGarbageCollect(MultiStatusHolder statusHolder) {
        taskService.checkCanStartManualTask(BinaryStoreGarbageCollectorJob.class, statusHolder);
        if (!statusHolder.isError()) {
            try {
                execOneGcAndWait(true);
            } catch (Exception e) {
                statusHolder.error("Error activating Artifactory Storage Garbage Collector: " + e.getMessage(), e,
                        log);
            }
        }
    }

    @Override
    public void pruneUnreferencedFileInDataStore(MultiStatusHolder statusHolder) {
        binaryStore.prune(statusHolder);
    }

    private String execOneGcAndWait(boolean waitForCompletion) {
        TaskBase task = TaskUtils.createManualTask(BinaryStoreGarbageCollectorJob.class, 0L);
        String token = taskService.startTask(task, true, true);
        if (waitForCompletion) {
            taskService.waitForTaskCompletion(token);
        }
        return token;
    }

    @Override
    public boolean isDerbyUsed() {
        return derbyUsed;
    }

    @Override
    public void init() {
        derbyUsed = dbService.getDatabaseType() == DbType.DERBY;

        ContextHelper.get().beanForType(MBeanRegistrationService.class).
                register(new ManagedStorage(this, binaryStore), "Storage", "Binary Storage");

        CentralConfigDescriptor descriptor = centralConfigService.getDescriptor();
        new GcSchedulerHandler(descriptor.getGcConfig(), null).reschedule();
    }

    @Override
    public void reload(CentralConfigDescriptor oldDescriptor) {
        CentralConfigDescriptor descriptor = centralConfigService.getDescriptor();
        new GcSchedulerHandler(descriptor.getGcConfig(), oldDescriptor.getGcConfig()).reschedule();
    }

    @Override
    public void destroy() {
        new GcSchedulerHandler(null, null).unschedule();
    }

    @Override
    public void convert(CompoundVersionDetails source, CompoundVersionDetails target) {
        //nop
    }

    static class GcSchedulerHandler extends BaseTaskServiceDescriptorHandler<GcConfigDescriptor> {

        final List<GcConfigDescriptor> oldDescriptorHolder = Lists.newArrayList();
        final List<GcConfigDescriptor> newDescriptorHolder = Lists.newArrayList();

        GcSchedulerHandler(GcConfigDescriptor newDesc, GcConfigDescriptor oldDesc) {
            if (newDesc != null) {
                newDescriptorHolder.add(newDesc);
            }
            if (oldDesc != null) {
                oldDescriptorHolder.add(oldDesc);
            }
        }

        @Override
        public String jobName() {
            return "Garbage Collector";
        }

        @Override
        public List<GcConfigDescriptor> getNewDescriptors() {
            return newDescriptorHolder;
        }

        @Override
        public List<GcConfigDescriptor> getOldDescriptors() {
            return oldDescriptorHolder;
        }

        @Override
        public Predicate<Task> getAllPredicate() {
            return new Predicate<Task>() {
                @Override
                public boolean apply(@Nullable Task input) {
                    return (input != null) && BinaryStoreGarbageCollectorJob.class.isAssignableFrom(input.getType());
                }
            };
        }

        @Override
        public Predicate<Task> getPredicate(@Nonnull GcConfigDescriptor descriptor) {
            return getAllPredicate();
        }

        @Override
        public void activate(@Nonnull GcConfigDescriptor descriptor, boolean manual) {
            AddonsManager addonsManager = InternalContextHelper.get().beanForType(AddonsManager.class);
            CoreAddons coreAddons = addonsManager.addonByType(CoreAddons.class);
            TaskBase garbageCollectorTask;
            if (coreAddons.isAol()) {
                garbageCollectorTask = TaskUtils.createRepeatingTask(BinaryStoreGarbageCollectorJob.class,
                        TimeUnit.SECONDS.toMillis(ConstantValues.gcIntervalSecs.getLong()),
                        TimeUnit.SECONDS.toMillis(ConstantValues.gcDelaySecs.getLong()));
            } else {
                garbageCollectorTask = TaskUtils.createCronTask(BinaryStoreGarbageCollectorJob.class,
                        descriptor.getCronExp());
            }
            InternalContextHelper.get().getTaskService().startTask(garbageCollectorTask, manual);
        }

        @Override
        public GcConfigDescriptor findOldFromNew(@Nonnull GcConfigDescriptor newDescriptor) {
            return oldDescriptorHolder.isEmpty() ? null : oldDescriptorHolder.get(0);
        }
    }
}