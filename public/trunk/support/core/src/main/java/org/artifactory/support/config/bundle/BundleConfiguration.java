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

import org.artifactory.support.config.configfiles.ConfigFilesConfiguration;
import org.artifactory.support.config.descriptor.ConfigDescriptorConfiguration;
import org.artifactory.support.config.analysis.ThreadDumpConfiguration;
import org.artifactory.support.config.security.SecurityInfoConfiguration;
import org.artifactory.support.config.storage.StorageSummaryConfiguration;
import org.artifactory.support.config.system.SystemInfoConfiguration;
import org.artifactory.support.config.systemlogs.SystemLogsConfiguration;

/**
 * Generic bundle configuration
 *
 * @author Michael Pasternak
 */
public interface BundleConfiguration {

    /**
     * @return {@link org.artifactory.support.config.systemlogs.SystemLogsConfiguration}
     */
    SystemLogsConfiguration getSystemLogsConfiguration();

    /**
     * @return {@link org.artifactory.support.config.system.SystemInfoConfiguration}
     */
    SystemInfoConfiguration getSystemInfoConfiguration();

    /**
     * @return {@link org.artifactory.support.config.security.SecurityInfoConfiguration}
     */
    SecurityInfoConfiguration getSecurityInfoConfiguration();

    /**
     * @return {@link org.artifactory.support.config.descriptor.ConfigDescriptorConfiguration}
     */
    ConfigDescriptorConfiguration getConfigDescriptorConfiguration();

    /**
     * @return {@link org.artifactory.support.config.configfiles.ConfigFilesConfiguration}
     */
    ConfigFilesConfiguration getConfigFilesConfiguration();

    /**
     * @return {@link org.artifactory.support.config.analysis.ThreadDumpConfiguration}
     */
    ThreadDumpConfiguration getThreadDumpConfiguration();

    /**
     * @return {@link org.artifactory.support.config.storage.StorageSummaryConfiguration}
     */
    StorageSummaryConfiguration getStorageSummaryConfiguration();

    /**
     * @return a default size of compressed archives
     */
    int getBundleSize();

    /**
     * Whether system logs should be collected
     *
     * @return boolean
     */
    boolean isCollectSystemLogs();

    /**
     * Whether SystemInfo should be collected
     *
     * @return boolean
     */
    boolean isCollectSystemInfo();

    /**
     * Whether SecurityConfig should be collected
     *
     * @return boolean
     */
    boolean isCollectSecurityConfig();

    /**
     * Whether ConfigDescriptor should be collected
     *
     * @return boolean
     */
    boolean isCollectConfigDescriptor();

    /**
     * Whether ConfigurationFiles should be collected
     *
     * @return boolean
     */
    boolean isCollectConfigurationFiles();

    /**
     * Whether ThreadDump should be collected
     *
     * @return boolean
     */
    boolean isCollectThreadDump();

    /**
     * Whether StorageSummary should be collected
     *
     * @return boolean
     */
    boolean isCollectStorageSummary();
}
