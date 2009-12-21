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

package org.artifactory.jcr.version;

import org.artifactory.jcr.version.v146.ArchiveIndexesConverter;
import org.artifactory.search.InternalSearchService;
import org.artifactory.version.ArtifactoryVersion;
import org.artifactory.version.SubConfigElementVersion;
import org.artifactory.version.VersionComparator;

/**
 * Not currently used, just for tracking of the jcr verion used
 *
 * @author yoavl
 * @author noamt
 */
public enum JcrVersion implements SubConfigElementVersion {
    v146(ArtifactoryVersion.v130beta5, ArtifactoryVersion.v208, new ArchiveIndexesConverter()),
    v150(ArtifactoryVersion.v210, ArtifactoryVersion.getCurrent(), null);

    private final VersionComparator comparator;
    private ArchiveIndexesConverter indexesConverter;

    JcrVersion(ArtifactoryVersion from, ArtifactoryVersion until, ArchiveIndexesConverter indexesConverter) {
        this.comparator = new VersionComparator(this, from, until);
        this.indexesConverter = indexesConverter;
    }

    public void convertIndexes(InternalSearchService searchService) {
        if (indexesConverter != null) {
            indexesConverter.convert(searchService);
        }
    }

    public boolean isCurrent() {
        return comparator.isCurrent();
    }

    public boolean supports(ArtifactoryVersion version) {
        return comparator.supports(version);
    }

    public VersionComparator getComparator() {
        return comparator;
    }
}