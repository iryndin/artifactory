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
import org.artifactory.config.CentralConfig;
import org.artifactory.config.ExportableConfig;
import org.artifactory.jcr.JcrWrapper;
import org.artifactory.keyval.KeyVals;
import org.artifactory.process.StatusHolder;
import org.artifactory.repo.LocalRepo;
import org.artifactory.security.ArtifactorySecurityManager;
import org.springframework.beans.BeansException;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public class ArtifactoryApplicationContext extends ClassPathXmlApplicationContext
        implements ArtifactoryContext {
    @SuppressWarnings({"UNUSED_SYMBOL", "UnusedDeclaration"})
    private final static Logger LOGGER =
            Logger.getLogger(ArtifactoryApplicationContext.class);
    public static final String CURRENT_TIME_EXPORT_DIR_NAME = "current";
    private JcrWrapper jcr;

    public ArtifactoryApplicationContext(String configLocation) throws BeansException {
        super(configLocation);
    }

    public CentralConfig getCentralConfig() {
        return beanForType(CentralConfig.class);
    }

    // TODO: Make an interface for the security manager
    public ArtifactorySecurityManager getSecurity() {
        return beanForType(ArtifactorySecurityManager.class);
    }

    private ExportableConfig getSafeBean(String beanName) {
        ExportableConfig bean = null;
        try {
            bean = (ExportableConfig) getBeanFactory().getBean(beanName);
        } catch (BeansException e) {
            String message = "Cannot find bean " + beanName + " in safe mode!";
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
        Iterator iter = getBeansOfType(type).values().iterator();
        if (!iter.hasNext()) {
            throw new RuntimeException("Could not find bean of type '" + type.getName() + "'.");
        }
        return (T) iter.next();
    }

    public JcrWrapper getJcr() {
        if (jcr == null) {
            jcr = beanForType(JcrWrapper.class);
        }
        return jcr;
    }

    @Override
    protected void onRefresh() {
        //Do nothing
    }

    public void importFrom(String basePath, StatusHolder status) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Beginning full system import...");
        }
        status.setStatus("Importing repositories...");
        getCentralConfig().importFrom(basePath, status);
        status.setStatus("Importing security...");
        getSecurity().importFrom(basePath, status);
        getKeyVal().importFrom(basePath, status);
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Full system import finished.");
        }
    }

    public void exportTo(File exportDir, StatusHolder status) {
        exportTo(exportDir, true, new Date(), status);
    }

    public void exportTo(File exportDir, boolean createArchive, Date time, StatusHolder status) {
        exportTo(exportDir, null, createArchive, new Date(), status);
    }

    public void exportTo(File basePath, List<LocalRepo> reposToExport, boolean createArchive,
            Date time, StatusHolder status) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Beginning full system export...");
        }
        status.setStatus("Creating export directory");
        String timestamp;
        if (time != null) {
            DateFormat formatter = new SimpleDateFormat("yyyyMMdd.HHmmss");
            timestamp = formatter.format(time);
        } else {
            timestamp = CURRENT_TIME_EXPORT_DIR_NAME;
        }
        File tmpExportDir = new File(basePath, timestamp + ".tmp");
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
        status.setStatus("Exporting repositories...");
        if (reposToExport != null) {
            getCentralConfig().exportTo(tmpExportDir, reposToExport, status);
        } else {
            getCentralConfig().exportTo(tmpExportDir, status);
        }
        ExportableConfig security = getSafeBean("security");
        if (security != null) {
            status.setStatus("Exporting security...");
            security.exportTo(tmpExportDir, status);
        } else {
            status.setStatus("No security defined no export done");
        }
        ExportableConfig keyVal = getSafeBean("keyVal");
        if (keyVal != null) {
            status.setStatus("Exporting key values...");
            keyVal.exportTo(tmpExportDir, status);
        } else {
            status.setStatus("No KeyVal defined no export done");
        }
        if (createArchive) {
            //Create an archive if necessary
            status.setStatus("Creating archive...");
            File tmpArchive = new de.schlichtherle.io.File(basePath, timestamp + ".tmp.zip");
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
            File archive = new de.schlichtherle.io.File(basePath, timestamp + ".zip");
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
            File exportDir = new File(basePath, timestamp);
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
}
