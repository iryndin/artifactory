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
package org.artifactory.config;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.artifactory.api.common.StatusHolder;
import org.artifactory.api.config.CentralConfigService;
import org.artifactory.api.config.ExportSettings;
import org.artifactory.api.config.ImportSettings;
import org.artifactory.api.config.VersionInfo;
import org.artifactory.api.repo.BackupService;
import org.artifactory.common.ArtifactoryHome;
import org.artifactory.config.jaxb.JaxbHelper;
import org.artifactory.descriptor.backup.BackupDescriptor;
import org.artifactory.descriptor.config.CentralConfigDescriptor;
import org.artifactory.descriptor.repo.ProxyDescriptor;
import org.artifactory.keyval.KeyVals;
import org.artifactory.repo.index.IndexerManagerImpl;
import org.artifactory.repo.service.InternalRepositoryService;
import org.artifactory.spring.InternalContextHelper;
import org.artifactory.spring.PostInitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class wraps the JAXB config descriptor TODO: For some reason the Transactional annotation
 * does not work?
 */
@Repository("centralConfig")
public class CentralConfigServiceImpl implements CentralConfigService,
        PostInitializingBean {

    private final static Logger LOGGER = Logger.getLogger(CentralConfigServiceImpl.class);

    private CentralConfigDescriptor descriptor;
    private File originalConfigFile;
    private DateFormat dateFormatter;
    private String serverName;

    @Autowired
    private InternalRepositoryService repositoryService;

    @Autowired
    private KeyVals keyVals;

    @Autowired
    private IndexerManagerImpl indexerManager;

    public CentralConfigServiceImpl() {
    }

    /**
     * For testing only
     *
     * @param descriptor
     */
    CentralConfigServiceImpl(CentralConfigDescriptor descriptor) {
        setDescriptor(descriptor);
    }

    @SuppressWarnings({"unchecked"})
    public Class<? extends PostInitializingBean>[] initAfter() {
        return new Class[0];
    }

    @PostConstruct
    public void register() {
        //Refresh ourselves from the default config file
        File configFilePath = ArtifactoryHome.getConfigFile();
        reloadConfiguration(configFilePath);
        //If we read the configuration from a local file, store it transiently so that we
        //can overwrite the configuration on system import
        setOriginalConfigFile(configFilePath);
        InternalContextHelper.get().addPostInit(CentralConfigServiceImpl.class);
        //TODO: [by yl] REMOVE ME
        /*
        MavenWrapper wrapper = getMavenWrapper();
        Artifact artifact = wrapper.createArtifact("groovy", "groovy", "1.0-jsr-06",
                Artifact.SCOPE_COMPILE, "jar");
        wrapper.resolve(artifact, getLocalRepoDescriptors().get(0), getRemoteRepositories());
        */
    }

    public void init() {
        //Check the repositories directories create by ArtifactoryHome
        checkWritableDirectory(ArtifactoryHome.getDataDir());
        checkWritableDirectory(ArtifactoryHome.getJcrRootDir());
        //Create the deployment dir
        checkWritableDirectory(ArtifactoryHome.getWorkingCopyDir());
    }

    public void setDescriptor(CentralConfigDescriptor descriptor) {
        this.descriptor = descriptor;
        checkUniqueProxies();
        //Create the date formatter
        dateFormatter = new SimpleDateFormat(descriptor.getDateFormat());
        dateFormatter.setTimeZone(CentralConfigDescriptor.UTC_TIME_ZONE);
        //Get the server name
        serverName = descriptor.getServerName();
        if (serverName == null) {
            LOGGER.warn("Could not determine server instance id from configuration." +
                    " Using hostname instead.");
            try {
                serverName = InetAddress.getLocalHost().getHostName();
            } catch (UnknownHostException e) {
                throw new RuntimeException("Failed to use hostname as the server instacne id.", e);
            }
        }
    }

    public CentralConfigDescriptor getDescriptor() {
        return descriptor;
    }

    public File getOriginalConfigFile() {
        return originalConfigFile;
    }

    public DateFormat getDateFormatter() {
        return dateFormatter;
    }

    public String getServerName() {
        return serverName;
    }

    public synchronized String format(long date) {
        return dateFormatter.format(new Date(date));
    }

    public VersionInfo getVersionInfo() {
        return new VersionInfo(keyVals.getVersion(), keyVals.getRevision());
    }

    public void saveTo(String path) {
        new JaxbHelper<CentralConfigDescriptor>().write(path, descriptor);
    }

    public void reload() {
        reloadConfiguration(originalConfigFile);
        init();
        repositoryService.init();
    }

    public void importFrom(ImportSettings settings, StatusHolder status) {
        status.setStatus("Importing config...");
        File configFile = new File(settings.getBaseDir(), ArtifactoryHome.ARTIFACTORY_CONFIG_FILE);
        if (configFile.exists()) {
            status.setStatus("Reloading configuration from " + configFile);
            reloadConfiguration(configFile);
            init();
            status.setStatus("Configuration reloaded from " + configFile);
        }
        status.setStatus("Importing repositories...");
        repositoryService.importFrom(settings, status);
        status.setStatus("Backing up config...");
        if (configFile.exists()) {
            //Backup the config file and overwrite it
            try {
                //TODO: [by fs] Use the switch files method that rename with a number
                FileUtils.copyFile(originalConfigFile, new File(
                        originalConfigFile.getPath() + ".orig"));
                //Check that config files are not the same (e.g. if we reimport from the same place)
                if (!configFile.equals(originalConfigFile)) {
                    FileUtils.copyFile(configFile, originalConfigFile);
                }
            } catch (IOException e) {
                throw new RuntimeException(
                        "Failed to backup the " + ArtifactoryHome.ARTIFACTORY_CONFIG_FILE, e);
            }
        }
    }

    public void exportTo(ExportSettings settings, StatusHolder status) {
        status.setStatus("Exporting config...");
        File destFile = new File(settings.getBaseDir(), ArtifactoryHome.ARTIFACTORY_CONFIG_FILE);
        try {
            FileUtils.copyFile(originalConfigFile, destFile);
        } catch (IOException e) {
            throw new RuntimeException(
                    "Failed to copy " + originalConfigFile + " to " + destFile, e);
        }
        //Export the local repositories
        repositoryService.exportTo(settings, status);
    }

    void setOriginalConfigFile(File originalConfigFile) {
        this.originalConfigFile = originalConfigFile;
    }

    private void reloadConfiguration(File path) {
        LOGGER.info("Loading configuration (using '" + path + "')...");
        try {
            CentralConfigDescriptor oldDescriptor = getDescriptor();
            BackupService bckpService = null;
            if (oldDescriptor != null) {
                bckpService = InternalContextHelper.get().beanForType(BackupService.class);
                // We need to remove the backups
                List<BackupDescriptor> list = oldDescriptor.getBackups();
                if (list != null && !list.isEmpty()) {
                    bckpService.unschedule(list.size());
                }
            }
            CentralConfigDescriptor descriptor = JaxbHelper.readConfig(path);
            //setDescriptor() will set the new date formatter and server name
            setDescriptor(descriptor);
            if (oldDescriptor != null) {
                ((PostInitializingBean) bckpService).init();
            }
            LOGGER.info("Loaded configuration from '" + path + "'.");
        } catch (Exception e) {
            LOGGER.error("Failed to load configuration from '" + path + "'.", e);
            throw new RuntimeException(e);
        }
    }

    private static void checkWritableDirectory(File dir) {
        if (!dir.exists() || !dir.isDirectory() || !dir.canWrite()) {
            throw new IllegalArgumentException(
                    "Failed to create writable directory: " +
                            dir.getAbsolutePath());
        }
    }

    private void checkUniqueProxies() {
        List<ProxyDescriptor> proxies = getDescriptor().getProxies();
        Map<String, ProxyDescriptor> map = new HashMap<String, ProxyDescriptor>(proxies.size());
        for (ProxyDescriptor proxy : proxies) {
            String key = proxy.getKey();
            ProxyDescriptor oldProxy = map.put(key, proxy);
            if (oldProxy != null) {
                throw new RuntimeException(
                        "Duplicate proxy key in configuration: " + key + ".");
            }
        }
    }
}
