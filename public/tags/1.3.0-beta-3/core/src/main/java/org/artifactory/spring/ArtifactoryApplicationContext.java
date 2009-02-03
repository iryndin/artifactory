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

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.artifactory.api.common.StatusHolder;
import org.artifactory.api.config.CentralConfigService;
import org.artifactory.api.config.ExportSettings;
import org.artifactory.api.config.ImportSettings;
import org.artifactory.api.config.ImportableExportable;
import org.artifactory.api.context.ArtifactoryContextThreadBinder;
import org.artifactory.api.repo.RepositoryService;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.api.security.SecurityService;
import org.artifactory.jcr.JcrService;
import org.artifactory.keyval.KeyVals;
import org.artifactory.repo.service.InternalRepositoryService;
import org.springframework.beans.BeansException;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public class ArtifactoryApplicationContext extends ClassPathXmlApplicationContext
        implements InternalArtifactoryContext {
    private final static Logger LOGGER =
            Logger.getLogger(ArtifactoryApplicationContext.class);
    public static final String CURRENT_TIME_EXPORT_DIR_NAME = "current";
    private JcrService jcr;
    private Set<Class<? extends PostInitializingBean>> toInitialize =
            new HashSet<Class<? extends PostInitializingBean>>();
    private boolean ready = false;
    private ConcurrentHashMap<Class, Object> beansForType = new ConcurrentHashMap<Class, Object>();

    public ArtifactoryApplicationContext(String configLocation) throws BeansException {
        super(new String[]{configLocation}, false, null);
        refresh();
    }

    public ArtifactoryApplicationContext(String[] configLocations) throws BeansException {
        super(configLocations, false, null);
        refresh();
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

    public RepositoryService getRepositoryService() {
        return beanForType(InternalRepositoryService.class);
    }

    public void addPostInit(Class<? extends PostInitializingBean> beanClass) {
        toInitialize.add(beanClass);
    }

    @Override
    public void refresh() throws BeansException, IllegalStateException {
        try {
            beansForType.clear();
            ready = false;
            ArtifactoryContextThreadBinder.bind(this);
            super.refresh();
            Set<Class<? extends PostInitializingBean>> toInit =
                    new HashSet<Class<? extends PostInitializingBean>>(toInitialize);
            for (Class<? extends PostInitializingBean> beanClass : toInitialize) {
                initPostBean(toInit, beanClass);
            }
            ready = true;
            LOGGER.info("Artifactory application context ready.");
        } finally {
            ArtifactoryContextThreadBinder.unbind();
        }
    }

    private void initPostBean(Set<Class<? extends PostInitializingBean>> toInit,
            Class<? extends PostInitializingBean> beanClass) {
        if (!toInit.contains(beanClass)) {
            // Already done
            return;
        }
        PostInitializingBean initializingBean = beanForType(beanClass);
        Class<? extends PostInitializingBean>[] dependUpon = initializingBean.initAfter();
        for (Class<? extends PostInitializingBean> doBefore : dependUpon) {
            initPostBean(toInit, doBefore);
        }
        // Avoid double init
        if (toInit.remove(beanClass)) {
            LOGGER.info("Initializing " + beanClass);
            initializingBean.init();
            LOGGER.debug("Initialized " + beanClass);
        }
    }

    public boolean isReady() {
        return ready;
    }

    private ImportableExportable getSafeBean(String beanName) {
        ImportableExportable bean = null;
        try {
            bean = (ImportableExportable) getBeanFactory().getBean(beanName);
        } catch (BeansException e) {
            String message = "Cannot find bean '" + beanName + "' in safe mode!";
            LOGGER.warn(message);
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(message, e);
            }
        }
        return bean;
    }

    public KeyVals getKeyVal() {
        return beanForType(KeyVals.class);
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
        if (jcr == null) {
            jcr = beanForType(JcrService.class);
        }
        return jcr;
    }

    public void importFrom(ImportSettings settings, StatusHolder status) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("### Beginning full system import ###");
        }
        getCentralConfig().importFrom(settings, status);
        getSecurityService().importFrom(settings, status);
        getKeyVal().importFrom(settings, status);
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("### Full system import finished ###");
        }
    }

    public void exportTo(ExportSettings settings, StatusHolder status) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Beginning full system export...");
        }
        status.setStatus("Creating export directory");
        String timestamp;
        Date time = settings.getTime();
        if (time != null) {
            DateFormat formatter = new SimpleDateFormat("yyyyMMdd.HHmmss");
            timestamp = formatter.format(time);
        } else {
            timestamp = CURRENT_TIME_EXPORT_DIR_NAME;
        }
        File baseDir = settings.getBaseDir();
        File tmpExportDir = new File(baseDir, timestamp + ".tmp");
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
        status.setStatus("Created temp export directory '" + tmpExportDir.getAbsolutePath() + "'.");
        //Export the repositories to the temp dir
        ExportSettings exportSettings = new ExportSettings(tmpExportDir, settings);
        CentralConfigService centralConfig = getCentralConfig();
        centralConfig.exportTo(exportSettings, status);
        //Security export
        exportSecurity(exportSettings, status);
        //Keyvals export
        exportKeyVals(exportSettings, status);
        if (settings.isCreateArchive()) {
            //Create an archive if necessary
            status.setStatus("Creating archive...");
            File tmpArchive = new de.schlichtherle.io.File(baseDir, timestamp + ".tmp.zip");
            new de.schlichtherle.io.File(tmpExportDir).copyAllTo(tmpArchive);
            try {
                de.schlichtherle.io.File.umount();
            } catch (Exception e) {
                throw new RuntimeException("Failed to create system export archive.", e);
            } finally {
                //Delete the temp archive dir
                try {
                    FileUtils.deleteDirectory(tmpExportDir);
                } catch (IOException e) {
                    LOGGER.warn("Failed to delete temp export directory.", e);
                }
            }
            //Delete any exiting final archive
            File archive = new de.schlichtherle.io.File(baseDir, timestamp + ".zip");
            try {
                FileUtils.forceDelete(archive);
            } catch (IOException e) {
                LOGGER.warn("Failed to delete existing final export archive.", e);
            }
            //Switch the files
            boolean success = tmpArchive.renameTo(archive);
            if (!success) {
                LOGGER.error("Failed to move '" + tmpArchive.getPath() + "' to '" +
                        archive.getPath() + "'.");
            }
            status.setCallback(archive.getAbsoluteFile());
        } else {
            //Delete any exiting final export dir
            File exportDir = new File(baseDir, timestamp);
            try {
                FileUtils.deleteDirectory(exportDir);
            } catch (IOException e) {
                LOGGER.warn("Failed to delete existing final export directory.", e);
            }
            //Switch the directories
            boolean success = tmpExportDir.renameTo(exportDir);
            if (!success) {
                LOGGER.error("Failed to move '" + tmpExportDir + "' to '" + exportDir + "'.");
            }
            status.setCallback(exportDir);
        }

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Full system export completed successfully.");
        }
    }

    private void exportKeyVals(ExportSettings exportSettings, StatusHolder status) {
        ImportableExportable keyVal = getSafeBean("keyVal");
        if (keyVal != null) {
            keyVal.exportTo(exportSettings, status);
        } else {
            status.setStatus("No KeyVal defined no export done");
        }
    }

    private void exportSecurity(ExportSettings settings, StatusHolder status) {
        ImportableExportable security = getSecurityService();
        if (security != null) {
            status.setStatus("Exporting security...");
            security.exportTo(settings, status);
        } else {
            status.setStatus("No security defined no export done");
        }
    }
}
