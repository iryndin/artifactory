package org.artifactory.api.config;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import org.artifactory.api.md.MetadataReader;
import org.artifactory.version.ArtifactoryVersion;

import java.io.File;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
@XStreamAlias("import-settings")
public class ImportSettings extends BaseSettings {

    private static class SettingsInfo {
        private boolean useSymLinks = false;
        /**
         * if set to false the import is syncronous
         */
        private boolean copyToWorkingFolder = true;

        /**
         * The actual artifactory version that created the folder that need to be imported.
         */
        private ArtifactoryVersion exportVersion;

        private MetadataReader metadataReader;
    }

    private final SettingsInfo info;

    public ImportSettings(File baseDir) {
        super(baseDir);
        info = new SettingsInfo();
    }

    public ImportSettings(File baseDir, ImportSettings settings) {
        super(baseDir, settings);
        info = settings.info;
    }

    public boolean isUseSymLinks() {
        return info.useSymLinks;
    }

    public void setUseSymLinks(boolean useSymLinks) {
        this.info.useSymLinks = useSymLinks;
    }

    public boolean isCopyToWorkingFolder() {
        return info.copyToWorkingFolder;
    }

    public void setCopyToWorkingFolder(boolean copyToWorkingFolder) {
        this.info.copyToWorkingFolder = copyToWorkingFolder;
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
}
