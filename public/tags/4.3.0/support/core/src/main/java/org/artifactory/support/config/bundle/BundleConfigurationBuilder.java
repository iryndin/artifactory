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

import org.joda.time.DateTime;

import java.util.Date;
import java.util.Optional;

/**
 * Constructs {@link BundleConfiguration}
 *
 * @author Michael Pasternak
 */
public class BundleConfigurationBuilder {
    private Date startDate=null; // TODO: consider using ZonedDateTime (if needed)
    private Date endDate=null;   // TODO: consider using ZonedDateTime (if needed)
    private Integer daysCount=null;

    private boolean collectSystemInfo;
    private boolean collectSecurityConfig;
    private boolean collectConfigDescriptor;
    private boolean collectConfigurationFiles;
    private boolean collectThreadDump;
    private int threadDumpCount;
    private long threadDumpInterval;
    private boolean collectStorageSummary;
    private Optional<Boolean> hideUserDetails = Optional.empty();

    private int bundleSize = BundleConfigurationImpl.DEFAULT_BUNDLE_SIZE;

    /**
     * Default configuration
     */
    public BundleConfigurationBuilder() {
    }

    /**
     * @param startDate collect logs from
     * @param endDate collect logs to
     */
    public BundleConfigurationBuilder(Date startDate, Date endDate) {
        this.startDate = startDate;
        this.endDate = endDate;
    }

    /**
     * @param daysCount amount of days eligible for logs collection
     */
    public BundleConfigurationBuilder(int daysCount) {
        this.daysCount = Integer.valueOf(daysCount);
    }

    /**
     * Whether SystemInfo should be collected
     */
    public BundleConfigurationBuilder collectSystemInfo() {
        this.collectSystemInfo = true;
        return this;
    }

    /**
     * Whether SecurityConfig should be collected
     */
    public BundleConfigurationBuilder collectSecurityConfig() {
        this.collectSecurityConfig = true;
        return this;
    }

    /**
     * Whether SecurityConfig should be collected
     *
     * @param hideUserDetails whether user details should be hidden (default true)
     */
    public BundleConfigurationBuilder collectSecurityConfig(boolean hideUserDetails) {
        this.collectSecurityConfig = true;
        this.hideUserDetails = Optional.of(hideUserDetails);
        return this;
    }

    /**
     * Whether ConfigDescriptor should be collected
     */
    public BundleConfigurationBuilder collectConfigDescriptor() {
        this.collectConfigDescriptor = true;
        return this;
    }

    /**
     *
     * Whether ConfigurationFiles should be collected
     */
    public BundleConfigurationBuilder collectConfigurationFiles() {
        this.collectConfigurationFiles = true;
        return this;
    }

    /**
     * Whether ThreadDump should be collected
     */
    public BundleConfigurationBuilder collectThreadDump() {
        this.collectThreadDump = true;
        return this;
    }

    /**
     * Whether ThreadDump should be collected
     *
     * @param count amount of dumps to take
     * @param interval interval between dumps (millis)
     */
    public BundleConfigurationBuilder collectThreadDump(int count, long interval) {
        this.collectThreadDump = true;
        this.threadDumpCount = count;
        this.threadDumpInterval = interval;
        return this;
    }

    /**
     * Whether StorageSummary should be collected
     */
    public BundleConfigurationBuilder collectStorageSummary() {
        this.collectStorageSummary = true;
        return this;
    }

    /**
     * A bundle size in Mb
     *
     * @param bundleSize
     */
    public BundleConfigurationBuilder bundleSize(int bundleSize) {
        this.bundleSize = bundleSize;
        return this;
    }

    /**
     * Constructs BundleConfiguration
     *
     * @return {@link BundleConfigurationImpl}
     */
    public BundleConfigurationImpl build() {
        if (daysCount == null && startDate == null && endDate == null) {
            endDate = new Date();
            startDate = new DateTime(endDate).minusDays(1).toDate();
            return new BundleConfigurationImpl(startDate, endDate, collectSystemInfo,
                    collectSecurityConfig, hideUserDetails, collectConfigDescriptor,
                    collectConfigurationFiles, collectThreadDump, threadDumpCount, threadDumpInterval,
                    collectStorageSummary, Optional.<Integer>of(this.bundleSize)
            );
        } else if (daysCount == null) {
            return new BundleConfigurationImpl(startDate, endDate, collectSystemInfo,
                    collectSecurityConfig, hideUserDetails, collectConfigDescriptor,
                    collectConfigurationFiles, collectThreadDump, threadDumpCount, threadDumpInterval,
                    collectStorageSummary, Optional.<Integer>of(this.bundleSize)
            );
        } else {
            return new BundleConfigurationImpl(daysCount, collectSystemInfo, collectSecurityConfig,
                    hideUserDetails,  collectConfigDescriptor, collectConfigurationFiles,
                    collectThreadDump, threadDumpCount, threadDumpInterval, collectStorageSummary,
                    Optional.<Integer>of(this.bundleSize)
            );
        }
    }
}
