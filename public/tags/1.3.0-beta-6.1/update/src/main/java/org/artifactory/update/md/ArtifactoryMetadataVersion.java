/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.artifactory.update.md;

import org.artifactory.api.common.StatusHolder;
import org.artifactory.api.config.ImportSettings;
import org.artifactory.api.fs.FileInfo;
import org.artifactory.api.fs.FolderInfo;
import org.artifactory.api.md.MetadataEntry;
import org.artifactory.api.md.MetadataReader;
import org.artifactory.update.md.current.MetadataReaderImpl;
import org.artifactory.update.md.v125rc0.MetadataReader125;
import org.artifactory.update.md.v130beta3.ArtifactoryFileConverter;
import org.artifactory.update.md.v130beta3.ArtifactoryFolderConverter;
import org.artifactory.update.md.v130beta3.MetadataReader130;
import org.artifactory.version.ArtifactoryVersion;
import org.artifactory.version.VersionComparator;

import java.io.File;
import java.util.List;

/**
 * @author freds
 * @date Nov 11, 2008
 */
public enum ArtifactoryMetadataVersion implements MetadataReader {
    v125rc0(ArtifactoryVersion.v125rc0, ArtifactoryVersion.v130beta2, new MetadataReader125()),
    v130beta3(ArtifactoryVersion.v130beta3, ArtifactoryVersion.v130beta5, new MetadataReader130()),
    current(ArtifactoryVersion.v130beta6, ArtifactoryVersion.getCurrent(), new MetadataReaderImpl());

    private static final String FILE_MD_NAME_V130_BETA_3 = ArtifactoryFileConverter.OLD_METADATA_NAME + ".xml";
    private static final String FOLDER_MD_NAME_V130_BETA_3 = ArtifactoryFolderConverter.OLD_METADATA_NAME + ".xml";
    private static final String FILE_MD_NAME_V130_BETA_6 = FileInfo.ROOT + ".xml";
    private static final String FOLDER_MD_NAME_V130_BETA_6 = FolderInfo.ROOT + ".xml";

    private final VersionComparator comparator;
    private final MetadataReader delegate;

    ArtifactoryMetadataVersion(ArtifactoryVersion from, ArtifactoryVersion until, MetadataReader delegate) {
        this.comparator = new VersionComparator(from, until);
        this.delegate = delegate;
    }

    public boolean isCurrent() {
        return comparator.isCurrent();
    }

    public boolean supports(ArtifactoryVersion version) {
        return comparator.supports(version);
    }

    public List<MetadataEntry> getMetadataEntries(File file, ImportSettings settings, StatusHolder status) {
        return delegate.getMetadataEntries(file, settings, status);
    }

    /**
     * Find the version from the format of the metadata folder
     *
     * @param metadataFolder
     */
    public static ArtifactoryMetadataVersion findVersion(File metadataFolder) {
        if (!metadataFolder.exists()) {
            throw new IllegalArgumentException(
                    "Cannot find metadata version of non existent file " + metadataFolder.getAbsolutePath());
        }
        if (metadataFolder.isDirectory()) {
            String[] mdFiles = metadataFolder.list();
            for (String fileName : mdFiles) {
                if (fileName.equalsIgnoreCase(FILE_MD_NAME_V130_BETA_3) ||
                        fileName.equalsIgnoreCase(FOLDER_MD_NAME_V130_BETA_3)) {
                    return v130beta3;
                }
                if (fileName.equalsIgnoreCase(FILE_MD_NAME_V130_BETA_6) ||
                        fileName.equalsIgnoreCase(FOLDER_MD_NAME_V130_BETA_6)) {
                    return current;
                }
            }
            throw new IllegalStateException("Metadata folder " + metadataFolder.getAbsolutePath() +
                    " does not contain any recognizable metadata files!");
        } else {
            // For v125rc0 to v130beta2, the folder is actually a file
            return v125rc0;
        }
    }

    public static ArtifactoryMetadataVersion findVersion(ArtifactoryVersion version) {
        ArtifactoryMetadataVersion[] metadataVersions = values();
        for (int i = metadataVersions.length - 1; i >= 0; i--) {
            ArtifactoryMetadataVersion metadataVersion = metadataVersions[i];
            if (metadataVersion.supports(version)) {
                return metadataVersion;
            }
        }
        throw new IllegalStateException("Metadata import from Artifactory version " + version +
                " is not supported!");
    }
}
