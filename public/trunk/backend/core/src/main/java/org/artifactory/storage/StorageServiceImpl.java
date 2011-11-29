/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2011 JFrog Ltd.
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
import org.apache.jackrabbit.core.RepositoryImpl;
import org.artifactory.addon.AddonsManager;
import org.artifactory.addon.CoreAddons;
import org.artifactory.api.common.MultiStatusHolder;
import org.artifactory.api.config.CentralConfigService;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.api.storage.StorageUnit;
import org.artifactory.common.ArtifactoryHome;
import org.artifactory.common.ConstantValues;
import org.artifactory.descriptor.config.CentralConfigDescriptor;
import org.artifactory.descriptor.gc.GcConfigDescriptor;
import org.artifactory.jcr.JcrService;
import org.artifactory.jcr.JcrSession;
import org.artifactory.jcr.jackrabbit.ArtifactoryDbDataStoreImpl;
import org.artifactory.jcr.jackrabbit.ExtendedDbDataStore;
import org.artifactory.jcr.schedule.JcrGarbageCollectorJob;
import org.artifactory.jcr.utils.DerbyUtils;
import org.artifactory.jcr.utils.JcrUtils;
import org.artifactory.jcr.version.v240.ActualChecksumsConverter;
import org.artifactory.log.LoggerFactory;
import org.artifactory.schedule.BaseTaskServiceDescriptorHandler;
import org.artifactory.schedule.Task;
import org.artifactory.schedule.TaskBase;
import org.artifactory.schedule.TaskService;
import org.artifactory.schedule.TaskUtils;
import org.artifactory.spring.InternalContextHelper;
import org.artifactory.spring.Reloadable;
import org.artifactory.storage.mbean.Storage;
import org.artifactory.storage.mbean.StorageMBean;
import org.artifactory.version.CompoundVersionDetails;
import org.slf4j.Logger;
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
    private JcrService jcrService;

    @Autowired
    private TaskService taskService;

    private boolean derbyUsed;

    @Override
    public void compress(MultiStatusHolder statusHolder) {
        if (!derbyUsed) {
            statusHolder.setError("Compress command is not supported on current database type.", log);
            return;
        }

        logStorageSizes();
        DerbyUtils.compress(statusHolder);
        logStorageSizes();
    }

    @Override
    public void logStorageSizes() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n-----Derby storage sizes-----\n");
        ArtifactoryHome artifactoryHome = ContextHelper.get().getArtifactoryHome();
        File dataDir = artifactoryHome.getDataDir();
        // print the size of derby directories (derby is the new name, db and store for old installations)
        File[] dirs = {new File(dataDir, "derby"), new File(dataDir, "db"), new File(dataDir, "store")};
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
    public long getStorageSize() {
        JcrSession session = jcrService.getUnmanagedSession();
        try {
            RepositoryImpl repository = (RepositoryImpl) session.getRepository();
            ExtendedDbDataStore dataStore = JcrUtils.getExtendedDataStore(repository);
            return dataStore.getStorageSize();
        } catch (Exception e) {
            throw new RuntimeException("Failed to calculate storage size.", e);
        } finally {
            session.logout();
        }
    }

    @Override
    public void ping() {
        jcrService.ping();
    }

    @Override
    public void convertActualChecksums(MultiStatusHolder statusHolder) {
        if (new ActualChecksumsConverter().convertAllActualChecksumsProperties()) {
            statusHolder.setStatus("Conversion was successful", log);
        } else {
            statusHolder.setError("Conversion failed", log);
        }
    }

    @Override
    public void callManualGarbageCollect(MultiStatusHolder statusHolder) {
        taskService.checkCanStartManualTask(JcrGarbageCollectorJob.class, statusHolder);
        if (!statusHolder.isError()) {
            try {
                String firstToken = execOneGcAndWait(false);
                InternalStorageService me = InternalContextHelper.get().getBean(InternalStorageService.class);
                if (ConstantValues.gcUseV1.getBoolean()) {
                    me.asyncManualGarbageCollect(firstToken);
                }
                statusHolder
                        .setStatus("Artifactory Storage Garbage Collector process activated in the background!", log);
            } catch (Exception e) {
                statusHolder.setError("Error activating Artifactory Storage Garbage Collector: " + e.getMessage(), e,
                        log);
            }
        }
    }

    @Override
    public void pruneUnreferencedFileInDataStore(MultiStatusHolder statusHolder) {
        JcrSession session = jcrService.getUnmanagedSession();
        try {
            RepositoryImpl repository = (RepositoryImpl) session.getRepository();
            ExtendedDbDataStore dataStore = JcrUtils.getExtendedDataStore(repository);
            dataStore.pruneUnreferencedFileInDataStore(statusHolder);
        } finally {
            session.logout();
        }
    }

    @Override
    public void asyncManualGarbageCollect(String firstRunToken) {
        taskService.waitForTaskCompletion(firstRunToken);
        execOneGcAndWait(true);
    }

    @Override
    public void manualGarbageCollect() {
        try {
            //GC in-use-records weak references used by the file datastore
            System.gc();
            log.info("Scheduling manual garbage collector to run immediately.");
            execOneGcAndWait(true);
            if (ConstantValues.gcUseV1.getBoolean()) {
                //Run a second gc
                execOneGcAndWait(true);
            }
        } catch (RuntimeException e) {
            throw new RuntimeException("Error in executing the manual garbage collector.", e);
        }
    }

    private String execOneGcAndWait(boolean waitForCompletion) {
        TaskBase task = TaskUtils.createManualTask(JcrGarbageCollectorJob.class, 0L);
        task.addAttribute(JcrGarbageCollectorJob.FIX_CONSISTENCY, "true");
        String token = taskService.startTask(task, true);
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
        derbyUsed = DerbyUtils.isDerbyUsed();
        InternalContextHelper.get().registerArtifactoryMBean(new Storage(this), StorageMBean.class, null);
        CentralConfigDescriptor descriptor = centralConfigService.getDescriptor();
        new GcSchedulerHandler(descriptor.getGcConfig(), null).reschedule();
    }

    @Override
    public void reload(CentralConfigDescriptor oldDescriptor) {
        derbyUsed = DerbyUtils.isDerbyUsed();
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

    @Override
    public void exportDbDataStore(String destDir) {
        JcrSession session = jcrService.getUnmanagedSession();
        try {
            RepositoryImpl repository = (RepositoryImpl) session.getRepository();
            ExtendedDbDataStore dataStore = JcrUtils.getExtendedDataStore(repository);
            if (dataStore instanceof ArtifactoryDbDataStoreImpl) {
                try {
                    ((ArtifactoryDbDataStoreImpl) dataStore).exportData(destDir);
                } catch (Exception e) {
                    log.error("Failed to export datasource data.", e);
                }
            } else {
                throw new IllegalArgumentException("Datasource used is not a db one.");
            }
        } finally {
            session.logout();
        }
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
                    return (input != null) && JcrGarbageCollectorJob.class.isAssignableFrom(input.getType());
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
            TaskBase jcrGarbageCollectorTask;
            if (coreAddons.isAol()) {
                jcrGarbageCollectorTask = TaskUtils.createRepeatingTask(JcrGarbageCollectorJob.class,
                        TimeUnit.SECONDS.toMillis(ConstantValues.gcIntervalSecs.getLong()),
                        TimeUnit.SECONDS.toMillis(ConstantValues.gcDelaySecs.getLong()));
            } else {
                jcrGarbageCollectorTask = TaskUtils.createCronTask(JcrGarbageCollectorJob.class,
                        descriptor.getCronExp());
            }
            InternalContextHelper.get().getTaskService().startTask(jcrGarbageCollectorTask, manual);
        }

        @Override
        public GcConfigDescriptor findOldFromNew(@Nonnull GcConfigDescriptor newDescriptor) {
            return oldDescriptorHolder.isEmpty() ? null : oldDescriptorHolder.get(0);
        }
    }
}