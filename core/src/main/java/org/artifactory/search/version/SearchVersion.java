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

package org.artifactory.search.version;

import com.google.common.collect.Lists;
import org.artifactory.search.InternalSearchService;
import org.artifactory.search.version.v146.ArchiveIndexesConverter;
import org.artifactory.version.ArtifactoryVersion;
import org.artifactory.version.SubConfigElementVersion;
import org.artifactory.version.VersionComparator;
import org.artifactory.version.converter.ConfigurationConverter;

import java.util.List;

/**
 * Search version track
 *
 * @author Noam Y. Tenne
 */
public enum SearchVersion implements SubConfigElementVersion {

    v1(ArtifactoryVersion.v130beta5, ArtifactoryVersion.v208, new ArchiveIndexesConverter()),
    v2(ArtifactoryVersion.v210, ArtifactoryVersion.getCurrent(), null);

    private final VersionComparator comparator;
    private ConfigurationConverter<InternalSearchService> configurationConverter;

    /**
     * Main constructor
     *
     * @param from                   Start version
     * @param until                  End version
     * @param configurationConverter Configuration converter required for the specified range
     */
    SearchVersion(ArtifactoryVersion from, ArtifactoryVersion until,
            ConfigurationConverter<InternalSearchService> configurationConverter) {
        this.comparator = new VersionComparator(this, from, until);
        this.configurationConverter = configurationConverter;
    }

    /**
     * calls the converters relevant for the specified version range
     *
     * @param internalSearchService Instance of internal search service
     */
    public void convert(InternalSearchService internalSearchService) {
        // First create the list of converters to apply
        List<ConfigurationConverter<InternalSearchService>> converters = Lists.newArrayList();

        // All converters of versions above me needs to be executed in sequence
        SearchVersion[] versions = SearchVersion.values();
        for (SearchVersion version : versions) {
            if (version.ordinal() >= ordinal() && version.configurationConverter != null) {
                converters.add(version.configurationConverter);
            }
        }

        for (ConfigurationConverter<InternalSearchService> converter : converters) {
            converter.convert(internalSearchService);
        }
    }

    public VersionComparator getComparator() {
        return comparator;
    }
}