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

package org.artifactory.support.core.bundle;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.artifactory.addon.AddonsManager;
import org.artifactory.addon.ha.HaCommonAddon;
import org.artifactory.addon.ha.semaphore.SemaphoreWrapper;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.common.ArtifactoryHome;
import org.artifactory.common.ConstantValues;
import org.artifactory.support.config.analysis.ThreadDumpConfiguration;
import org.artifactory.support.config.bundle.BundleConfiguration;
import org.artifactory.support.config.configfiles.ConfigFilesConfiguration;
import org.artifactory.support.config.descriptor.ConfigDescriptorConfiguration;
import org.artifactory.support.config.security.SecurityInfoConfiguration;
import org.artifactory.support.config.storage.StorageSummaryConfiguration;
import org.artifactory.support.config.system.SystemInfoConfiguration;
import org.artifactory.support.config.systemlogs.SystemLogsConfiguration;
import org.artifactory.support.core.annotations.CollectService;
import org.artifactory.support.core.compression.CompressionService;
import org.artifactory.support.core.exceptions.BundleConfigurationException;
import org.artifactory.support.core.exceptions.TempDirAccessException;
import org.artifactory.util.StringUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.PreDestroy;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Provides generic bundle generation capabilities
 *
 * @author Michael Pasternak
 */
public abstract class AbstractSupportBundleService implements SupportBundleService {

    private static final Logger log = LoggerFactory.getLogger(AbstractSupportBundleService.class);
    private static final String SUPPORT_BUNDLE_PREFIX = "support-bundle-";
    private static final String SUPPORT_BUNDLE_TIMESTAMP_PATTERN = "yyyyMMdd-HHmmssS";

    private volatile SemaphoreWrapper executionGuard;
    private volatile ExecutorService executorService;

    @Autowired
    private CompressionService compressionService;
    private static ImmutableList<Method> collectServices = null;

    /**
     * @return CollectServices
     */
    private static ImmutableList<Method> getCollectServices() {
        if (collectServices == null) {
            synchronized (AbstractSupportBundleService.class) {
                if (collectServices == null) {
                    collectServices = identifyCollectServices();
                }
            }
        }
        return collectServices;
    }

    /**
     * Collects SystemLogs
     *
     * @param progressLatch pipe listening for worker progress events
     * @param tmpDir output directory for produced content
     * @param configuration the runtime configuration
     *
     * @return operation result
     */
    @CollectService(priority = CollectService.Priority.LOW)
    private boolean collectSystemLogs(CountDownLatch progressLatch, File tmpDir, BundleConfiguration configuration) {
        if(configuration.isCollectSystemLogs()) {
            try {
                return doCollectSystemLogs(
                        tmpDir,
                        configuration.getSystemLogsConfiguration()
                );
            } finally {
                postProcess(progressLatch);
            }
        }
        return false;
    }

    /**
     * Collects SystemInfo
     *
     * @param progressLatch pipe listening for worker progress events
     * @param tmpDir output directory for produced content
     * @param configuration the runtime configuration
     *
     * @return operation result
     */
    @CollectService
    private boolean collectSystemInfo(CountDownLatch progressLatch, File tmpDir, BundleConfiguration configuration) {
        if(configuration.isCollectSystemInfo()) {
            try {
                return doCollectSystemInfo(
                        tmpDir,
                        configuration.getSystemInfoConfiguration()
                );
            } finally {
                postProcess(progressLatch);
            }
        }
        return false;
    }

    /**
     * Collects SecurityConfig
     *
     * @param progressLatch pipe listening for worker progress events
     * @param tmpDir output directory for produced content
     * @param configuration the runtime configuration
     *
     * @return operation result
     */
    @CollectService
    private boolean collectSecurityConfig(CountDownLatch progressLatch, File tmpDir, BundleConfiguration configuration) {
        if(configuration.isCollectSecurityConfig()) {
            try {
                return doCollectSecurityConfig(
                        tmpDir,
                        configuration.getSecurityInfoConfiguration()
                );
            } finally {
                postProcess(progressLatch);
            }
        }
        return false;
    }

    /**
     * Collects ConfigDescriptor
     *
     * @param progressLatch pipe listening for worker progress events
     * @param tmpDir output directory for produced content
     * @param configuration the runtime configuration
     *
     * @return operation result
     */
    @CollectService
    private boolean collectConfigDescriptor(CountDownLatch progressLatch, File tmpDir, BundleConfiguration configuration) {
        if(configuration.isCollectConfigDescriptor()) {
            try {
                return doCollectConfigDescriptor(
                        tmpDir,
                        configuration.getConfigDescriptorConfiguration()
                );
            } finally {
                postProcess(progressLatch);
            }
        }
        return false;
    }

    /**
     * Collects ConfigurationFiles
     *
     * @param progressLatch pipe listening for worker progress events
     * @param tmpDir output directory for produced content
     * @param configuration the runtime configuration
     *
     * @return operation result
     */
    @CollectService
    private boolean collectConfigurationFiles(CountDownLatch progressLatch, File tmpDir, BundleConfiguration configuration) {
        if(configuration.isCollectConfigurationFiles()) {
            try {
                return doCollectConfigurationFiles(
                        tmpDir,
                        configuration.getConfigFilesConfiguration()
                );
            } finally {
                postProcess(progressLatch);
            }
        }
        return false;
    }

    /**
     * Collects ThreadDump
     *
     * @param progressLatch pipe listening for worker progress events
     * @param tmpDir output directory for produced content
     * @param configuration the runtime configuration
     *
     * @return operation result
     */
    @CollectService(priority = CollectService.Priority.HIGH)
    private boolean collectThreadDump(CountDownLatch progressLatch, File tmpDir, BundleConfiguration configuration) {
        if(configuration.isCollectThreadDump()) {
            try {
                return doCollectThreadDump(
                        tmpDir,
                        configuration.getThreadDumpConfiguration()
                );
            } finally {
                postProcess(progressLatch);
            }
        }
        return false;
    }

    /**
     * Collects StorageSummary
     *
     * @param progressLatch pipe listening for worker progress events
     * @param tmpDir output directory for produced content
     * @param configuration the runtime configuration
     *
     * @return operation result
     */
    @CollectService
    private boolean collectStorageSummary(CountDownLatch progressLatch, File tmpDir, BundleConfiguration configuration) {
        if(configuration.isCollectStorageSummary()) {
            try {
                return doCollectStorageSummary(
                        tmpDir,
                        configuration.getStorageSummaryConfiguration()
                );
            } finally {
                postProcess(progressLatch);
            }
        }
        return false;
    }

    /**
     * Invoked post {@link @CollectService}
     *
     * @param progress
     */
    private void postProcess(CountDownLatch progress) {
        synchronized (progress) {
            progress.countDown();
            if (progress.getCount() > 0) {
                getLog().debug(
                        Thread.currentThread().getName() + " has finished, " +
                                "still left '{}' tasks to break the latch",
                        progress.getCount()
                );
            } else {
                getLog().debug(
                        Thread.currentThread().getName() + " has finished, all tasks are done!"
                );
            }
        }
    }

    /**
     * Performs post generation cleanup
     */
    private void postGenerate() {
        rollBundles();
        getExecutionGuard().release();
    }

    /**
     * Performs rolling up to {@link ConstantValues#maxBundles} bundles
     */
    private void rollBundles() {
        if (list().size() > ConstantValues.maxBundles.getInt()) {
            synchronized (this) {
                List<String> bundles = list();
                if (bundles.size() > ConstantValues.maxBundles.getInt()) {
                    try {
                        do {
                            // deleting bundle may take time if bundle size is too large,
                            // thus we delete it asynchronously
                            delete(bundles.get(bundles.size() - 1), true);
                            bundles = list();
                        } while (bundles.size() > ConstantValues.maxBundles.getInt());
                    } catch (FileNotFoundException e) {
                        getLog().debug("FileNotFound during rollBundles(): ", e);
                    }
                }
            }
        }
    }

    /**
     * Performs content collection of all {@link CollectService} products
     * and compresses output
     *
     * @param configuration the runtime configuration
     *
     * @return compressed archives/s
     */
    @Override
    public final List<String> generate(BundleConfiguration configuration) {

        String alreadyRunningMsg = "Another support content collection process already running, " +
                "please try again in few moments";
        try {
            if (!getExecutionGuard().tryAcquire(
                    ConstantValues.waitForSlotBeforeWithdraw.getInt(), TimeUnit.SECONDS)) {
                getLog().warn(alreadyRunningMsg);
            } else {
                try {
                    return doGenerate(configuration);
                } finally {
                    postGenerate();
                }
            }
        } catch (InterruptedException e) {
            getLog().debug("Interrupted while waiting for execution: {}", e);
            getLog().warn(alreadyRunningMsg);
        }
        return Lists.newLinkedList();
    }

    /**
     * Performs content collection of all {@link CollectService} products
     * and compresses output
     *
     * @param configuration the runtime configuration
     *
     * @return compressed archives/s
     */
    private List<String> doGenerate(BundleConfiguration configuration) {
        String executionId = Long.toString(System.currentTimeMillis());

        getLog().info("Initiating support content collection ...");

        getLog().debug("Creating output directory");
        File outputDirectory = createOutputDirectory(executionId);

        getLog().debug("Initiating content generation");
        generateContent(outputDirectory, executionId, configuration);

        getLog().debug("Initiating content compression");
        List<File> compressedContent = compressContent(outputDirectory, executionId, configuration);

        List<String> files = org.artifactory.support.utils.FileUtils.toFileNames(compressedContent);
        getLog().info("Support request content collection is done!, - " + files);

        return files;
    }

    /**
     * Creates output directory
     *
     * @param executionId The id of initiating context
     *
     * @return reference to output directory
     */
    private File createOutputDirectory(String executionId) {
        File tmpDir = getOutputDirectory();
        File archiveTmpDir = new File(
                tmpDir,
                SUPPORT_BUNDLE_PREFIX + DateTime.now()
                        .toString(SUPPORT_BUNDLE_TIMESTAMP_PATTERN) + "-" + executionId);
        try {
            FileUtils.forceMkdir(archiveTmpDir);
        } catch (IOException e) {
            throw new TempDirAccessException(
                    "Output directory creation has failed - " + e.getMessage(),
                    e
            );
        }
        return archiveTmpDir;
    }

    /**
     * @return support bundle output directory
     */
    @Override
    public File getOutputDirectory() {
        return ArtifactoryHome.get().getSupportDir();
    }

    /**
     * Collects and compresses generated content
     *
     * @param archiveTmpDir location of generated content
     * @param executionId The id of initiating context
     * @param configuration the runtime configuration
     *
     * @return reference to compressed archive
     */
    private List<File> compressContent(File archiveTmpDir, String executionId, BundleConfiguration configuration) {
        getLog().info("Compressing collected content ...");
        return compressionService.compress(
                archiveTmpDir,
                configuration.getBundleSize()
        );
    }

    /**
     * Invokes asynchronously all eligible {@link CollectService}
     *
     * @param archiveTmpDir location for generated content
     * @param progress progress signaling mechanism
     * @param configuration the runtime configuration
     */
    private void invokeContentCollection(File archiveTmpDir, CountDownLatch progress, String executionId, BundleConfiguration configuration) {
        if (getCollectServices().size() > 0) {
            for (Method task : getCollectServices()) {
                getLog().debug("Checking task '{}' for execution", task.getName());

                if(isTaskEnabled(task, configuration)) {
                    getLog().info("Scheduling task '" + task.getName() + "'");
                    AbstractSupportBundleService owner = this;
                    try {
                        submitAsyncTask(() -> {
                            try {
                                task.setAccessible(true);
                                task.invoke(owner, progress, archiveTmpDir, configuration);
                            } catch (IllegalAccessException | IllegalArgumentException e) {
                                getLog().error("Task '" + task.getName() + "' has failed");
                                getLog().debug("Cause: {}", e);
                            } catch (InvocationTargetException e) {
                                getLog().error("Task '" + task.getName() + "' has failed, " + getCause(e));
                                getLog().debug("Cause: {}", e);
                            }
                        });
                    } catch (RejectedExecutionException e) {
                        getLog().error("Task '" + task.getName() + "' has failed");
                        getLog().debug("Cause: {}", e);
                    }
                } else {
                    getLog().debug("Task '{}' is not eligible for execution", task.getName());
                }
            }
        }
    }

    /**
     * Fetches Throwable cause
     *
     * @param e exception to check
     * @return actual exception cause
     */
    private String getCause(InvocationTargetException e) {
        return (e.getCause() != null ? e.getCause().getMessage() : e.getMessage());
    }

    /**
     * Triggers async content generation and awaits for
     * all triggered tasks accomplishing
     *
     * @param archiveTmpDir location for generated content
     * @param executionId The id of initiating context
     * @param configuration the runtime configuration
     *
     * @return result
     */
    private boolean generateContent(File archiveTmpDir, String executionId, BundleConfiguration configuration) {
        CountDownLatch progress = createProgressPipe(configuration);
        getLog().info("Awaiting for tasks collecting content ...");
        invokeContentCollection(archiveTmpDir, progress, executionId, configuration);
        try {
            progress.await(
                    ConstantValues.contentCollectionAwaitTimeout.getInt(),
                    TimeUnit.MINUTES
            );
            getLog().info("All collecting tasks were accomplished!");
            return true;
        } catch (Exception e) {
            getLog().error("Await for collecting tasks has ended with error, - " +
                            e.getMessage()
            );
            getLog().debug("cause: {}", e);
        }
        return false;
    }

    /**
     * Produces pipe listening for worker progress events
     *
     * @param configuration the runtime configuration
     *
     * @return {@link CountDownLatch}
     */
    private CountDownLatch createProgressPipe(BundleConfiguration configuration) {
        return new CountDownLatch(getMembersCount(configuration));
    }

    /**
     * Collects all {@link CollectService}
     *
     * @return ImmutableList<Method>
     */
    private static ImmutableList<Method> identifyCollectServices() {
        return ImmutableList.copyOf(
                Arrays.stream(AbstractSupportBundleService.class.getDeclaredMethods())
                    .parallel()
                    .filter(m -> m.getAnnotation(CollectService.class) != null)
                    .sorted(new Comparator<Method>() {
                        @Override
                        public int compare(Method m1, Method m2) {
                            CollectService c1 = m1.getAnnotation(CollectService.class);
                            CollectService c2 = m2.getAnnotation(CollectService.class);
                            if (c1 != null && c2 != null)
                                return c2.priority().compareTo(c1.priority());
                            return 0;
                        }
                    })
                    .collect(Collectors.toList()
                )
        );
    }

    /**
     * Check whether given {@link CollectService} is enabled
     *
     * @param task task to check
     * @param configuration the runtime configuration
     *
     * @return boolean
     */
    private boolean isTaskEnabled(Method task, BundleConfiguration configuration) {
        try {
            String isMethodName = "is" + StringUtils.capitalize(task.getName());
            Method isMethod = configuration.getClass().getDeclaredMethod(isMethodName);
            if (isMethod != null) {
                return  (boolean) isMethod.invoke(configuration);
            } else {
                // should not happen
                getLog().debug("Could not find corresponding verification method to '{}'", task.getName());
            }
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            getLog().debug("Cannot verify if task enabled: {}", e);
        }
        return false;
    }

    /**
     * Calculates amount of collect services eligible for execution
     *
     * @param configuration the runtime configuration
     *
     * @return amount of {@link CollectService} eligible for execution
     */
    private int getMembersCount(BundleConfiguration configuration) {
        int workersCount=0;
        for (Method method : getCollectServices()) {
            boolean enabled = isTaskEnabled(method, configuration);
            workersCount += (enabled ? 1 : 0);
        }
        return workersCount;
    }

    /**
     * @return {@link SemaphoreWrapper}
     */
    private SemaphoreWrapper getExecutionGuard() {
        if (executionGuard == null) {
            synchronized (this) {
                if (executionGuard == null) {
                    AddonsManager addonsManager = ContextHelper.get().beanForType(AddonsManager.class);
                    HaCommonAddon haCommonAddon = addonsManager.addonByType(HaCommonAddon.class);
                    executionGuard = haCommonAddon.getSemaphore(HaCommonAddon.SUPPORT_BUNDLE_SEMAPHORE_NAME);
                }
            }
        }
        return executionGuard;
    }

    /**
     * @return {@link ExecutorService}
     */
    private ExecutorService getExecutorService() {
        if (executorService == null) {
            synchronized (this) {
                if (executorService == null) {
                    executorService = Executors.newFixedThreadPool(
                            Runtime.getRuntime().availableProcessors()
                    );
                }
            }
        }
        return executorService;
    }

    @PreDestroy
    private void destroy() {
        if(!getExecutorService().isShutdown())
            getExecutorService().shutdown();
    }

    /**
     * Lists previously created bundles
     *
     * @return archive/s
     */
    @Override
    public final List<String> list() {

        List<String> archives = Lists.newLinkedList();

        try {
             Files.walk(getOutputDirectory().toPath(), FileVisitOption.FOLLOW_LINKS)
                    .filter(Files::isRegularFile)
                    .filter(f -> f.toFile().getName().startsWith(SUPPORT_BUNDLE_PREFIX))
                    .filter (p -> FilenameUtils.getExtension(p.toString())
                            .equals(CompressionService.ARCHIVE_EXTENSION))
                     .sorted(new Comparator<Path>() {
                         @Override
                         public int compare(Path o1, Path o2) {
                             return o2.toString().compareTo(o1.toString());
                         }
                     })
                    .forEach(p -> archives.add(p.toFile().getName()));
        } catch (IOException e) {
            e.printStackTrace();
        }

        return archives;
    }

    /**
     * Downloads support bundles
     *
     * @param bundleName
     * @return {@link InputStream} to support bundle
     *         (user responsibility is to close stream upon consumption)
     *
     * @throws FileNotFoundException
     */
    @Override
    public final InputStream download(String bundleName) throws FileNotFoundException {
        assert !Strings.isNullOrEmpty(bundleName) : "bundleName cannot be empty";

        getLog().debug("Downloading support bundle '{}'", bundleName);
        return new FileInputStream(calculateBundlePath(bundleName));
        // TODO: check if stream can be closed by server (if not closed by client)
    }

    /**
     * Calculates file location on filesystem
     *
     * @param bundleName
     *
     * @return result
     */
    private String calculateBundlePath(String bundleName) {
        return calculateBundleFolder(bundleName) + File.separator + bundleName;
    }

    /**
     * Calculates bundle folder
     *
     * @param bundleName
     *
     * @return folder containing bundle location on filesystem
     */
    private String calculateBundleFolder(String bundleName) {
        return ArtifactoryHome.get().getSupportDir() + File.separator +
                StringUtils.replaceLast(bundleName, ("." + CompressionService.ARCHIVE_EXTENSION), "") ;
    }

    /**
     * Deletes support bundles
     *
     * @param bundleName name of bundle to delete
     * @param async whether delete should be performed asynchronously
     *
     * @return result
     *
     * @throws FileNotFoundException
     */
    @Override
    public final boolean delete(String bundleName, boolean async) throws FileNotFoundException {
        File file = new File(calculateBundlePath(bundleName));

        if (file.exists()) {
            if (async)
                return deleteAsync(bundleName, file);
            return deleteSync(bundleName, file);
        }
        throw new FileNotFoundException(bundleName);
    }

    /**
     * Deletes bundle asynchronously
     *
     * @param bundleName name
     * @param file bundle
     *
     * @return true if scheduling has succeeded, otherwise false
     */
    private boolean deleteAsync(String bundleName, File file) {
        try {
            submitAsyncTask(() -> deleteSync(bundleName, file));
            return true;
        } catch (RejectedExecutionException e) {
            getLog().warn("Deleting '" + bundleName + "' has failed, see logs for more details");
            getLog().debug("Cause: {}", e);
            return false;
        }
    }

    /**
     * Deletes bundle synchronously
     *
     * @param bundleName name
     * @param file bundle
     * @return success/failure
     */
    private boolean deleteSync(String bundleName, File file) {
        boolean result = true;
        try {
            result = file.delete();
            if (result)
                getLog().info("'" + bundleName + "' was successfully deleted");
            else
                getLog().warn("'" + bundleName + "' was not deleted");
            File folder = new File(calculateBundleFolder(bundleName));
            if (folder.isDirectory() && folder.list().length == 0) {
                FileUtils.deleteDirectory(folder);
            }
        } catch (IOException | SecurityException e) {
            getLog().warn("Deleting '" + bundleName + "' has failed, ", e.getMessage());
            getLog().debug("Cause: {}", e);
            result &= false;
        }
        return result;
    }

    /**
     * Submits async task for execution
     *
     * @param task {@link Runnable tp execute}
     */
    private void submitAsyncTask(Runnable task) {
        getExecutorService().submit(task);
    }

    /**
     * @return {@link Logger}
     */
    protected static Logger getLog() {
        return log;
    }

    /**
     * Collects SystemLogs
     *
     * @param tmpDir output directory for produced content
     * @param configuration the runtime configuration
     *
     * @return operation result
     */
    protected abstract boolean doCollectSystemLogs(File tmpDir, SystemLogsConfiguration configuration);
    /**
     * Collects SystemInfo
     *
     * @param tmpDir output directory for produced content
     * @param configuration the runtime configuration
     *
     * @return operation result
     */
    protected abstract boolean doCollectSystemInfo(File tmpDir, SystemInfoConfiguration configuration);
    /**
     * Collects SecurityConfig
     *
     * @param tmpDir output directory for produced content
     * @param configuration the runtime configuration
     *
     * @return operation result
     */
    protected abstract boolean doCollectSecurityConfig(File tmpDir, SecurityInfoConfiguration configuration);
    /**
     * Collects ConfigDescriptor
     *
     * @param tmpDir output directory for produced content
     * @param configuration the runtime configuration
     *
     * @return operation result
     */
    protected abstract boolean doCollectConfigDescriptor(File tmpDir, ConfigDescriptorConfiguration configuration);
    /**
     * Collects ConfigurationFiles
     *
     * @param tmpDir output directory for produced content
     * @param configuration the runtime configuration
     *
     * @return operation result
     */
    protected abstract boolean doCollectConfigurationFiles(File tmpDir, ConfigFilesConfiguration configuration);
    /**
     * Collects ThreadDump
     *
     * @param tmpDir output directory for produced content
     * @param configuration the runtime configuration
     *
     * @return operation result
     */
    protected abstract boolean doCollectThreadDump(File tmpDir, ThreadDumpConfiguration configuration);
    /**
     * Collects StorageSummary
     *
     * @param tmpDir output directory for produced content
     * @param configuration the runtime configuration
     *
     * @return operation result
     */
    protected abstract boolean doCollectStorageSummary(File tmpDir, StorageSummaryConfiguration configuration);
}
