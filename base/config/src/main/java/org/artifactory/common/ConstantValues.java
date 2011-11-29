/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2011 JFrog Ltd.
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

package org.artifactory.common;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;

/**
 * @author freds
 * @date Oct 10, 2008
 */
@SuppressWarnings({"EnumeratedConstantNamingConvention"})
public enum ConstantValues {
    test("runMode.test", FALSE), //Use and set only in specific itests - has serious performance implications
    qa("runMode.qa", FALSE),
    dev("runMode.dev", FALSE),
    artifactoryVersion("version"),
    artifactoryRevision("revision"),
    artifactoryTimestamp("timestamp"),
    disabledAddons("addons.disabled", ""),
    addonsInfoUrl("addons.info.url", "http://service.jfrog.org/artifactory/addons/info/%s"),
    springConfigDir("spring.configDir"),
    jcrConfigDir("jcr.configDir"),
    jcrFixConsistency("jcr.fixConsistency", FALSE),
    jcrAutoRemoveMissingBinaries("jcr.autoRemoveMissingBinaries", FALSE),
    jcrCorePoolSize("jcr.corePoolSize", 6),
    versioningQueryIntervalSecs("versioningQueryIntervalSecs", Seconds.MINUTE * 12),
    logsViewRefreshRateSecs("logs.viewRefreshRateSecs", 10),
    locksTimeoutSecs("locks.timeoutSecs", 120),
    locksDebugTimeouts("locks.debugTimeouts", FALSE),
    taskCompletionLockTimeoutRetries("task.completionLockTimeoutRetries", 100),
    substituteRepoKeys("repo.key.subst."),
    repoCleanupIntervalHours("repo.cleanup.intervalHours", 21),
    repoConcurrentDownloadSyncTimeoutSecs("repo.concurrentDownloadSyncTimeoutSecs", Seconds.MINUTE * 15),
    fsItemCacheIdleTimeSecs("fsitem.cache.idleTimeSecs", Seconds.MINUTE * 20),
    searchMaxResults("search.maxResults", 500),
    searchUserQueryLimit("search.userQueryLimit", 1000),
    searchMaxFragments("search.content.maxFragments", 500),
    searchMaxFragmentsSize("search.content.maxFragmentsSize", 5000),
    searchArchiveMinQueryLength("search.archive.minQueryLength", 3),
    searchForceArchiveIndexing("search.content.forceArchiveIndexing", FALSE),
    searchPatternTimeoutSecs("search.pattern.timeoutSecs", 30),
    searchXmlIndexing("search.xmlIndexing", FALSE),
    searchArtifactSearchUseV2Storage("search.artifactSearch.useV2Storage", TRUE),
    gcUseIndex("gc.useIndex", FALSE),
    gcIntervalSecs("gc.intervalSecs", Seconds.DAY),
    gcDelaySecs("gc.delaySecs", Seconds.HOUR * 2),
    gcSleepBetweenNodesMillis("gc.sleepBetweenNodesMillis", 20),
    gcScanStartSleepingThresholdMillis("gc.scanStartSleepingThresholdMillis", 20000),
    gcScanSleepBetweenIterationsMillis("gc.scanSleepBetweenIterationsMillis", 200),
    gcFileScanSleepIterationMillis("gc.fileScanSleepIterationMillis", 1000),
    gcFileScanSleepMillis("gc.fileScanSleepMillis", 250),
    gcUseV1("gc.useV1", false),
    gcMaxCacheEntries("gc.maxCacheEntries", 10000),
    trafficCollectionActive("traffic.collectionActive", FALSE),
    trafficCollectionIntervalSecs("traffic.collectionIntervalSecs", Seconds.MINUTE),
    trafficEntriesRetentionSecs("traffic.trafficEntriesRetentionSecs", Seconds.HOUR * 2),
    securityAuthenticationCacheIdleTimeSecs("security.authentication.cache.idleTimeSecs", Seconds.MINUTE * 5),
    userLastAccessUpdatesResolutionSecs("security.userLastAccessUpdatesResolutionSecs", Seconds.MINUTE),
    securityAuthenticationEncryptedPasswordSurroundChars(
            "security.authentication.encryptedPassword.surroundChars", "{}"),
    mvnCentralHostPattern("mvn.central.hostPattern", ".maven.org"),
    mvnCentralIndexerMaxQueryIntervalSecs("mvn.central.indexerMaxQueryIntervalSecs", Seconds.DAY),
    mvnMetadataVersionsComparator("mvn.metadataVersionsComparatorFqn"),
    mvnDynamicMetadataCacheRetentionSecs("mvn.dynamicMetadata.cacheRetentionSecs", 10),
    mvnMetadataVersion3Enabled("mvn.metadata.version3.enabled", TRUE),
    buildMaxFoldersToScanForDeletionWarnings("build.maxFoldersToScanForDeletionWarnings", 2),
    missingBuildChecksumCacheIdeTimeSecs("build.checksum.cache.idleTimeSecs", Seconds.MINUTE * 5),
    artifactoryUpdatesRefreshIntervalSecs("updates.refreshIntervalSecs", Seconds.HOUR * 4),
    artifactoryUpdatesUrl("updates.url", "http://service.jfrog.org/artifactory/updates"),
    artifactoryRequestsToGlobalCanRetrieveRemoteArtifacts(
            "artifactoryRequestsToGlobalCanRetrieveRemoteArtifacts", FALSE),
    uiSyntaxColoringMaxTextSizeBytes("ui.syntaxColoringMaxTextSizeBytes", 512000),
    pluginScriptsRefreshIntervalSecs("plugin.scripts.refreshIntervalSecs", Integer.MAX_VALUE),
    uiChroot("ui.chroot"),
    artifactoryLicenseDir("licenseDir"),
    fileRollerMaxFilesToRetain("file.roller.maxFileToRetain", 10),
    backupFileExportSleepIterationMillis("backup.fileExportSleepIterationMillis", 2000),
    backupFileExportSleepMillis("backup.fileExportSleepMillis", 250),
    httpAcceptEncodingGzip("http.acceptEncoding.gzip", true),
    httpUseExpectContinue("http.useExpectContinue", false),
    filteringResourceSizeKb("filtering.resourceSizeKb", 64),
    searchForExistingResourceOnRemoteRequest("repo.remote.checkForExistingResourceOnRequest", TRUE),
    versionQueryEnabled("version.query.enabled", true),
    hostId("host.id"),
    yumCalculationRequestAggregationTimeWindowSecs("yum.calculationRequest.aggregationTimeWindowSecs", 60),
    yumCalculationRequestAggregationCycleSecs("yum.calculationRequest.aggregationCycleSecs", 60),
    globalExcludes("repo.includeExclude.globalExcludes"),
    archiveLicenseFileNames("archive.licenseFile.names", "license,LICENSE,license.txt,LICENSE.txt,LICENSE.TXT"),
    uiSearchMaxRowsPerPage("ui.search.maxRowsPerPage", 20);

    public static final String SYS_PROP_PREFIX = "artifactory.";

    private final String propertyName;
    private final String defValue;

    ConstantValues(String propertyName) {
        this(propertyName, null);
    }

    ConstantValues(String propertyName, Object defValue) {
        this.propertyName = SYS_PROP_PREFIX + propertyName;
        this.defValue = defValue == null ? null : defValue.toString();
    }

    public String getPropertyName() {
        return propertyName;
    }

    public String getDefValue() {
        return defValue;
    }

    public String getString() {
        return ArtifactoryHome.get().getArtifactoryProperties().getProperty(propertyName, defValue);
    }

    public int getInt() {
        return (int) getLong();
    }

    public long getLong() {
        return ArtifactoryHome.get().getArtifactoryProperties().getLongProperty(propertyName, defValue);
    }

    public boolean getBoolean() {
        return ArtifactoryHome.get().getArtifactoryProperties().getBooleanProperty(propertyName, defValue);
    }

    public boolean isSet() {
        return ArtifactoryHome.get().getArtifactoryProperties().getProperty(propertyName, null) != null;
    }

    private static class Seconds {
        private static final int MINUTE = 60;
        private static final int HOUR = MINUTE * 60;
        private static final int DAY = HOUR * 24;
    }
}
