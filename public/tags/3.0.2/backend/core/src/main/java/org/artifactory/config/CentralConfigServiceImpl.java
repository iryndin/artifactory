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

package org.artifactory.config;

import com.google.common.base.Charsets;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.SerializationUtils;
import org.apache.commons.lang.StringUtils;
import org.artifactory.api.config.VersionInfo;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.api.security.AuthorizationException;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.api.security.SecurityService;
import org.artifactory.common.ArtifactoryHome;
import org.artifactory.common.ConstantValues;
import org.artifactory.common.MutableStatusHolder;
import org.artifactory.descriptor.config.CentralConfigDescriptor;
import org.artifactory.descriptor.config.MutableCentralConfigDescriptor;
import org.artifactory.descriptor.reader.CentralConfigReader;
import org.artifactory.descriptor.repo.ProxyDescriptor;
import org.artifactory.jaxb.JaxbHelper;
import org.artifactory.sapi.common.ExportSettings;
import org.artifactory.sapi.common.ImportSettings;
import org.artifactory.security.AccessLogger;
import org.artifactory.spring.ContextReadinessListener;
import org.artifactory.spring.InternalArtifactoryContext;
import org.artifactory.spring.InternalContextHelper;
import org.artifactory.spring.Reloadable;
import org.artifactory.storage.db.DbService;
import org.artifactory.storage.fs.service.ConfigsService;
import org.artifactory.util.Files;
import org.artifactory.util.SerializablePair;
import org.artifactory.version.ArtifactoryConfigVersion;
import org.artifactory.version.CompoundVersionDetails;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Repository;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class wraps the JAXB config descriptor.
 */
@Repository("centralConfig")
@Reloadable(beanClass = InternalCentralConfigService.class,
        initAfter = {DbService.class, ConfigurationChangesInterceptors.class})
public class CentralConfigServiceImpl implements InternalCentralConfigService, ContextReadinessListener {
    private static final Logger log = LoggerFactory.getLogger(CentralConfigServiceImpl.class);

    private CentralConfigDescriptor descriptor;
    private DateTimeFormatter dateFormatter;
    private String serverName;

    @Autowired
    private AuthorizationService authService;

    @Autowired
    private SecurityService securityService;

    @Autowired
    private ConfigsService configsService;

    @Autowired
    private ConfigurationChangesInterceptors interceptors;

    public CentralConfigServiceImpl() {
    }

    @Override
    public void init() {
        SerializablePair<CentralConfigDescriptor, Boolean> result = getCurrentConfig();
        CentralConfigDescriptor currentConfig = result.getFirst();
        boolean updateDescriptor = result.getSecond();
        setDescriptor(currentConfig, updateDescriptor);
    }

    private SerializablePair<CentralConfigDescriptor, Boolean> getCurrentConfig() {
        ArtifactoryHome artifactoryHome = ContextHelper.get().getArtifactoryHome();

        //First try to see if there is an import config file to load
        String currentConfigXml = artifactoryHome.getImportConfigXml();

        boolean updateDescriptor = true;

        //If no import config file exists, or is empty, continue as normal
        if (StringUtils.isBlank(currentConfigXml)) {
            //Check in DB
            String dbConfigName = ArtifactoryHome.ARTIFACTORY_CONFIG_FILE;
            if (configsService.hasConfig(dbConfigName)) {
                log.debug("Loading existing configuration from storage.");
                currentConfigXml = configsService.getConfig(dbConfigName);
                updateDescriptor = false;
            } else {
                log.info("Loading bootstrap configuration (artifactory home dir is {}).", artifactoryHome.getHomeDir());
                currentConfigXml = artifactoryHome.getBootstrapConfigXml();
            }
        }
        artifactoryHome.renameInitialConfigFileIfExists();
        log.trace("Current config xml is:\n{}", currentConfigXml);
        return new SerializablePair<>(new CentralConfigReader().read(currentConfigXml), updateDescriptor);
    }

    @Override
    public void setDescriptor(CentralConfigDescriptor descriptor) {
        setDescriptor(descriptor, true);
    }

    @Override
    public CentralConfigDescriptor getDescriptor() {
        return descriptor;
    }

    @Override
    public DateTimeFormatter getDateFormatter() {
        return dateFormatter;
    }

    @Override
    public String getServerName() {
        return serverName;
    }

    @Override
    public String format(long date) {
        return dateFormatter.print(date);
    }

    @Override
    public VersionInfo getVersionInfo() {
        return new VersionInfo(ConstantValues.artifactoryVersion.getString(),
                ConstantValues.artifactoryRevision.getString());
    }

    @Override
    public String getConfigXml() {
        return JaxbHelper.toXml(descriptor);
    }

    @Override
    public void setConfigXml(String xmlConfig) {
        CentralConfigDescriptor newDescriptor = new CentralConfigReader().read(xmlConfig);
        reloadConfiguration(newDescriptor);
        storeLatestConfigToFile(xmlConfig);
    }

    @Override
    public void setLogo(File logo) throws IOException {
        ArtifactoryHome artifactoryHome = ContextHelper.get().getArtifactoryHome();
        final File targetFile = new File(artifactoryHome.getLogoDir(), "logo");
        if (logo == null) {
            FileUtils.deleteQuietly(targetFile);
        } else {
            FileUtils.copyFile(logo, targetFile);
        }
    }

    @Override
    public boolean defaultProxyDefined() {
        List<ProxyDescriptor> proxyDescriptors = descriptor.getProxies();
        for (ProxyDescriptor proxyDescriptor : proxyDescriptors) {
            if (proxyDescriptor.isDefaultProxy()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public MutableCentralConfigDescriptor getMutableDescriptor() {
        return (MutableCentralConfigDescriptor) SerializationUtils.clone(descriptor);
    }

    @Override
    public void saveEditedDescriptorAndReload(CentralConfigDescriptor descriptor) {
        if (descriptor == null) {
            throw new IllegalStateException("Currently edited descriptor is null.");
        }

        if (!authService.isAdmin()) {
            throw new AuthorizationException("Only an admin user can save the artifactory configuration.");
        }

        // before doing anything do a sanity check that the edited descriptor is valid
        // will fail if not valid without affecting the current configuration
        // in any case we will use this newly loaded config as the descriptor
        String configXml = JaxbHelper.toXml(descriptor);
        setConfigXml(configXml);
    }

    @Override
    public void importFrom(ImportSettings settings) {
        MutableStatusHolder status = settings.getStatusHolder();
        File dirToImport = settings.getBaseDir();
        if ((dirToImport != null) && (dirToImport.isDirectory()) && (dirToImport.listFiles().length > 0)) {
            status.setStatus("Importing config...", log);
            File newConfigFile = new File(settings.getBaseDir(), ArtifactoryHome.ARTIFACTORY_CONFIG_FILE);
            if (newConfigFile.exists()) {
                status.setStatus("Reloading configuration from " + newConfigFile, log);
                String xmlConfig = Files.readFileToString(newConfigFile);
                setConfigXml(xmlConfig);
                status.setStatus("Configuration reloaded from " + newConfigFile, log);
            }
        } else if (settings.isFailIfEmpty()) {
            String error = "The given base directory is either empty, or non-existent";
            throw new IllegalArgumentException(error);
        }
    }

    @Override
    public void exportTo(ExportSettings settings) {
        MutableStatusHolder status = settings.getStatusHolder();
        status.setStatus("Exporting config...", log);
        File destFile = new File(settings.getBaseDir(), ArtifactoryHome.ARTIFACTORY_CONFIG_FILE);
        JaxbHelper.writeConfig(descriptor, destFile);
    }

    private void setDescriptor(CentralConfigDescriptor descriptor, boolean save) {
        log.trace("Setting central config descriptor for config #{}.", System.identityHashCode(this));

        if (save) {
            // call the interceptors before saving the new descriptor
            interceptors.onBeforeSave(descriptor);
        }

        this.descriptor = descriptor;
        checkUniqueProxies();
        //Create the date formatter
        String dateFormat = descriptor.getDateFormat();
        dateFormatter = DateTimeFormat.forPattern(dateFormat);
        //Get the server name
        serverName = descriptor.getServerName();
        if (serverName == null) {
            log.debug("No custom server name in configuration. Using hostname instead.");
            try {
                serverName = InetAddress.getLocalHost().getHostName();
            } catch (UnknownHostException e) {
                log.warn("Could not use local hostname as the server instance id: {}", e.getMessage());
                serverName = "localhost";
            }
        }
        if (save) {
            log.info("Saving new configuration in storage...");
            String configString = JaxbHelper.toXml(descriptor);
            configsService.addOrUpdateConfig(ArtifactoryHome.ARTIFACTORY_CONFIG_FILE, configString);
            log.info("New configuration saved.");
        }
    }

    private void reloadConfiguration(CentralConfigDescriptor newDescriptor) {
        log.info("Reloading configuration...");
        try {
            CentralConfigDescriptor oldDescriptor = getDescriptor();
            if (oldDescriptor == null) {
                throw new IllegalStateException("The system was not loaded, and a reload was called");
            }

            InternalArtifactoryContext ctx = InternalContextHelper.get();

            //setDescriptor() will set the new date formatter and server name
            setDescriptor(newDescriptor, true);

            ctx.reload(oldDescriptor);
            log.info("Configuration reloaded.");
            AccessLogger.configurationChanged();
            log.debug("Old configuration:\n{}", JaxbHelper.toXml(oldDescriptor));
            log.debug("New configuration:\n{}", JaxbHelper.toXml(newDescriptor));
        } catch (Exception e) {
            String msg = "Failed to reload configuration: " + e.getMessage();
            log.error(msg, e);
            throw new RuntimeException(msg, e);
        }
    }

    private void storeLatestConfigToFile(String configXml) {
        try {
            Files.writeContentToRollingFile(configXml,
                    new File(ArtifactoryHome.get().getEtcDir(), ArtifactoryHome.ARTIFACTORY_CONFIG_LATEST_FILE));
        } catch (IOException e) {
            log.error("Error occurred while performing a backup of the latest configuration.", e);
        }
    }

    @Override
    public void onContextCreated() {
        ArtifactoryHome artifactoryHome = ContextHelper.get().getArtifactoryHome();
        File logoUrlFile = new File(artifactoryHome.getLogoDir(), "logoUrl.txt");
        if (logoUrlFile.exists()) {
            BufferedReader fileReader = null;
            try {
                fileReader = new BufferedReader(
                        new InputStreamReader(new FileInputStream(logoUrlFile), Charsets.UTF_8));
                String url = fileReader.readLine();
                IOUtils.closeQuietly(fileReader);
                if (StringUtils.isNotBlank(url)) {
                    log.info("Setting logo url: {}", url);
                    MutableCentralConfigDescriptor mutableConfig = getMutableDescriptor();
                    mutableConfig.setLogo(url);
                    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                    securityService.authenticateAsSystem();
                    try {
                        saveEditedDescriptorAndReload(mutableConfig);
                    } finally {
                        // restore the previous authentication
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    }
                }
                logoUrlFile.delete();
            } catch (Exception e) {
                log.error("Failed to read logo url", e);
            } finally {
                IOUtils.closeQuietly(fileReader);
            }
        }
    }

    @Override
    public void onContextUnready() {
    }

    @Override
    public void onContextReady() {
    }

    @Override
    public void reload(CentralConfigDescriptor oldDescriptor) {
        // Nothing to do
    }

    @Override
    public void destroy() {
        // Nothing to do
    }

    // Convert and save the artifactory config descriptor

    @Override
    public void convert(CompoundVersionDetails source, CompoundVersionDetails target) {
        //Initialize the enum registration
        ArtifactoryConfigVersion.values();

        // getCurrentConfig() will always return the latest version (ie will do the conversion)
        CentralConfigDescriptor artifactoryConfig = getCurrentConfig().getFirst();
        // Save result in DB
        setDescriptor(artifactoryConfig);

        String artifactoryConfigXml = JaxbHelper.toXml(artifactoryConfig);

        // Save new bootstrap config file
        ArtifactoryHome artifactoryHome = ContextHelper.get().getArtifactoryHome();
        File parentFile = artifactoryHome.getEtcDir();
        if (parentFile.canWrite()) {
            try {
                log.info("Automatically converting the config file, original will be saved in " +
                        parentFile.getAbsolutePath());
                File bootstrapConfigFile = new File(parentFile, ArtifactoryHome.ARTIFACTORY_CONFIG_BOOTSTRAP_FILE);
                File newConfigFile;
                if (bootstrapConfigFile.exists()) {
                    newConfigFile = new File(parentFile, "new_" + ArtifactoryHome.ARTIFACTORY_CONFIG_BOOTSTRAP_FILE);
                } else {
                    newConfigFile = bootstrapConfigFile;
                }
                FileOutputStream fos = new FileOutputStream(newConfigFile);
                IOUtils.write(artifactoryConfigXml, fos);
                fos.close();
                if (newConfigFile != bootstrapConfigFile) {
                    Files.switchFiles(newConfigFile, bootstrapConfigFile);
                }
            } catch (Exception e) {
                log.warn("The converted config xml is:\n" + artifactoryConfigXml +
                        "\nThe new configuration is saved in DB but it failed to be saved automatically to '" +
                        parentFile.getAbsolutePath() + "' due to :" + e.getMessage() + ".\n", e);
            }
        } else {
            log.warn("The converted config xml is:\n" + artifactoryConfigXml +
                    "\nThe new configuration is saved in DB but it failed to be saved automatically to '" +
                    parentFile.getAbsolutePath() + "' since the folder is not writable.\n");
        }
    }

    private void checkUniqueProxies() {
        List<ProxyDescriptor> proxies = getDescriptor().getProxies();
        Map<String, ProxyDescriptor> map = new HashMap<>(proxies.size());
        for (ProxyDescriptor proxy : proxies) {
            String key = proxy.getKey();
            ProxyDescriptor oldProxy = map.put(key, proxy);
            if (oldProxy != null) {
                throw new RuntimeException("Duplicate proxy key in configuration: " + key + ".");
            }
        }
    }
}