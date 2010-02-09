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
package org.artifactory.webapp.spring;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.artifactory.backup.BackupHelper;
import org.artifactory.config.CentralConfig;
import org.artifactory.jcr.JcrHelper;
import org.artifactory.keyval.KeyVals;
import org.artifactory.maven.Maven;
import org.artifactory.process.StatusHolder;
import org.artifactory.security.SecurityHelper;
import org.artifactory.spring.ArtifactoryContext;
import org.codehaus.plexus.archiver.zip.ZipArchiver;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.springframework.web.context.support.XmlWebApplicationContext;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public class ArtifactoryWebApplicationContext extends XmlWebApplicationContext
        implements ArtifactoryContext {
    @SuppressWarnings({"UNUSED_SYMBOL", "UnusedDeclaration"})
    private final static Logger LOGGER =
            Logger.getLogger(ArtifactoryWebApplicationContext.class);

    public CentralConfig getCentralConfig() {
        return beanForType(CentralConfig.class);
    }

    public SecurityHelper getSecurity() {
        return beanForType(SecurityHelper.class);
    }

    public Maven getMaven() {
        return beanForType(Maven.class);
    }

    public BackupHelper getBackup() {
        return beanForType(BackupHelper.class);
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

    public JcrHelper getJcr() {
        return getCentralConfig().getJcr();
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

    public void exportTo(String basePath, StatusHolder status) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Beginning full system export...");
        }
        File tempDir = new File(basePath);
        try {
            FileUtils.deleteDirectory(tempDir);
        } catch (IOException e) {
            throw new IllegalArgumentException(
                    "Failed to delete export directory: " + tempDir.getAbsolutePath(), e);
        }
        //Sanity check
        try {
            FileUtils.forceMkdir(tempDir);
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to create export directory.", e);
        }
        status.setStatus("Exporting repositories...");
        getCentralConfig().exportTo(basePath, status);
        status.setStatus("Exporting security...");
        getSecurity().exportTo(basePath, status);
        getKeyVal().exportTo(basePath, status);
        status.setStatus("Creating archive...");
        ZipArchiver archiver = new ZipArchiver();
        //Avoid ugly output to stdout
        archiver.enableLogging(new ConsoleLogger(
                org.codehaus.plexus.logging.Logger.LEVEL_DISABLED, ""));
        File archive = new File(tempDir.getParentFile(), tempDir.getName() + ".zip");
        archiver.setDestFile(archive);
        try {
            archiver.addDirectory(tempDir);
            archiver.createArchive();
            status.setCallback(archive);
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("Full system export finished.");
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to create system export archive.", e);
        } finally {
            //Delete the dir
            try {
                FileUtils.deleteDirectory(tempDir);
            } catch (IOException e) {
                LOGGER.warn("Failed to delete export directory.", e);
            }
        }
    }
}
