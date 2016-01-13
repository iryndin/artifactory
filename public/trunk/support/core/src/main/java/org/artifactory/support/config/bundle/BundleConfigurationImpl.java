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

package org.artifactory.support.config.bundle;

import org.artifactory.api.jackson.JacksonWriter;
import org.artifactory.support.config.configfiles.ConfigFilesConfiguration;
import org.artifactory.support.config.descriptor.ConfigDescriptorConfiguration;
import org.artifactory.support.config.analysis.ThreadDumpConfiguration;
import org.artifactory.support.config.security.SecurityInfoConfiguration;
import org.artifactory.support.config.storage.StorageSummaryConfiguration;
import org.artifactory.support.config.system.SystemInfoConfiguration;
import org.artifactory.support.config.systemlogs.SystemLogsConfiguration;

import java.io.IOException;
import java.util.Date;
import java.util.Optional;

/**
 * Generic bundle configuration
 *
 * @author Michael Pasternak
 */
public class BundleConfigurationImpl implements BundleConfiguration {

    private static final boolean FORCE_COLLECT_SYS_LOGS = true;
    public static final int DEFAULT_BUNDLE_SIZE = 10; // in Mb

    private SystemLogsConfiguration systemLogsConfiguration;
    private SystemInfoConfiguration systemInfoConfiguration;
    private SecurityInfoConfiguration securityInfoConfiguration;
    private ConfigDescriptorConfiguration configDescriptorConfiguration;
    private ConfigFilesConfiguration configFilesConfiguration;
    private StorageSummaryConfiguration storageSummaryConfiguration;
    private ThreadDumpConfiguration threadDumpConfiguration;

    private int bundleSize = DEFAULT_BUNDLE_SIZE;

    /**
     * serialization .ctr
     */
    public BundleConfigurationImpl() {
        this.systemLogsConfiguration = new SystemLogsConfiguration();
        this.systemInfoConfiguration = new SystemInfoConfiguration();
        this.securityInfoConfiguration =  new SecurityInfoConfiguration();
        this.configDescriptorConfiguration = new ConfigDescriptorConfiguration();
        this.configFilesConfiguration = new ConfigFilesConfiguration();
        this.storageSummaryConfiguration = new StorageSummaryConfiguration();
        this.threadDumpConfiguration = new ThreadDumpConfiguration();
    }

    /**
     * @param startDate collect logs from
     * @param endDate collect logs to
     * @param collectSystemInfo
     * @param collectSecurityConfig
     * @param hideUserDetails whether user details should be hidden (default true)
     * @param collectConfigDescriptor
     * @param collectConfigurationFiles
     * @param collectThreadDump
     * @param threadDumpCount amount of dumps to take
     * @param threadDumpInterval interval between dumps (millis)
     * @param collectStorageSummary
     * @param bundleSize
     */
    public BundleConfigurationImpl(Date startDate, Date endDate, boolean collectSystemInfo,
            boolean collectSecurityConfig, Optional<Boolean> hideUserDetails, boolean collectConfigDescriptor,
            boolean collectConfigurationFiles, boolean collectThreadDump, int threadDumpCount, long threadDumpInterval,
            boolean collectStorageSummary, Optional<Integer> bundleSize) {

        this.systemLogsConfiguration = new SystemLogsConfiguration(FORCE_COLLECT_SYS_LOGS, startDate, endDate);
        this.systemInfoConfiguration = new SystemInfoConfiguration(collectSystemInfo);
        this.securityInfoConfiguration =  new SecurityInfoConfiguration(collectSecurityConfig, hideUserDetails);
        this.configDescriptorConfiguration = new ConfigDescriptorConfiguration(collectConfigDescriptor, hideUserDetails);
        this.configFilesConfiguration = new ConfigFilesConfiguration(collectConfigurationFiles);
        this.storageSummaryConfiguration = new StorageSummaryConfiguration(collectStorageSummary);
        this.threadDumpConfiguration = new ThreadDumpConfiguration(collectThreadDump, threadDumpCount, threadDumpInterval);

        if(bundleSize.isPresent()) {
            this.bundleSize = bundleSize.get().intValue();
        }
    }

    /**
     * @param daysCount amount of days eligible for logs collection
     * @param collectSystemInfo
     * @param collectSecurityConfig
     * @param hideUserDetails whether user details should be hidden (default true)
     * @param collectConfigDescriptor
     * @param collectConfigurationFiles
     * @param collectThreadDump
     * @param threadDumpCount amount of dumps to take
     * @param threadDumpInterval interval between dumps (millis)
     * @param collectStorageSummary
     * @param bundleSize
     */
    public BundleConfigurationImpl(Integer daysCount, boolean collectSystemInfo, boolean collectSecurityConfig,
            Optional<Boolean> hideUserDetails, boolean collectConfigDescriptor, boolean collectConfigurationFiles,
            boolean collectThreadDump, int threadDumpCount, long threadDumpInterval, boolean collectStorageSummary,
            Optional<Integer> bundleSize) {

        this.systemLogsConfiguration = new SystemLogsConfiguration(FORCE_COLLECT_SYS_LOGS, daysCount);
        this.systemInfoConfiguration = new SystemInfoConfiguration(collectSystemInfo);
        this.securityInfoConfiguration =  new SecurityInfoConfiguration(collectSecurityConfig, hideUserDetails);
        this.configDescriptorConfiguration = new ConfigDescriptorConfiguration(collectConfigDescriptor, hideUserDetails);
        this.configFilesConfiguration = new ConfigFilesConfiguration(collectConfigurationFiles);
        this.storageSummaryConfiguration = new StorageSummaryConfiguration(collectStorageSummary);
        this.threadDumpConfiguration = new ThreadDumpConfiguration(collectThreadDump, threadDumpCount, threadDumpInterval);

        if(bundleSize.isPresent()) {
            this.bundleSize = bundleSize.get().intValue();
        }
    }

    /**
     * @return {@link org.artifactory.support.config.systemlogs.SystemLogsConfiguration}
     */
    @Override
    public SystemLogsConfiguration getSystemLogsConfiguration() {
        return systemLogsConfiguration;
    }

    /**
     * @return {@link org.artifactory.support.config.system.SystemInfoConfiguration}
     */
    @Override
    public SystemInfoConfiguration getSystemInfoConfiguration() {
        return systemInfoConfiguration;
    }

    /**
     * @return {@link org.artifactory.support.config.security.SecurityInfoConfiguration}
     */
    @Override
    public SecurityInfoConfiguration getSecurityInfoConfiguration() {
        return securityInfoConfiguration;
    }

    /**
     * @return {@link org.artifactory.support.config.descriptor.ConfigDescriptorConfiguration}
     */
    @Override
    public ConfigDescriptorConfiguration getConfigDescriptorConfiguration() {
        return configDescriptorConfiguration;
    }

    /**
     * @return {@link org.artifactory.support.config.configfiles.ConfigFilesConfiguration}
     */
    @Override
    public ConfigFilesConfiguration getConfigFilesConfiguration() {
        return configFilesConfiguration;
    }

    /**
     * @return {@link org.artifactory.support.config.storage.StorageSummaryConfiguration}
     */
    @Override
    public StorageSummaryConfiguration getStorageSummaryConfiguration() {
        return storageSummaryConfiguration;
    }

    /**
     * @return {@link org.artifactory.support.config.analysis.ThreadDumpConfiguration}
     */
    @Override
    public ThreadDumpConfiguration getThreadDumpConfiguration() {
        return threadDumpConfiguration;
    }

    /**
     * @return a default size of compressed archives
     */
    @Override
    public int getBundleSize() {
        return bundleSize;
    }

    @Override
    public boolean isCollectSystemLogs() {
        return systemLogsConfiguration.isEnabled();
    }

    @Override
    public boolean isCollectSystemInfo() {
        return systemInfoConfiguration.isEnabled();
    }

    @Override
    public boolean isCollectSecurityConfig() {
        return securityInfoConfiguration.isEnabled();
    }

    @Override
    public boolean isCollectConfigDescriptor() {
        return configDescriptorConfiguration.isEnabled();
    }

    @Override
    public boolean isCollectConfigurationFiles() {
        return configFilesConfiguration.isEnabled();
    }

    @Override
    public boolean isCollectThreadDump() {
        return threadDumpConfiguration.isEnabled();
    }

    @Override
    public boolean isCollectStorageSummary() {
        return storageSummaryConfiguration.isEnabled();
    }

    @Override
    public String toString() {
        try {
            return JacksonWriter.serialize(this, true);
        } catch (IOException e) {
            return ""; // should not happen
        }
    }
}
