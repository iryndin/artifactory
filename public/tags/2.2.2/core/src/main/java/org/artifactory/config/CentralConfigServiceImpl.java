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

package org.artifactory.config;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.SerializationUtils;
import org.apache.commons.lang.StringUtils;
import org.artifactory.addon.AddonsManager;
import org.artifactory.api.common.MultiStatusHolder;
import org.artifactory.api.config.ExportSettings;
import org.artifactory.api.config.ImportSettings;
import org.artifactory.api.config.VersionInfo;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.api.mime.ContentType;
import org.artifactory.api.security.AuthorizationException;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.api.util.Pair;
import org.artifactory.common.ArtifactoryHome;
import org.artifactory.common.ConstantValues;
import org.artifactory.descriptor.config.CentralConfigDescriptor;
import org.artifactory.descriptor.config.MutableCentralConfigDescriptor;
import org.artifactory.descriptor.reader.CentralConfigReader;
import org.artifactory.descriptor.repo.ProxyDescriptor;
import org.artifactory.jaxb.JaxbHelper;
import org.artifactory.jcr.JcrPath;
import org.artifactory.jcr.JcrService;
import org.artifactory.log.LoggerFactory;
import org.artifactory.repo.index.InternalIndexerService;
import org.artifactory.repo.service.InternalRepositoryService;
import org.artifactory.security.AccessLogger;
import org.artifactory.spring.InternalArtifactoryContext;
import org.artifactory.spring.InternalContextHelper;
import org.artifactory.spring.Reloadable;
import org.artifactory.spring.ReloadableBean;
import org.artifactory.version.ArtifactoryConfigVersion;
import org.artifactory.version.CompoundVersionDetails;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.io.File;
import java.io.FileOutputStream;
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
@Reloadable(beanClass = InternalCentralConfigService.class, initAfter = JcrService.class)
public class CentralConfigServiceImpl implements InternalCentralConfigService {
    private static final Logger log = LoggerFactory.getLogger(CentralConfigServiceImpl.class);

    private CentralConfigDescriptor descriptor;
    private DateFormat dateFormatter;
    private String serverName;

    @Autowired
    private AddonsManager addonsManager;

    @Autowired
    private AuthorizationService authService;

    @Autowired
    private InternalRepositoryService repositoryService;

    @Autowired
    private InternalIndexerService indexer;

    @Autowired
    private ConfigurationChangesInterceptors interceptors;

    public CentralConfigServiceImpl() {
    }

    @SuppressWarnings({"unchecked"})
    public Class<? extends ReloadableBean>[] initAfter() {
        return new Class[]{JcrService.class};
    }

    public void init() {
        Pair<CentralConfigDescriptor, Boolean> result = getCurrentConfig();
        CentralConfigDescriptor currentConfig = result.getFirst();
        boolean updateDescriptor = result.getSecond();
        setDescriptor(currentConfig, updateDescriptor);
        backupStartupConfigXml(currentConfig);
    }

    /**
     * Creates a backup of the startup configuration XML in the "etc" dir
     *
     * @param startupConfig Startup configuration content
     */
    private void backupStartupConfigXml(CentralConfigDescriptor startupConfig) {
        ArtifactoryHome artifactoryHome = ContextHelper.get().getArtifactoryHome();
        File startupConfigFile = new File(artifactoryHome.getEtcDir(), ArtifactoryHome.ARTIFACTORY_STARTUP_CONFIG_FILE);
        FileUtils.deleteQuietly(startupConfigFile);
        try {
            String startupConfigXml = JaxbHelper.toXml(startupConfig);
            FileUtils.writeStringToFile(startupConfigFile, startupConfigXml, "utf-8");
        } catch (IOException e) {
            log.warn("Unable to backup startup configuration file", e);
        }
    }

    private Pair<CentralConfigDescriptor, Boolean> getCurrentConfig() {
        ArtifactoryHome artifactoryHome = ContextHelper.get().getArtifactoryHome();

        //First try to see if there is an import config file to load
        String currentConfigXml = artifactoryHome.getImportConfigXml();

        boolean updateDescriptor = true;

        //If no import config file exists, or is empty, continue as normal
        if (StringUtils.isBlank(currentConfigXml)) {
            //Check in DB
            JcrService jcr = InternalContextHelper.get().getJcrService();
            String jcrConfPath = getCurrentConfigRootNodePath();
            if (jcr.itemNodeExists(jcrConfPath)) {
                log.info("Loading existing configuration from storage.");
                currentConfigXml = jcr.getString(jcrConfPath);
                updateDescriptor = false;
            } else {
                log.info("Loading bootstrap configuration (artifactory home dir is {}).", artifactoryHome.getHomeDir());
                currentConfigXml = artifactoryHome.getBootstrapConfigXml();
            }
        }
        log.trace("Current config xml is:\n{}", currentConfigXml);
        return new Pair<CentralConfigDescriptor, Boolean>(
                new CentralConfigReader().read(currentConfigXml), updateDescriptor);
    }

    public void setDescriptor(CentralConfigDescriptor descriptor) {
        setDescriptor(descriptor, true);
    }

    public CentralConfigDescriptor getDescriptor() {
        return descriptor;
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
        return new VersionInfo(ConstantValues.artifactoryVersion.getString(),
                ConstantValues.artifactoryRevision.getString());
    }

    public String getConfigXml() {
        return JaxbHelper.toXml(descriptor);
    }

    public void setConfigXml(String xmlConfig) {
        CentralConfigDescriptor newDescriptor = new CentralConfigReader().read(xmlConfig);
        reloadConfiguration(newDescriptor);
    }

    public void setLogo(File logo) throws IOException {
        ArtifactoryHome artifactoryHome = ContextHelper.get().getArtifactoryHome();
        final File targetFile = new File(artifactoryHome.getLogoDir(), "logo");
        if (logo == null) {
            FileUtils.deleteQuietly(targetFile);
        } else {
            FileUtils.copyFile(logo, targetFile);
        }
    }

    public boolean defaultProxyDefined() {
        List<ProxyDescriptor> proxyDescriptors = descriptor.getProxies();
        for (ProxyDescriptor proxyDescriptor : proxyDescriptors) {
            if (proxyDescriptor.isDefaultProxy()) {
                return true;
            }
        }
        return false;
    }

    public MutableCentralConfigDescriptor getMutableDescriptor() {
        return (MutableCentralConfigDescriptor) SerializationUtils.clone(descriptor);
    }

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
        CentralConfigReader centralConfigReader = new CentralConfigReader();
        CentralConfigDescriptor newDescriptor =
                centralConfigReader.read(JaxbHelper.toXml(descriptor));// will fail if invalid
        reloadConfiguration(newDescriptor);
    }

    public void importFrom(ImportSettings settings) {
        MultiStatusHolder status = settings.getStatusHolder();
        File dirToImport = settings.getBaseDir();
        if ((dirToImport != null) && (dirToImport.isDirectory()) && (dirToImport.listFiles().length > 0)) {
            status.setStatus("Importing config...", log);
            File newConfigFile = new File(settings.getBaseDir(), ArtifactoryHome.ARTIFACTORY_CONFIG_FILE);
            if (newConfigFile.exists()) {
                status.setStatus("Reloading configuration from " + newConfigFile, log);
                String xmlConfig = org.artifactory.util.FileUtils.readFileToString(newConfigFile);
                setConfigXml(xmlConfig);
                status.setStatus("Configuration reloaded from " + newConfigFile, log);
            }
        } else if (settings.isFailIfEmpty()) {
            String error = "The given base directory is either empty, or non-existant";
            throw new IllegalArgumentException(error);
        }
    }

    public void exportTo(ExportSettings settings) {
        MultiStatusHolder status = settings.getStatusHolder();
        status.setStatus("Exporting config...", log);
        File destFile = new File(settings.getBaseDir(), ArtifactoryHome.ARTIFACTORY_CONFIG_FILE);
        JaxbHelper.writeConfig(descriptor, destFile);
        //Export the local repositories
        // TODO: Remove from here
        if (!settings.isExcludeContent()) {
            repositoryService.exportTo(settings);
        }
    }

    private void setDescriptor(CentralConfigDescriptor descriptor, boolean save) {
        log.trace("Setting central config descriptor for config #{}.", System.identityHashCode(this));
        this.descriptor = descriptor;
        checkUniqueProxies();
        //Create the date formatter
        dateFormatter = new SimpleDateFormat(descriptor.getDateFormat());
        dateFormatter.setTimeZone(CentralConfigDescriptor.UTC_TIME_ZONE);
        //Get the server name
        serverName = descriptor.getServerName();
        if (serverName == null) {
            log.debug("No custom server id in configuration. Using hostname instead.");
            try {
                serverName = InetAddress.getLocalHost().getHostName();
            } catch (UnknownHostException e) {
                log.warn("Could not use local hostname as the server instance id: {}", e.getMessage());
                serverName = "localhost";
            }
        }
        if (save) {
            log.info("Saving new configuration in storage...");
            JcrService jcr = InternalContextHelper.get().getJcrService();
            jcr.setString(getConfigRootNodePath(), "current", JaxbHelper.toXml(descriptor),
                    ContentType.applicationXml.getMimeType(), authService.currentUsername());
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
            // call the interceptors before saving the new descriptor
            interceptors.onBeforeSave(newDescriptor);

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

    public void reload(CentralConfigDescriptor oldDescriptor) {
        // Nothing to do
    }

    public void destroy() {
        // Nothing to do
    }

    // Convert and save the artifactory config descriptor

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
                    org.artifactory.util.FileUtils.switchFiles(newConfigFile, bootstrapConfigFile);
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

    private static String getCurrentConfigRootNodePath() {
        return getConfigRootNodePath() + "/current";
    }

    private static String getConfigRootNodePath() {
        return JcrPath.get().getConfigJcrPath("artifactory");
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