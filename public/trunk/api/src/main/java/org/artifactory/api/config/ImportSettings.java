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

package org.artifactory.api.config;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import org.artifactory.api.common.MultiStatusHolder;
import org.artifactory.api.md.MetadataReader;
import org.artifactory.version.ArtifactoryVersion;

import java.io.File;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
@XStreamAlias("import-settings")
public class ImportSettings extends BaseSettings {

    /**
     * Need an internal holder class to make sure that the same flags are used even after copy constructor. The info
     * member is pointing to same instance during the all import process.
     */
    private static class SharedInfo {
        /**
         * The actual artifactory version that created the folder that need to be imported.
         */
        private ArtifactoryVersion exportVersion;

        private MetadataReader metadataReader;

        private boolean indexMarkedArchives;
    }

    private final SharedInfo info;

    public ImportSettings(File baseDir) {
        super(baseDir);
        info = new SharedInfo();
    }

    public ImportSettings(File baseDir, MultiStatusHolder statusHolder) {
        super(baseDir, statusHolder);
        this.info = new SharedInfo();
    }

    public ImportSettings(File baseDir, ImportSettings settings) {
        super(baseDir, settings);
        info = settings.info;
    }

    public ImportSettings(File baseDir, ImportSettings settings, MultiStatusHolder statusHolder) {
        super(baseDir, settings, statusHolder);
        info = settings.info;
    }

    public ArtifactoryVersion getExportVersion() {
        return info.exportVersion;
    }

    public void setExportVersion(ArtifactoryVersion exportVersion) {
        this.info.exportVersion = exportVersion;
    }

    public MetadataReader getMetadataReader() {
        return info.metadataReader;
    }

    public void setMetadataReader(MetadataReader metadataReader) {
        this.info.metadataReader = metadataReader;
    }

    /**
     * This method activates the archive indexer immediately on all artifacts that are marked for indexing. This is
     * usually used when importing a repository (usually a single one) and indexing will take place right after the
     * import process and not in async manner like after importing many repositories.
     *
     * @return Whether immediate indexing for marked artifacts is active or not.
     */
    public boolean isIndexMarkedArchives() {
        return info.indexMarkedArchives;
    }

    public void setIndexMarkedArchives(boolean indexMarkedArchives) {
        info.indexMarkedArchives = indexMarkedArchives;
    }
}
