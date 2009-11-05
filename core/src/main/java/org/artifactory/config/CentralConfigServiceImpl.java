/*
 * This file is part of Artifactory.
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
import org.artifactory.addon.CoreAddons;
import org.artifactory.api.common.MultiStatusHolder;
import org.artifactory.api.config.ExportSettings;
import org.artifactory.api.config.ImportSettings;
import org.artifactory.api.config.VersionInfo;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.api.security.AuthorizationException;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.common.ArtifactoryHome;
import org.artifactory.common.ConstantValues;
import org.artifactory.descriptor.config.CentralConfigDescriptor;
import org.artifactory.descriptor.config.CentralConfigDescriptorImpl;
import org.artifactory.descriptor.config.MutableCentralConfigDescriptor;
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
import org.artifactory.spring.ReloadableBean;
import org.artifactory.version.ArtifactoryConfigVersion;
import org.artifactory.version.CompoundVersionDetails;
import org.artifactory.version.converter.v136.LogbackConfigConverter;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import javax.annotation.PostConstruct;
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

    @PostConstruct
    public void register() {
        InternalContextHelper.get().addReloadableBean(InternalCentralConfigService.class);
    }

    public void init() {
        String currentConfigXml = getCurrentConfigXml();
        backupStartupConfigXml(currentConfigXml);
        setDescriptor(JaxbHelper.readConfig("bootstrapConfigXml", currentConfigXml));
    }

    /**
     * Creates a backup of the startup configuration XML in the "etc" dir
     *
     * @param startupConfigXml Startup configuration XML content
     */
    private void backupStartupConfigXml(String startupConfigXml) {
        ArtifactoryHome artifactoryHome = ContextHelper.get().getArtifactoryHome();
        File startupConfigFile = new File(artifactoryHome.getEtcDir(), ArtifactoryHome.ARTIFACTORY_STARTUP_CONFIG_FILE);
        FileUtils.deleteQuietly(startupConfigFile);
        try {
            FileUtils.writeStringToFile(startupConfigFile, startupConfigXml, "utf-8");
        } catch (IOException e) {
            log.warn("Unable to backup startup configuration file", e);
        }
    }

    private String getCurrentConfigXml() {
        String currentConfigXml;
        ArtifactoryHome artifactoryHome = ContextHelper.get().getArtifactoryHome();

        //First try to see if there is an import config file to load
        currentConfigXml = artifactoryHome.getImportConfigXml();

        //If no import config file exists, or is empty, continue as normal
        if (StringUtils.isBlank(currentConfigXml)) {
            //Check in DB
            JcrService jcr = InternalContextHelper.get().getJcrService();
            String jcrConfPath = getCurrentConfigRootNodePath();
            if (jcr.itemNodeExists(jcrConfPath)) {
                log.info("Loading existing configuration from storage.");
                currentConfigXml = jcr.getXml(jcrConfPath);
            } else {
                log.info("Loading bootstrap configuration (artifactory home dir is {}).", artifactoryHome.getHomeDir());
                currentConfigXml = artifactoryHome.getBootstrapConfigXml();
            }
        }
        log.trace("Current config xml is:\n{}", currentConfigXml);
        return currentConfigXml;
    }

    public void setDescriptor(CentralConfigDescriptor descriptor) {
        log.trace("Setting central config descriptor for config #{}.", System.identityHashCode(this));
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
        // Save result in DB
        JcrService jcr = InternalContextHelper.get().getJcrService();
        jcr.setXml(getConfigRootNodePath(), "current", JaxbHelper.toXml(descriptor), authService.currentUsername());
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
        // Check the version
        ArtifactoryConfigVersion configVersion = ArtifactoryConfigVersion.getConfigVersion(xmlConfig);
        if (!configVersion.getComparator().isCurrent()) {
            // We need convert
            xmlConfig = configVersion.convert(xmlConfig);
        }
        CentralConfigDescriptorImpl newDescriptor = JaxbHelper.readConfig("setConfigXml", xmlConfig);
        reloadConfiguration(newDescriptor);
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
        JaxbHelper.readConfig("SanityCheckReload", JaxbHelper.toXml(descriptor));// will fail if invalid

        reloadConfiguration(descriptor);
    }

    public void importFrom(ImportSettings settings) {
        MultiStatusHolder status = settings.getStatusHolder();
        File dirToImport = settings.getBaseDir();
        if ((dirToImport != null) && (dirToImport.isDirectory()) && (dirToImport.listFiles().length > 0)) {
            status.setStatus("Importing config...", log);
            File newConfigFile = new File(settings.getBaseDir(), ArtifactoryHome.ARTIFACTORY_CONFIG_FILE);
            if (newConfigFile.exists()) {
                status.setStatus("Reloading configuration from " + newConfigFile, log);
                String xmlConfig = JaxbHelper.xmlAsString(newConfigFile);
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
            setDescriptor(newDescriptor);
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

    public void convert(CompoundVersionDetails source, CompoundVersionDetails target) {
        String configXmlString = getCurrentConfigXml();

        //Auto convert and save if write permission to the etc folder
        //Initialize the enum registration
        ArtifactoryConfigVersion.values();
        ArtifactoryConfigVersion originalVersion =
                source.getVersion().getSubConfigElementVersion(ArtifactoryConfigVersion.class);
        String newConfigXml = originalVersion.convert(configXmlString);
        // Save result in DB
        JcrService jcr = InternalContextHelper.get().getJcrService();
        jcr.setXml(getConfigRootNodePath(), "current", newConfigXml, authService.currentUsername());

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
                IOUtils.write(newConfigXml, fos);
                fos.close();
                if (newConfigFile != bootstrapConfigFile) {
                    org.artifactory.util.FileUtils.switchFiles(newConfigFile, bootstrapConfigFile);
                }
            } catch (Exception e) {
                log.warn("The converted config xml is:\n" + newConfigXml +
                        "\nThe new configuration is saved in DB but it failed to be saved automatically to '" +
                        parentFile.getAbsolutePath() + "' due to :" + e.getMessage() + ".\n", e);
            }
        } else {
            log.warn("The converted config xml is:\n" + newConfigXml +
                    "\nThe new configuration is saved in DB but it failed to be saved automatically to '" +
                    parentFile.getAbsolutePath() + "' since the folder is not writable.\n");
        }

        /**
         * Checks via the original ArtifactoryConfigVersion if we need to convert the logback configuration file.
         * <p>
         * The reason this is done here and not in any converter mechanism is because we need access to the context,
         * which isn't available to the converter mechanisms.<br> See issue: RTFACT-1553 - On the fly conversion is not
         * scalable.
         */
        CoreAddons coreAddons = addonsManager.addonByType(CoreAddons.class);
        boolean logbackConversionRequired = coreAddons.isLogbackConversionRequired(originalVersion);
        if (logbackConversionRequired) {
            LogbackConfigConverter logbackConfigConverter = new LogbackConfigConverter();
            logbackConfigConverter.convert(artifactoryHome);
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