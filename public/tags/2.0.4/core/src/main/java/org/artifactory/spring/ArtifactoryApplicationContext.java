/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.artifactory.spring;

import de.schlichtherle.io.ArchiveException;
import de.schlichtherle.io.ArchiveDetector;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.artifactory.api.common.StatusHolder;
import org.artifactory.api.config.CentralConfigService;
import org.artifactory.api.config.ExportSettings;
import org.artifactory.api.config.ImportSettings;
import org.artifactory.api.config.ImportableExportable;
import org.artifactory.api.context.ArtifactoryContextThreadBinder;
import org.artifactory.api.repo.RepositoryService;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.api.security.SecurityService;
import org.artifactory.common.ArtifactoryHome;
import org.artifactory.common.ArtifactoryProperties;
import org.artifactory.descriptor.config.CentralConfigDescriptor;
import org.artifactory.jcr.JcrRepoService;
import org.artifactory.jcr.JcrService;
import org.artifactory.repo.service.InternalRepositoryService;
import org.artifactory.schedule.TaskService;
import org.artifactory.update.utils.BackupUtils;
import org.artifactory.version.ArtifactoryVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public class ArtifactoryApplicationContext extends ClassPathXmlApplicationContext
        implements InternalArtifactoryContext {
    private static final Logger log = LoggerFactory.getLogger(ArtifactoryApplicationContext.class);
    public static final String CURRENT_TIME_EXPORT_DIR_NAME = "current";
    private Set<Class<? extends ReloadableBean>> toInitialize = new HashSet<Class<? extends ReloadableBean>>();
    private boolean ready = false;
    private ConcurrentHashMap<Class, Object> beansForType = new ConcurrentHashMap<Class, Object>();
    private List<ReloadableBean> reloadableBeans;
    private long started;

    public ArtifactoryApplicationContext(String configLocation) throws BeansException {
        super(new String[]{configLocation}, false, null);
        started = System.currentTimeMillis();
        refresh();
    }

    public ArtifactoryApplicationContext(String[] configLocations) throws BeansException {
        super(configLocations, false, null);
        started = System.currentTimeMillis();
        refresh();
    }

    public long getUptime() {
        return System.currentTimeMillis() - started;
    }

    public CentralConfigService getCentralConfig() {
        return beanForType(CentralConfigService.class);
    }

    public SecurityService getSecurityService() {
        return beanForType(SecurityService.class);
    }

    public AuthorizationService getAuthorizationService() {
        return beanForType(AuthorizationService.class);
    }

    public TaskService getTaskService() {
        return beanForType(TaskService.class);
    }

    public RepositoryService getRepositoryService() {
        return beanForType(InternalRepositoryService.class);
    }

    public void addReloadableBean(Class<? extends ReloadableBean> beanClass) {
        toInitialize.add(beanClass);
    }

    @Override
    public void refresh() throws BeansException, IllegalStateException {
        try {
            // TODO: Check concurrency issue during reload/refresh
            ready = false;
            beansForType.clear();
            ArtifactoryContextThreadBinder.bind(this);
            super.refresh();
            reloadableBeans = new ArrayList<ReloadableBean>(toInitialize.size());
            Set<Class<? extends ReloadableBean>> toInit = new HashSet<Class<? extends ReloadableBean>>(toInitialize);
            for (Class<? extends ReloadableBean> beanClass : toInitialize) {
                orderReloadableBeans(toInit, beanClass);
            }
            log.debug("Reloadable list of beans: {}", reloadableBeans);
            for (ReloadableBean reloadableBean : reloadableBeans) {
                String beanIfc = getInterfaceName(reloadableBean);
                log.info("Initializing " + beanIfc);
                reloadableBean.init();
                log.debug("Initialized " + beanIfc);
            }
            ready = true;
            log.info("Artifactory application context ready.");
        } finally {
            ArtifactoryContextThreadBinder.unbind();
        }
    }

    public void init() {
        // Nothing
    }

    public Class<? extends ReloadableBean>[] initAfter() {
        throw new IllegalStateException("The context cannot be part of dependency ordering");
    }

    @Override
    public void destroy() {
        ready = false;
        ArtifactoryContextThreadBinder.bind(this);
        try {
            if (reloadableBeans != null && !reloadableBeans.isEmpty()) {
                log.debug("Destroying beans: {}", reloadableBeans);
                for (int i = reloadableBeans.size() - 1; i >= 0; i--) {
                    ReloadableBean bean = reloadableBeans.get(i);
                    String beanIfc = getInterfaceName(bean);
                    log.info("Destroying " + beanIfc);
                    try {
                        bean.destroy();
                    } catch (Exception e) {
                        log.error("Exception while destroying " + beanIfc);
                    }
                    log.debug("Destroyed " + beanIfc);
                }
            }
        } finally {
            super.destroy();
            ArtifactoryContextThreadBinder.unbind();
        }
    }

    private String getInterfaceName(ReloadableBean bean) {
        return bean.getClass().getInterfaces()[0].getName();
    }

    public void reload(CentralConfigDescriptor oldDescriptor) {
        ready = false;
        log.debug("Reloading beans: {}", reloadableBeans);
        for (ReloadableBean reloadableBean : reloadableBeans) {
            String beanIfc = getInterfaceName(reloadableBean);
            log.info("Reloading " + beanIfc);
            reloadableBean.reload(oldDescriptor);
            log.debug("Reloaded " + beanIfc);
        }
        ready = true;
    }

    private void orderReloadableBeans(Set<Class<? extends ReloadableBean>> toInit,
            Class<? extends ReloadableBean> beanClass) {
        if (!toInit.contains(beanClass)) {
            // Already done
            return;
        }
        ReloadableBean initializingBean = beanForType(beanClass);
        Class<? extends ReloadableBean>[] dependUpon = initializingBean.initAfter();
        for (Class<? extends ReloadableBean> doBefore : dependUpon) {
            if (!doBefore.isInterface()) {
                throw new IllegalStateException(
                        "Cannot order bean with implementation class.\n" +
                                " Please provide an interface extending " +
                                ReloadableBean.class.getName());
            }
            orderReloadableBeans(toInit, doBefore);
        }
        // Avoid double init
        if (toInit.remove(beanClass)) {
            reloadableBeans.add(initializingBean);
        }
    }

    public boolean isReady() {
        return ready;
    }

    @SuppressWarnings({"unchecked"})
    public <T> T beanForType(Class<T> type) {
        //No sych needed. Synch is done on write, so in the worst case we might end up with
        //a bean with the same value, which is fine
        T bean = (T) beansForType.get(type);
        if (bean == null) {
            //Find the bean
            Iterator iter = getBeansOfType(type).values().iterator();
            if (!iter.hasNext()) {
                throw new RuntimeException("Could not find bean of type '" + type.getName() + "'.");
            }
            bean = (T) iter.next();
        }
        beansForType.put(type, bean);
        return bean;
    }

    public JcrService getJcrService() {
        return beanForType(JcrService.class);
    }

    public JcrRepoService getJcrRepoService() {
        return beanForType(JcrRepoService.class);
    }

    public void importFrom(ImportSettings settings, StatusHolder status) {
        status.setStatus("### Beginning full system import ###", log);
        // First sync status and settings
        status.setFailFast(settings.isFailFast());
        status.setVerbose(settings.isVerbose());
        // First check the version of the folder imported
        ArtifactoryVersion backupVersion = BackupUtils.findVersion(settings.getBaseDir());
        // We don't support import from 125 and before
        ArtifactoryVersion supportFrom = ArtifactoryVersion.v125;
        if (backupVersion.before(supportFrom)) {
            throw new IllegalArgumentException("Folder " + settings.getBaseDir().getAbsolutePath() +
                    " contain an export from a version older than " + supportFrom.getValue() + ".\n" +
                    "Please use Artifactory Command Line command dump to import from theses versions!");
        }
        settings.setExportVersion(backupVersion);

        getCentralConfig().importFrom(settings, status);
        getRepositoryService().importFrom(settings, status);
        getSecurityService().importFrom(settings, status);
        status.setStatus("### Full system import finished ###", log);
    }

    public void exportTo(ExportSettings settings, StatusHolder status) {
        // First sync status and settings
        status.setFailFast(settings.isFailFast());
        status.setVerbose(settings.isVerbose());
        log.info("Beginning full system export...");
        status.setStatus("Creating export directory", log);
        String timestamp;
        boolean incremental = settings.isIncremental();
        if (!incremental) {
            DateFormat formatter = new SimpleDateFormat("yyyyMMdd.HHmmss");
            timestamp = formatter.format(settings.getTime());
        } else {
            timestamp = CURRENT_TIME_EXPORT_DIR_NAME;
        }
        File baseDir = settings.getBaseDir();

        //Only create a temp dir when not doing in-place backup (time == null), otherwise do
        //in-place and make sure all exports except repositories delete their target or write to temp before exporting
        File tmpExportDir;
        if (incremental) {
            //Will alwyas be baseDir/CURRENT_TIME_EXPORT_DIR_NAME
            tmpExportDir = new File(baseDir, timestamp);
            try {
                FileUtils.forceMkdir(tmpExportDir);
            } catch (IOException e) {
                throw new RuntimeException("Failed to create in place " +
                        tmpExportDir.getAbsolutePath() + " backup dir.", e);
            }
            status.setStatus("Using in place export directory '" + tmpExportDir.getAbsolutePath() + "'.", log);
        } else {
            tmpExportDir = new File(baseDir, timestamp + ".tmp");
            //Make sure the directory does not already exist
            try {
                FileUtils.deleteDirectory(tmpExportDir);
            } catch (IOException e) {
                throw new IllegalArgumentException(
                        "Failed to delete temp export directory: " + tmpExportDir.getAbsolutePath(), e);
            }
            try {
                FileUtils.forceMkdir(tmpExportDir);
            } catch (IOException e) {
                throw new RuntimeException("Failed to create temp backup dir.", e);
            }
            status.setStatus("Created temp export directory '" + tmpExportDir.getAbsolutePath() + "'.", log);
        }
        //Export the repositories to the temp dir
        ExportSettings exportSettings = new ExportSettings(tmpExportDir, settings);
        CentralConfigService centralConfig = getCentralConfig();
        centralConfig.exportTo(exportSettings, status);
        if (status.isError() && settings.isFailFast()) {
            return;
        }
        //Security export
        exportSecurity(exportSettings, status);
        if (status.isError() && settings.isFailFast()) {
            return;
        }
        //artifactory.properties export
        exportArtifactoryProperties(exportSettings, status);
        if (status.isError() && settings.isFailFast()) {
            return;
        }
        exportSystemProperties(exportSettings, status);
        if (incremental && settings.isCreateArchive()) {
            log.warn("Cannot create archive for an in place backup.");
        }
        if (!incremental) {
            //Create an archive if necessary
            if (settings.isCreateArchive()) {
                createArchive(status, timestamp, baseDir, tmpExportDir);
            } else {
                moveTmpToBackupDir(status, timestamp, baseDir, tmpExportDir);
            }
        } else {
            status.setCallback(tmpExportDir);
        }
        status.setStatus("Full system export completed successfully.", log);
    }

    private void moveTmpToBackupDir(StatusHolder status, String timestamp, File baseDir, File tmpExportDir) {
        //Delete any exiting final export dir
        File exportDir = new File(baseDir, timestamp);
        try {
            FileUtils.deleteDirectory(exportDir);
        } catch (IOException e) {
            log.warn("Failed to delete existing final export directory.", e);
        }
        //Switch the directories
        boolean success = tmpExportDir.renameTo(exportDir);
        if (!success) {
            log.error("Failed to move '" + tmpExportDir + "' to '" + exportDir + "'.");
        }
        status.setCallback(exportDir);
    }

    private void createArchive(StatusHolder status, String timestamp, File baseDir, File tmpExportDir) {
        status.setStatus("Creating archive...", log);
        de.schlichtherle.io.File tmpArchive = new de.schlichtherle.io.File(baseDir, timestamp + ".tmp.zip");
        boolean successful = new de.schlichtherle.io.File(tmpExportDir).copyAllTo(tmpArchive, ArchiveDetector.NULL, ArchiveDetector.DEFAULT);

        try {
            de.schlichtherle.io.File.umount();
        } catch (ArchiveException e) {
            throw new RuntimeException("Failed to create system export archive.", e);
        }

        String tempArchivePath = tmpArchive.getAbsolutePath();
        if (!successful) {
            status.setError("Failed to create zip file " + tempArchivePath, log);
            return;
        }

        //Delete the temp export dir
        try {
            FileUtils.deleteDirectory(tmpExportDir);
        } catch (IOException e) {
            log.warn("Failed to delete temp export directory.", e);
        }

        // From now on use only java.io.File for the file actions!

        //Delete any exiting final archive
        File archive = new File(baseDir, timestamp + ".zip");
        if (archive.exists()) {
            boolean deleted = archive.delete();
            if (!deleted) {
                status.setWarning("Failed to delete existing final export archive.", log);
            }
        }
        //Rename the archive file
        File tmpArchiveFile = new File(tempArchivePath);
        boolean success = tmpArchiveFile.renameTo(archive);
        if (!success) {
            status.setWarning(String.format("Failed to move '%s' to '%s'.",
                    tmpArchiveFile.getPath(), archive.getPath()), log);
        }
        status.setCallback(archive.getAbsoluteFile());
    }

    private void exportArtifactoryProperties(ExportSettings settings, StatusHolder status) {
        File artifactoryPropFile = ArtifactoryHome.getArtifactoryPropertiesFile();
        if (artifactoryPropFile.exists()) {
            try {
                FileUtils.copyFileToDirectory(artifactoryPropFile, settings.getBaseDir());
            } catch (IOException e) {
                status.setError("Failed to copy artifactory.properties file", e, log);
            }
        } else {
            status.setStatus("No KeyVal defined no export done", log);
        }
    }

    private void exportSystemProperties(ExportSettings settings, StatusHolder status) {
        FileOutputStream targetOutputStream = null;
        try {
            File dumpTargetFile = new File(settings.getBaseDir(),
                    ArtifactoryHome.ARTIFACTORY_SYSTEM_PROPERTIES_FILE);
            targetOutputStream = new FileOutputStream(dumpTargetFile);
            ArtifactoryProperties.get().store(targetOutputStream);
        } catch (IOException e) {
            status.setError("Failed to dump artifactory.system.properties file", e, log);
        } finally {
            IOUtils.closeQuietly(targetOutputStream);
        }
    }

    private void exportSecurity(ExportSettings settings, StatusHolder status) {
        ImportableExportable security = getSecurityService();
        if (security != null) {
            status.setStatus("Exporting security...", log);
            security.exportTo(settings, status);
        } else {
            status.setStatus("No security defined no export done", log);
        }
    }

    public List<ReloadableBean> getBeans() {
        return reloadableBeans;
    }
}
