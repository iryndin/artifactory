package org.artifactory.api.config;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import org.apache.log4j.Logger;
import org.artifactory.descriptor.repo.LocalRepoDescriptor;

import java.io.File;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
@XStreamAlias("import-settings")
public class ImportSettings implements Serializable {
    @SuppressWarnings({"UNUSED_SYMBOL", "UnusedDeclaration"})
    private final static Logger LOGGER = Logger.getLogger(ImportSettings.class);

    private final File baseDir;
    private boolean includeMetadata = true;
    private boolean useSymLinks = false;
    private boolean copyToWorkingFolder = true;
    List<LocalRepoDescriptor> reposToImport = Collections.emptyList();//When empty - import all

    public ImportSettings(File baseDir) {
        this.baseDir = baseDir;
    }

    public ImportSettings(File baseDir, ImportSettings settings) {
        this(baseDir);
        this.includeMetadata = settings.includeMetadata;
        this.useSymLinks = settings.useSymLinks;
        this.copyToWorkingFolder = settings.copyToWorkingFolder;
        this.reposToImport = settings.reposToImport;
    }

    public File getBaseDir() {
        return baseDir;
    }

    public boolean isIncludeMetadata() {
        return includeMetadata;
    }

    public void setIncludeMetadata(boolean includeMetadata) {
        this.includeMetadata = includeMetadata;
    }

    public boolean isUseSymLinks() {
        return useSymLinks;
    }

    public void setUseSymLinks(boolean useSymLinks) {
        this.useSymLinks = useSymLinks;
    }

    public boolean isCopyToWorkingFolder() {
        return copyToWorkingFolder;
    }

    public void setCopyToWorkingFolder(boolean copyToWorkingFolder) {
        this.copyToWorkingFolder = copyToWorkingFolder;
    }

    public List<LocalRepoDescriptor> getReposToImport() {
        return reposToImport;
    }

    public void setReposToImport(List<LocalRepoDescriptor> reposToImport) {
        this.reposToImport = reposToImport;
    }
}
