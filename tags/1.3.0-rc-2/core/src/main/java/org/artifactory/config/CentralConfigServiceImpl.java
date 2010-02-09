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
import org.artifactory.api.common.StatusHolder;
import org.artifactory.api.config.ExportSettings;
import org.artifactory.api.config.ImportSettings;
import org.artifactory.api.config.VersionInfo;
import org.artifactory.api.security.AuthorizationException;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.common.ArtifactoryHome;
import org.artifactory.common.ConstantsValue;
import org.artifactory.config.jaxb.JaxbHelper;
import org.artifactory.descriptor.config.CentralConfigDescriptor;
import org.artifactory.descriptor.config.CentralConfigDescriptorImpl;
import org.artifactory.descriptor.config.MutableCentralConfigDescriptor;
import org.artifactory.descriptor.repo.ProxyDescriptor;
import org.artifactory.repo.index.IndexerService;
import org.artifactory.repo.service.InternalRepositoryService;
import org.artifactory.spring.InternalArtifactoryContext;
import org.artifactory.spring.InternalContextHelper;
import org.artifactory.spring.ReloadableBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
 * This class wraps the JAXB config descriptor.
 */
@Repository("centralConfig")
public class CentralConfigServiceImpl implements InternalCentralConfigService {
    private static final Logger log = LoggerFactory.getLogger(CentralConfigServiceImpl.class);

    private CentralConfigDescriptor descriptor;
    private MutableCentralConfigDescriptor editedDescriptor;
    private File originalConfigFile;
    private DateFormat dateFormatter;
    private String serverName;

    @Autowired
    private AuthorizationService authService;

    @Autowired
    private InternalRepositoryService repositoryService;

    @Autowired
    private IndexerService indexer;

    public CentralConfigServiceImpl() {
    }

    @SuppressWarnings({"unchecked"})
    public Class<? extends ReloadableBean>[] initAfter() {
        return new Class[0];
    }

    @PostConstruct
    public void register() {
        //Refresh ourselves from the default config file
        File configFilePath = ArtifactoryHome.getConfigFile();
        loadConfiguration(configFilePath);
        //If we read the configuration from a local file, store it transiently so that we
        //can overwrite the configuration on system import
        setOriginalConfigFile(configFilePath);
        InternalContextHelper.get().addReloadableBean(InternalCentralConfigService.class);
    }

    public void init() {
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
            log.info("No custom server id in configuration. Using hostname instead.");
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
        return new VersionInfo(ConstantsValue.artifactoryVersion.getString(),
                ConstantsValue.artifactoryRevision.getString());
    }

    public MutableCentralConfigDescriptor getDescriptorForEditing() {
        // TODO: support locking
        if (editedDescriptor == null) {
            // create a duplicate descriptor by reading the currrent configuration file
            editedDescriptor = JaxbHelper.readConfig(originalConfigFile);
        }
        return editedDescriptor;
    }

    private void discardEditedDescriptor() {
        editedDescriptor = null;
    }

    public void saveEditedDescriptorAndReload() {
        if (editedDescriptor == null) {
            throw new IllegalStateException("Mutable editing descriptor is null");
        }

        if (!authService.isAdmin()) {
            throw new AuthorizationException("Only admin user can save the artifactory config file");
        }

        // before doing anything do a sanity check that the edited descriptor is valid
        // will fail if not valid without affecting the current configuration
        File tmpNewConfig = new File(originalConfigFile.getAbsolutePath() + ".tmp");
        JaxbHelper.writeConfig(editedDescriptor, tmpNewConfig);
        JaxbHelper.readConfig(tmpNewConfig);// will fail if invalid

        // create a backup first
        File backupConfigFile = new File(originalConfigFile.getPath() + ".orig");
        try {
            FileUtils.copyFile(originalConfigFile, backupConfigFile);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create backup of the original config file", e);
        }

        // replace the config file
        boolean deleted = originalConfigFile.delete();
        if (!deleted) {
            throw new RuntimeException("Failed to delete original config file");
        }
        try {
            FileUtils.moveFile(tmpNewConfig, originalConfigFile);
        } catch (IOException e) {
            // copy from the backup
            try {
                FileUtils.copyFile(backupConfigFile, originalConfigFile);
            } catch (IOException e1) {
                throw new RuntimeException("Failed to recover original file from backup", e1);
            }
            throw new RuntimeException("Failed to replace current config file", e);
        }

        reload();
    }

    public void reload() {
        reloadConfiguration(originalConfigFile);
    }

    public void importFrom(ImportSettings settings, StatusHolder status) {
        File dirToImport = settings.getBaseDir();
        if ((dirToImport != null) && (dirToImport.isDirectory()) && (dirToImport.listFiles().length > 0)) {
            status.setStatus("Importing config...", log);
            File newConfigFile = new File(settings.getBaseDir(), ArtifactoryHome.ARTIFACTORY_CONFIG_FILE);
            if (newConfigFile.exists()) {
                // copy the newly imported config file to the current artifactory etc directory 
                File etcDir = ArtifactoryHome.getEtcDir();
                File tempConfFile = new File(etcDir, "import_" + ArtifactoryHome.ARTIFACTORY_CONFIG_FILE);
                try {
                    FileUtils.copyFile(newConfigFile, tempConfFile);
                } catch (IOException e) {
                    throw new RuntimeException("Failed to backup " + ArtifactoryHome.ARTIFACTORY_CONFIG_FILE, e);
                }
                status.setStatus("Reloading configuration from " + tempConfFile, log);
                reloadConfiguration(tempConfFile);
                init();
                status.setStatus("Configuration reloaded from " + newConfigFile, log);

                //Check that config files are not the same (e.g. if we reimport from the same place)
                if (!newConfigFile.equals(originalConfigFile)) {
                    // Switch the originial config file with the new one
                    status.setStatus("Switching config files...", log);
                    org.artifactory.util.FileUtils.switchFiles(originalConfigFile, tempConfFile);
                }
            }
        } else if (settings.isFailIfEmpty()) {
            String error = "The given base directory is either empty, or non-existant";
            throw new IllegalArgumentException(error);
        }
    }

    public void exportTo(ExportSettings settings, StatusHolder status) {
        status.setStatus("Exporting config...", log);
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
        log.info("Reloading configuration (using '" + path + "')...");
        try {
            discardEditedDescriptor();
            CentralConfigDescriptor oldDescriptor = getDescriptor();
            if (oldDescriptor == null) {
                throw new IllegalStateException("The system was not loaded, and a reload was called");
            }
            InternalArtifactoryContext ctx = InternalContextHelper.get();
            CentralConfigDescriptorImpl descriptor = JaxbHelper.readConfig(path);
            //setDescriptor() will set the new date formatter and server name
            setDescriptor(descriptor);
            ctx.reload(oldDescriptor);
            log.info("Reloaded configuration from '" + path + "'.");
        } catch (Exception e) {
            log.error("Failed to reload configuration from '" + path + "'.", e);
            throw new RuntimeException(e);
        }
    }

    public void reload(CentralConfigDescriptor oldDescriptor) {
        // Nothing to do
    }

    public void destroy() {
        // Nothing to do
    }

    private void loadConfiguration(File path) {
        log.info("Loading configuration (using '" + path + "')...");
        try {
            CentralConfigDescriptorImpl descriptor = JaxbHelper.readConfig(path);
            //setDescriptor() will set the new date formatter and server name
            setDescriptor(descriptor);
            log.info("Loaded configuration from '" + path + "'.");
        } catch (Exception e) {
            log.error("Failed to load configuration from '" + path + "'.", e);
            throw new RuntimeException(e);
        }
    }

    private void checkUniqueProxies() {
        List<ProxyDescriptor> proxies = getDescriptor().getProxies();
        Map<String, ProxyDescriptor> map = new HashMap<String, ProxyDescriptor>(proxies.size());
        for (ProxyDescriptor proxy : proxies) {
            String key = proxy.getKey();
            ProxyDescriptor oldProxy = map.put(key, proxy);
            if (oldProxy != null) {
                throw new RuntimeException("Duplicate proxy key in configuration: " + key + ".");
            }
        }
    }
}
