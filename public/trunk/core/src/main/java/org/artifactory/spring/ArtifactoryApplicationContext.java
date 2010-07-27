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

package org.artifactory.spring;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.artifactory.addon.AddonsManager;
import org.artifactory.addon.WebstartAddon;
import org.artifactory.api.build.BuildService;
import org.artifactory.api.common.MultiStatusHolder;
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
import org.artifactory.descriptor.config.CentralConfigDescriptor;
import org.artifactory.jcr.JcrRepoService;
import org.artifactory.jcr.JcrService;
import org.artifactory.jcr.schedule.JcrGarbageCollectorJob;
import org.artifactory.log.LoggerFactory;
import org.artifactory.repo.index.IndexerJob;
import org.artifactory.repo.service.InternalRepositoryService;
import org.artifactory.schedule.TaskService;
import org.artifactory.update.utils.BackupUtils;
import org.artifactory.util.ZipUtils;
import org.artifactory.version.ArtifactoryVersion;
import org.artifactory.version.CompoundVersionDetails;
import org.artifactory.version.FatalConversionException;
import org.slf4j.Logger;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.jmx.access.MBeanProxyFactoryBean;

import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author yoavl
 */
public class ArtifactoryApplicationContext extends ClassPathXmlApplicationContext
        implements InternalArtifactoryContext {
    private static final Logger log = LoggerFactory.getLogger(ArtifactoryApplicationContext.class);

    public static final String CURRENT_TIME_EXPORT_DIR_NAME = "current";

    private Set<Class<? extends ReloadableBean>> toInitialize = new HashSet<Class<? extends ReloadableBean>>();
    private ConcurrentHashMap<Class, Object> beansForType = new ConcurrentHashMap<Class, Object>();
    private List<ReloadableBean> reloadableBeans;
    private final ArtifactoryHome artifactoryHome;
    private final String contextId;
    private final SpringConfigPaths springConfigPaths;
    private boolean ready;
    private long started;

    public ArtifactoryApplicationContext(
            String contextId, SpringConfigPaths springConfigPaths, ArtifactoryHome artifactoryHome)
            throws BeansException {
        super(springConfigPaths.getAllPaths(), false, null);
        this.contextId = contextId;
        this.artifactoryHome = artifactoryHome;
        this.springConfigPaths = springConfigPaths;
        this.started = System.currentTimeMillis();
        refresh();
    }

    public ArtifactoryHome getArtifactoryHome() {
        return artifactoryHome;
    }

    public String getContextId() {
        return contextId;
    }

    public SpringConfigPaths getConfigPaths() {
        return springConfigPaths;
    }

    public MBeanServer getMBeanServer() {
        //Delegate to the mbean server already created by the platform
        //return JmxUtils.locateMBeanServer();
        return ManagementFactory.getPlatformMBeanServer();
    }

    public <T> T getArtifactoryMBean(Class<T> mbeanIfc, String mbeanProps) {
        ObjectName mbeanName = createArtifactoryMBeanName(mbeanIfc, mbeanProps);
        MBeanProxyFactoryBean factory = new MBeanProxyFactoryBean();
        factory.setProxyInterface(mbeanIfc);
        try {
            factory.setObjectName(mbeanName);
        } catch (MalformedObjectNameException e) {
            throw new IllegalStateException("Unexpected failure when using an existing object name instance.", e);
        }
        factory.afterPropertiesSet();
        //noinspection unchecked
        return (T) factory.getObject();
        /*
        Use this once we are JDK 6+ only
        if (JMX.isMXBeanInterface(mbeanIfc)) {
            return JMX.newMXBeanProxy(getMBeanServer(), mbeanName, mbeanIfc);
        } else {
            return JMX.newMBeanProxy(getMBeanServer(), mbeanName, mbeanIfc);
        }
        */
    }

    public <T> T registerArtifactoryMBean(T mbean, Class<T> mbeanIfc, String mbeanProps) {
        ObjectName mbeanName = createArtifactoryMBeanName(mbeanIfc, mbeanProps);
        try {
            if (getMBeanServer().isRegistered(mbeanName)) {
                log.debug("Unregistering existing mbean '{}'.", mbeanName);
                getMBeanServer().unregisterMBean(mbeanName);
            }
            log.debug("Registering mbean '{}'.", mbeanName);
            getMBeanServer().registerMBean(mbean, mbeanName);
        } catch (Exception e) {
            throw new RuntimeException("Could not register new mbean '" + mbeanName + "'.", e);
        }
        return getArtifactoryMBean(mbeanIfc, mbeanProps);
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
            setReady(false);
            beansForType.clear();
            ArtifactoryContextThreadBinder.bind(this);
            super.refresh();
            reloadableBeans = new ArrayList<ReloadableBean>(toInitialize.size());
            Set<Class<? extends ReloadableBean>> toInit = new HashSet<Class<? extends ReloadableBean>>(toInitialize);
            for (Class<? extends ReloadableBean> beanClass : toInitialize) {
                orderReloadableBeans(toInit, beanClass);
            }
            log.debug("Reloadable list of beans: {}", reloadableBeans);
            boolean startedFormDifferentVersion = artifactoryHome.startedFromDifferentVersion();
            log.info("Artifactory context starting up...");
            if (startedFormDifferentVersion) {
                log.info("Conversion from previous version is active.");
            }
            for (ReloadableBean reloadableBean : reloadableBeans) {
                String beanIfc = getInterfaceName(reloadableBean);
                log.info("Initializing {}", beanIfc);
                if (startedFormDifferentVersion) {
                    //Run any necessary conversions to bring the system up to date with the current version
                    try {
                        reloadableBean.convert(
                                artifactoryHome.getOriginalVersionDetails(),
                                artifactoryHome.getRunningVersionDetails());
                    } catch (FatalConversionException e) {
                        //When a fatal conversion happens fail the context loading
                        log.error(
                                "Conversion failed with fatal status.\n" +
                                        "You should analyze the error and retry launching Artifactory. Error is: " +
                                        e.getMessage());
                        throw e;
                    } catch (Exception e) {
                        //When conversion fails - report and continue - don't fail
                        log.error("Failed to run configuration conversion.", e);
                    }
                }
                try {
                    reloadableBean.init();
                } catch (Exception e) {
                    throw new BeanInitializationException("Failed to initialize bean '" + beanIfc + "'.", e);
                }
                log.debug("Initialized {}", beanIfc);
            }
            //After all converters have run we can declare we are running on the latest version. We always update the
            //bundled version props to reflect revision changes, even if the version itself is current.
            artifactoryHome.writeBundledArtifactoryProperties();
            if (startedFormDifferentVersion) {
                artifactoryHome.initAndLoadSystemPropertyFile();
            }
            setReady(true);
        } finally {
            ArtifactoryContextThreadBinder.unbind();
        }
    }

    @Override
    protected void prepareBeanFactory(ConfigurableListableBeanFactory beanFactory) {
        super.prepareBeanFactory(beanFactory);
        //Add our own post processor that registers all reloadable beans automagically after construction
        beanFactory.addBeanPostProcessor(new BeanPostProcessor() {
            public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
                Class<?> targetClass = AopUtils.getTargetClass(bean);
                if (ReloadableBean.class.isAssignableFrom(targetClass)) {
                    Reloadable annotation;
                    if (targetClass.isAnnotationPresent(Reloadable.class)) {
                        annotation = targetClass.getAnnotation(Reloadable.class);
                        Class<? extends ReloadableBean> beanClass = annotation.beanClass();
                        addReloadableBean(beanClass);
                    } else {
                        throw new IllegalStateException("Bean " + targetClass.getName() +
                                " requires initialization beans to be initialized, but no such beans were found");
                    }
                }
                return bean;
            }

            public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
                //Do nothing
                return bean;
            }
        });
    }

    public void init() {
        // Nothing
    }

    @Override
    public void destroy() {
        setReady(false);
        ArtifactoryContextThreadBinder.bind(this);
        try {
            if (reloadableBeans != null && !reloadableBeans.isEmpty()) {
                log.debug("Destroying beans: {}", reloadableBeans);
                for (int i = reloadableBeans.size() - 1; i >= 0; i--) {
                    ReloadableBean bean = reloadableBeans.get(i);
                    String beanIfc = getInterfaceName(bean);
                    log.info("Destroying {}", beanIfc);
                    try {
                        bean.destroy();
                    } catch (Exception e) {
                        log.error("Exception while destroying {} ({}).", beanIfc, e.getMessage());
                    }
                    log.debug("Destroyed {}", beanIfc);
                }
            }
        } finally {
            ArtifactoryContextThreadBinder.unbind();
            super.destroy();
        }
    }

    public void convert(CompoundVersionDetails source, CompoundVersionDetails target) {
    }

    public void reload(CentralConfigDescriptor oldDescriptor) {
        setReady(false);
        log.debug("Reloading beans: {}", reloadableBeans);
        for (ReloadableBean reloadableBean : reloadableBeans) {
            String beanIfc = getInterfaceName(reloadableBean);
            log.debug("Reloading {}", beanIfc);
            reloadableBean.reload(oldDescriptor);
            log.debug("Reloaded {}", beanIfc);
        }
        setReady(true);
    }

    private void setReady(boolean ready) {
        this.ready = ready;
        if (hasBeanFactory()) {
            //Signal to all the context ready listener beans
            final Map<String, ContextReadinessListener> contextReadinessListeners =
                    beansForType(ContextReadinessListener.class);
            log.debug("Signaling context ready={} to context readiness listener beans.", ready);
            for (ContextReadinessListener bean : contextReadinessListeners.values()) {
                String beanIfc = getInterfaceName(bean);
                log.debug("Signaling context ready={} to {}.", ready, beanIfc);
                if (ready) {
                    bean.onContextReady();
                } else {
                    bean.onContextUnready();
                }
            }
        }
        if (ready) {
            log.info("Artifactory application context is ready.");
        }
    }

    private String getInterfaceName(Object bean) {
        return bean.getClass().getInterfaces()[0].getName();
    }

    private void orderReloadableBeans(Set<Class<? extends ReloadableBean>> beansLeftToInit,
            Class<? extends ReloadableBean> beanClass) {
        if (!beansLeftToInit.contains(beanClass)) {
            // Already done
            return;
        }
        ReloadableBean initializingBean = beanForType(beanClass);
        Class<?> targetClass = AopUtils.getTargetClass(initializingBean);
        Reloadable annotation;
        if (targetClass.isAnnotationPresent(Reloadable.class)) {
            annotation = targetClass.getAnnotation(Reloadable.class);
        } else {
            throw new IllegalStateException(
                    "Bean " + targetClass.getName() + " requires the @Reloadable annotation to be present.");
        }
        Class<? extends ReloadableBean>[] dependsUpon = annotation.initAfter();
        for (Class<? extends ReloadableBean> doBefore : dependsUpon) {
            //Sanity check that prerequisite bean was registered
            if (!toInitialize.contains(doBefore)) {
                throw new IllegalStateException(
                        "Bean '" + beanClass.getName() + "' requires bean '" + doBefore.getName() +
                                "' to be initialized, but no such bean is registered for init.");
            }
            if (!doBefore.isInterface()) {
                throw new IllegalStateException(
                        "Cannot order bean with implementation class.\n" +
                                " Please provide an interface extending " +
                                ReloadableBean.class.getName());
            }
            orderReloadableBeans(beansLeftToInit, doBefore);
        }
        // Avoid double init
        if (beansLeftToInit.remove(beanClass)) {
            reloadableBeans.add(initializingBean);
        }
    }

    public boolean isReady() {
        return ready;
    }

    @SuppressWarnings("unchecked")
    public <T> T beanForType(Class<T> type) {
        //No sych needed. Synch is done on write, so in the worst case we might end up with
        //a bean with the same value, which is fine
        T bean = (T) beansForType.get(type);
        if (bean == null) {
            Map<String, T> beans = getBeansOfType(type);
            if (beans.isEmpty()) {
                throw new RuntimeException("Could not find bean of type '" + type.getName() + "'.");
            }

            bean = beans.values().iterator().next(); // default to the first bean encountered
            if (beans.size() > 1) {
                // prefer beans marked as primary
                for (Map.Entry<String, T> beanEntry : beans.entrySet()) {
                    BeanDefinition beanDefinition = getBeanFactory().getBeanDefinition(beanEntry.getKey());
                    if (beanDefinition != null && beanDefinition.isPrimary()) {
                        bean = beanEntry.getValue();
                    }
                }
            }
        }
        beansForType.put(type, bean);
        return bean;
    }

    public <T> Map<String, T> beansForType(Class<T> type) {
        return getBeansOfType(type);
    }

    public <T> T beanForType(String name, Class<T> type) {
        return getBean(name, type);
    }

    public JcrService getJcrService() {
        return beanForType(JcrService.class);
    }

    public JcrRepoService getJcrRepoService() {
        return beanForType(JcrRepoService.class);
    }

    public void importFrom(ImportSettings settings) {
        MultiStatusHolder status = settings.getStatusHolder();
        status.setStatus("### Beginning full system import ###", log);
        // First sync status and settings
        status.setFailFast(settings.isFailFast());
        status.setVerbose(settings.isVerbose());
        // First check the version of the folder imported
        ArtifactoryVersion backupVersion = BackupUtils.findVersion(settings.getBaseDir());
        // We don't support import from 125 and below
        ArtifactoryVersion supportFrom = ArtifactoryVersion.v125;
        if (backupVersion.before(supportFrom)) {
            throw new IllegalArgumentException("Folder " + settings.getBaseDir().getAbsolutePath() +
                    " contains an export from a version older than " + supportFrom.getValue() + ".\n" +
                    "Please use the dump-legacy-dbs first, to dump this version's data, then import it " +
                    "into Artifactory.");
        }
        settings.setExportVersion(backupVersion);
        toggleImportExportRelatedTasks(false);
        try {
            importEtcDirectory(settings);
            getCentralConfig().importFrom(settings);
            getSecurityService().importFrom(settings);

            AddonsManager addonsManager = beanForType(AddonsManager.class);
            WebstartAddon webstartAddon = addonsManager.addonByType(WebstartAddon.class);
            webstartAddon.importKeyStore(settings);

            BuildService buildService = beanForType(BuildService.class);
            buildService.importFrom(settings);

            if (!settings.isExcludeContent()) {
                getRepositoryService().importFrom(settings);
            }
            status.setStatus("### Full system import finished ###", log);
        } finally {
            toggleImportExportRelatedTasks(true);
        }
    }

    public void exportTo(ExportSettings settings) {
        log.info("Beginning full system export...");
        MultiStatusHolder status = settings.getStatusHolder();
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
            //Will always be baseDir/CURRENT_TIME_EXPORT_DIR_NAME
            tmpExportDir = new File(baseDir, timestamp);
        } else {
            tmpExportDir = new File(baseDir, timestamp + ".tmp");
            //Make sure the directory does not already exist
            try {
                FileUtils.deleteDirectory(tmpExportDir);
            } catch (IOException e) {
                throw new RuntimeException(
                        "Failed to delete temp export directory: " + tmpExportDir.getAbsolutePath(), e);
            }
        }
        try {
            FileUtils.forceMkdir(tmpExportDir);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create backup dir: " + tmpExportDir.getAbsolutePath(), e);
        }
        status.setStatus("Using backup directory: '" + tmpExportDir.getAbsolutePath() + "'.", log);

        //Export the repositories to the temp dir
        ExportSettings exportSettings = new ExportSettings(tmpExportDir, settings);
        CentralConfigService centralConfig = getCentralConfig();

        toggleImportExportRelatedTasks(false);
        try {
            centralConfig.exportTo(exportSettings);
            if (status.isError() && settings.isFailFast()) {
                return;
            }
            //Security export
            exportSecurity(exportSettings);
            if (status.isError() && settings.isFailFast()) {
                return;
            }
            // keystore export
            AddonsManager addonsManager = beanForType(AddonsManager.class);
            WebstartAddon webstartAddon = addonsManager.addonByType(WebstartAddon.class);
            webstartAddon.exportKeyStore(exportSettings);
            if (status.isError() && settings.isFailFast()) {
                return;
            }
            //artifactory.properties export
            exportArtifactoryProperties(exportSettings);
            if (status.isError() && settings.isFailFast()) {
                return;
            }
            exportEtcDirectory(exportSettings);
            if (status.isError() && settings.isFailFast()) {
                return;
            }
            exportBuildInfo(exportSettings);

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
        } finally {
            toggleImportExportRelatedTasks(true);
        }
    }

    private void moveTmpToBackupDir(StatusHolder status, String timestamp, File baseDir,
            File tmpExportDir) {
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

        File tempArchiveFile = new File(baseDir, timestamp + ".tmp.zip");
        try {
            ZipUtils.archive(tmpExportDir, tempArchiveFile, true);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create system export archive.", e);
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
        boolean success = tempArchiveFile.renameTo(archive);
        if (!success) {
            status.setWarning(String.format("Failed to move '%s' to '%s'.", tempArchiveFile.getAbsolutePath(),
                    archive.getAbsolutePath()), log);
        }
        status.setCallback(archive.getAbsoluteFile());
    }

    private void exportArtifactoryProperties(ExportSettings settings) {
        MultiStatusHolder status = settings.getStatusHolder();
        File artifactoryPropFile = artifactoryHome.getArtifactoryPropertiesFile();
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

    private void exportEtcDirectory(ExportSettings settings) {
        try {
            File targetBackupDir = new File(settings.getBaseDir(), "etc");
            FileUtils.copyDirectory(artifactoryHome.getEtcDir(), targetBackupDir);
        } catch (IOException e) {
            settings.getStatusHolder().setError(
                    "Failed to export etc directory: " + artifactoryHome.getEtcDir().getAbsolutePath(), e, log);
        }
    }

    private void importEtcDirectory(ImportSettings settings) {
        File sourceBackupDir = new File(settings.getBaseDir(), "etc");
        if (!sourceBackupDir.exists()) {
            // older versions didn't export the etc directory
            log.info("Skipping etc directory import. File doesn't exist: " + sourceBackupDir.getAbsolutePath());
            return;
        }
        try {
            // copy the etc directory from the backup dir
            FileUtils.copyDirectory(sourceBackupDir, artifactoryHome.getEtcDir());
            // re-initialize the mime types mapping
            artifactoryHome.initAndLoadMimeTypes();
            // refresh the addons manager
            beanForType(AddonsManager.class).refresh();
        } catch (Exception e) {
            settings.getStatusHolder().setError(
                    "Failed to import etc directory: " + sourceBackupDir.getAbsolutePath(), e, log);
        }
    }

    private void exportSecurity(ExportSettings settings) {
        MultiStatusHolder status = settings.getStatusHolder();
        ImportableExportable security = getSecurityService();
        if (security != null) {
            status.setStatus("Exporting security...", log);
            security.exportTo(settings);
        } else {
            status.setStatus("No security defined no export done", log);
        }
    }

    private void exportBuildInfo(ExportSettings exportSettings) {
        MultiStatusHolder status = exportSettings.getStatusHolder();
        ImportableExportable build = beanForType(BuildService.class);
        if (build != null) {
            status.setStatus("Exporting build info...", log);
            build.exportTo(exportSettings);
        } else {
            status.setStatus("No build info defined. No export done", log);
        }
    }

    public List<ReloadableBean> getBeans() {
        return reloadableBeans;
    }

    private void toggleImportExportRelatedTasks(boolean start) {
        TaskService taskService = getTaskService();
        //Turn on/off indexing and garbage collection
        if (start) {
            taskService.resumeTasks(JcrGarbageCollectorJob.class);
            taskService.resumeTasks(IndexerJob.class);
        } else {
            taskService.stopTasks(IndexerJob.class, true);
            taskService.stopTasks(JcrGarbageCollectorJob.class, true);
        }
    }

    private ObjectName createArtifactoryMBeanName(Class mbeanIfc, String mbeanProps) {
        String type = mbeanIfc.getSimpleName();
        if (type.endsWith("MBean")) {
            type = type.substring(0, type.length() - 5);
        }
        String nameStr = null;
        try {
            nameStr = MBEANS_DOMAIN_NAME + "instance=" + contextId + ", type=" + type +
                    (StringUtils.isNotEmpty(mbeanProps) ? "," + mbeanProps : "");
            return new ObjectName(nameStr);
        } catch (MalformedObjectNameException e) {
            throw new IllegalArgumentException("Failed to create mbean name from '" + nameStr + "'.", e);
        }
    }
}
