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

package org.artifactory.repo;

import java.util.List;

/**
 * @author Yoav Landman
 */
public interface HttpRepositoryConfiguration extends RepositoryConfiguration {
    String TYPE = "remote";

    int getMaxUniqueSnapshots();

    boolean isSuppressPomConsistencyChecks();

    boolean isHandleReleases();

    boolean isHandleSnapshots();

    String getUrl();

    boolean isBlackedOut();

    long getFailedRetrievalCachePeriodSecs();

    boolean isFetchJarsEagerly();

    boolean isFetchSourcesEagerly();

    boolean isHardFail();

    String getLocalAddress();

    long getMissedRetrievalCachePeriodSecs();

    boolean isOffline();

    String getPassword();

    List<String> getPropertySets();

    String getProxy();

    String getRemoteRepoChecksumPolicyType();

    long getRetrievalCachePeriodSecs();

    boolean isShareConfiguration();

    int getSocketTimeoutMillis();

    boolean isStoreArtifactsLocally();

    boolean isSynchronizeProperties();

    boolean isUnusedArtifactsCleanupEnabled();

    int getUnusedArtifactsCleanupPeriodHours();

    String getUsername();
}
