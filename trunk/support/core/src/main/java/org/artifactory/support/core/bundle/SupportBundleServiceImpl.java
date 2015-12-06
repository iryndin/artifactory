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

package org.artifactory.support.core.bundle;

import org.artifactory.support.config.analysis.ThreadDumpConfiguration;
import org.artifactory.support.config.configfiles.ConfigFilesConfiguration;
import org.artifactory.support.config.descriptor.ConfigDescriptorConfiguration;
import org.artifactory.support.config.security.SecurityInfoConfiguration;
import org.artifactory.support.config.storage.StorageSummaryConfiguration;
import org.artifactory.support.config.system.SystemInfoConfiguration;
import org.artifactory.support.config.systemlogs.SystemLogsConfiguration;
import org.artifactory.support.core.collectors.analysis.ThreadDumpCollector;
import org.artifactory.support.core.collectors.config.ConfigDescriptorCollector;
import org.artifactory.support.core.collectors.configfiles.ConfigFilesCollector;
import org.artifactory.support.core.collectors.logs.LogsCollector;
import org.artifactory.support.core.collectors.security.SecurityInfoCollector;
import org.artifactory.support.core.collectors.storage.StorageSummaryCollector;
import org.artifactory.support.core.collectors.system.SystemInfoCollector;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.io.File;


/**
 * @author Michael Pasternak
 */
@Scope(BeanDefinition.SCOPE_SINGLETON)
@Service
public class SupportBundleServiceImpl extends AbstractSupportBundleService {

    @Autowired
    LogsCollector logsCollector;
    @Autowired
    SystemInfoCollector systemInfoCollector;
    @Autowired
    SecurityInfoCollector securityInfoCollector;
    @Autowired
    ConfigDescriptorCollector configDescriptorCollector;
    @Autowired
    ConfigFilesCollector configFilesCollector;
    @Autowired
    StorageSummaryCollector storageSummaryCollector;
    @Autowired
    ThreadDumpCollector threadDumpCollector;

    /**
     * Collects SystemLogs
     *
     * @param tmpDir output directory for produced content
     * @param configuration the runtime configuration
     *
     * @return operation result
     */
    @Override
    protected boolean doCollectSystemLogs(File tmpDir, SystemLogsConfiguration configuration) {
        return logsCollector.collect(configuration, tmpDir);
    }

    /**
     * Collects SystemInfo
     *
     * @param tmpDir output directory for produced content
     * @param configuration the runtime configuration
     *
     * @return operation result
     */
    @Override
    protected boolean doCollectSystemInfo(File tmpDir, SystemInfoConfiguration configuration) {
        return systemInfoCollector.collect(configuration, tmpDir);
    }

    /**
     * Collects SecurityConfig
     *
     * @param tmpDir output directory for produced content
     * @param configuration the runtime configuration
     *
     * @return operation result
     */
    @Override
    protected boolean doCollectSecurityConfig(File tmpDir, SecurityInfoConfiguration configuration) {
        return securityInfoCollector.collect(configuration, tmpDir);
    }

    /**
     * Collects ConfigDescriptor
     *
     * @param tmpDir output directory for produced content
     * @param configuration the runtime configuration
     *
     * @return operation result
     */
    @Override
    protected boolean doCollectConfigDescriptor(File tmpDir, ConfigDescriptorConfiguration configuration) {
        return configDescriptorCollector.collect(configuration, tmpDir);
    }

    /**
     * Collects ConfigurationFiles
     *
     * @param tmpDir output directory for produced content
     * @param configuration the runtime configuration
     *
     * @return operation result
     */
    @Override
    protected boolean doCollectConfigurationFiles(File tmpDir, ConfigFilesConfiguration configuration) {
        return configFilesCollector.collect(configuration, tmpDir);
    }

    /**
     * Collects ThreadDump
     *
     * @param tmpDir output directory for produced content
     * @param configuration the runtime configuration
     *
     * @return operation result
     */
    @Override
    protected boolean doCollectThreadDump(File tmpDir, ThreadDumpConfiguration configuration) {
        return threadDumpCollector.collect(configuration, tmpDir);
    }

    /**
     * Collects StorageSummary
     *
     * @param tmpDir output directory for produced content
     * @param configuration the runtime configuration
     *
     * @return operation result
     */
    @Override
    protected boolean doCollectStorageSummary(File tmpDir, StorageSummaryConfiguration configuration) {
        return storageSummaryCollector.collect(configuration, tmpDir);
    }
}
