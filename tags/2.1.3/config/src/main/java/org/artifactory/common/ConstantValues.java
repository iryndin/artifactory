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

package org.artifactory.common;

import org.artifactory.common.property.ArtifactorySystemProperties;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;

/**
 * @author freds
 * @date Oct 10, 2008
 */
public enum ConstantValues {
    test("runMode.test", FALSE.toString()), //Use and set only in specifc itests - has serious performance implications
    qa("runMode.qa", FALSE.toString()),
    dev("runMode.dev", FALSE.toString()),
    artifactoryVersion("version", null),
    artifactoryRevision("revision", null),
    disabledAddons("addons.disabled", ""),
    springConfigDir("spring.configDir", null),
    jcrConfigDir("jcr.configDir", null),
    jcrFixConsistency("jcr.fixConsistency", FALSE.toString()),
    jcrAutoRemoveMissingBinaries("jcr.autoRemoveMissingBinaries", TRUE.toString()),
    jcrSessionPoolMaxSize("jcr.session.pool.maxSize", "30"),
    versioningQueryIntervalSecs("versioningQueryIntervalSecs", "43200"),
    logsViewRefreshRateSecs("logs.viewRefreshRateSecs", "10"),
    locksTimeoutSecs("locks.timeoutSecs", "120"),
    locksDebugTimeouts("locks.debugTimeouts", FALSE.toString()),
    taskCompletionLockTimeoutRetries("task.completionLockTimeoutRetries", "100"),
    substituteRepoKeys("repo.key.subst.", null),
    repoCleanupIntervalHours("repo.cleanup.intervalHours", "1"),
    artifactoryRequestsToGlobalCanRetrieveRemoteArtifacts(
            "artifactoryRequestsToGlobalCanRetrieveRemoteArtifacts", FALSE.toString()),
    fsItemCacheIdleTimeSecs("fsitem.cache.idleTimeSecs", "1200"),
    searchMaxResults("search.maxResults", "500"),
    searchMaxFragments("search.content.maxFragments", "500"),
    searchMaxFragmentsSize("search.content.maxFragmentsSize", "5000"),
    gcIntervalSecs("gc.intervalSecs", "14400"),
    gcBatchDeleteMaxSize("gc.batchDeleteMaxSize", "30"),
    gcSleepBetweenNodesMillis("gc.sleepBetweenNodesMillis", "20"),
    forceArchiveIndexing("search.content.forceArchiveIndexing", FALSE.toString()),
    trafficCollectionIntervalSecs("traffic.collectionIntervalSecs", "60"),
    trafficEntriesRetentionSecs("traffic.trafficEntriesRetentionSecs", "86400"),
    securityAuthenticationCacheIdleTimeSecs("security.authentication.cache.idleTimeSecs", "300"),
    userLastAccessUpdatesResolutionSecs("security.userLastAccessUpdatesResolutionSecs", "60"),
    mvnCentralHostPattern("mvn.central.hostPattern", ".maven.org"),
    mvnCentralIndexerMaxQueryIntervalSecs("mvn.central.indexerMaxQueryIntervalSecs", "86400"),
    applicationContextClass("applicationContextClass", null),
    xmlAdditionalMimeTypeExtensions("xmlAdditionalMimeTypeExtensions", null),
    buildMaxFoldersToScanForDeletionWarnings("build.maxFoldersToScanForDeletionWarnings", "2");

    public static final String SYS_PROP_PREFIX = "artifactory.";

    private final String propertyName;
    private final String defValue;

    ConstantValues(String propertyName, String defValue) {
        this.propertyName = SYS_PROP_PREFIX + propertyName;
        this.defValue = defValue;
    }

    public String getPropertyName() {
        return propertyName;
    }

    public String getDefValue() {
        return defValue;
    }

    public String getString() {
        return ArtifactorySystemProperties.get().getProperty(propertyName, defValue);
    }

    public int getInt() {
        return (int) getLong();
    }

    public long getLong() {
        return ArtifactorySystemProperties.get().getLongProperty(propertyName, defValue);
    }

    public boolean getBoolean() {
        return ArtifactorySystemProperties.get().getBooleanProperty(propertyName, defValue);
    }
}