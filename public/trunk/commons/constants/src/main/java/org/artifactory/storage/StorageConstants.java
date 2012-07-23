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

package org.artifactory.storage;

/**
 * @author Yoav Landman
 */
public interface StorageConstants {
    String NT_UNSTRUCTURED = "nt:unstructured";

    String ARTIFACTORY_PREFIX = "artifactory:";

    //generic fs items
    String MIX_ARTIFACTORY_BASE = ARTIFACTORY_PREFIX + "base";
    String NT_ARTIFACTORY_FILE = ARTIFACTORY_PREFIX + "file";
    String NT_ARTIFACTORY_FOLDER = ARTIFACTORY_PREFIX + "folder";
    String NT_ARTIFACTORY_METADATA = ARTIFACTORY_PREFIX + "metadata";
    String NT_ARTIFACTORY_LOG_ENTRY = ARTIFACTORY_PREFIX + "logEntry";

    //basic properties used in artifactory
    String PROP_ARTIFACTORY_NAME = ARTIFACTORY_PREFIX + "name";
    String PROP_ARTIFACTORY_CREATED = ARTIFACTORY_PREFIX + "created";
    String PROP_ARTIFACTORY_CREATED_BY = ARTIFACTORY_PREFIX + "createdBy";
    String PROP_ARTIFACTORY_LAST_MODIFIED = ARTIFACTORY_PREFIX + "lastModified";
    String PROP_ARTIFACTORY_LAST_MODIFIED_BY = ARTIFACTORY_PREFIX + "lastModifiedBy";
    String PROP_ARTIFACTORY_LAST_UPDATED = ARTIFACTORY_PREFIX + "lastUpdated";

    //file item props
    String PROP_ARTIFACTORY_MD5_ACTUAL = ARTIFACTORY_PREFIX + "checksum.md5.actual";
    String PROP_ARTIFACTORY_MD5_ORIGINAL = ARTIFACTORY_PREFIX + "checksum.md5.original";
    String PROP_ARTIFACTORY_SHA1_ACTUAL = ARTIFACTORY_PREFIX + "checksum.sha1.actual";
    String PROP_ARTIFACTORY_SHA1_ORIGINAL = ARTIFACTORY_PREFIX + "checksum.sha1.original";

    //xml and metadata
    String NODE_ARTIFACTORY_XML = ARTIFACTORY_PREFIX + "xml";
    String NODE_ARTIFACTORY_METADATA = ARTIFACTORY_PREFIX + "metadata";
    String NODE_ARTIFACTORY_PROPERTIES = ARTIFACTORY_PREFIX + "properties";
    String PROP_ARTIFACTORY_METADATA_NAME = ARTIFACTORY_PREFIX + "metadataName";

    //indexed archives
    String PROP_ARTIFACTORY_ARCHIVE_ENTRY = ARTIFACTORY_PREFIX + "archiveEntry";
    String PROP_ARTIFACTORY_ARCHIVE_INDEXED = ARTIFACTORY_PREFIX + "archiveIndexed";

    //traffic
    String PROP_ARTIFACTORY_TRAFFIC_ENTRY = ARTIFACTORY_PREFIX + "trafficEntry";
    String PROP_ARTIFACTORY_TIMESTAMP = ARTIFACTORY_PREFIX + "timestamp";
    String PROP_ARTIFACTORY_LAST_COLLECTED = ARTIFACTORY_PREFIX + "lastCollected";

    //stats
    String MIX_ARTIFACTORY_STATS = ARTIFACTORY_PREFIX + "stats";
    String PROP_ARTIFACTORY_STATS_DOWNLOAD_COUNT = ARTIFACTORY_PREFIX + "downloadCount";
    String PROP_ARTIFACTORY_STATS_LAST_DOWNLOADED = ARTIFACTORY_PREFIX + "lastDownloaded";
    String PROP_ARTIFACTORY_STATS_LAST_DOWNLOADED_BY = ARTIFACTORY_PREFIX + "lastDownloadedBy";

    //build
    String PROP_BUILD_ARTIFACT_CHECKSUMS = ARTIFACTORY_PREFIX + "build.artifact.checksums";
    String PROP_BUILD_DEPENDENCY_CHECKSUMS = ARTIFACTORY_PREFIX + "build.dependency.checksums";
    String PROP_BUILD_RELEASE_LAST_STATUS = ARTIFACTORY_PREFIX + "build.release.last.status";

    //maven
    String PROP_ARTIFACTORY_RECALC_MAVEN_METADATA = ARTIFACTORY_PREFIX + "recalcMavenMetadata";
    String PROP_ARTIFACTORY_MAVEN_PLUGIN = ARTIFACTORY_PREFIX + "maven.mavenPlugin";

    //keystore
    String PROP_ARTIFACTORY_KEYSTORE_PASSWORD = ARTIFACTORY_PREFIX + "keyStorePassword";

    //build
    String PROP_BUILD_LATEST_NUMBER = ARTIFACTORY_PREFIX + "build.latest.number";
    String PROP_BUILD_LATEST_START_TIME = ARTIFACTORY_PREFIX + "build.latest.startTime";

    //licenses
    String PROP_LICENSE_STATE = ARTIFACTORY_PREFIX + "license.state";

    //Nuget
    String PROP_NUGET_NUSPEC = ARTIFACTORY_PREFIX + "nuget.nuspec";

    //JCR namespaces
    String ARTIFACTORY_NAMESPACE_PREFIX = "artifactory";
    String ARTIFACTORY_NAMESPACE = "http://artifactory.jfrog.org/1.0";
    String OCM_NAMESPACE_PREFIX = "ocm";
    String OCM_NAMESPACE = "http://jackrabbit.apache.org/ocm";
}
