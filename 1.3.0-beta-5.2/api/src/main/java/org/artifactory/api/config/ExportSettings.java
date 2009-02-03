package org.artifactory.api.config;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import org.apache.log4j.Logger;
import org.artifactory.descriptor.repo.LocalRepoDescriptor;

import java.io.File;
import java.io.Serializable;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
@XStreamAlias("export-settings")
public class ExportSettings implements Serializable {
    @SuppressWarnings({"UNUSED_SYMBOL", "UnusedDeclaration"})
    private final static Logger LOGGER = Logger.getLogger(ExportSettings.class);

    private final File baseDir;
    private boolean includeMetadata = true;
    private boolean ignoreRepositoryFilteringRulesOn = false;
    private boolean createArchive = false;
    private Date time;
    List<LocalRepoDescriptor> reposToExport = Collections.emptyList();//When empty - export all

    public ExportSettings(File baseDir) {
        this.baseDir = baseDir;
    }

    public ExportSettings(File baseDir, ExportSettings settings) {
        this(baseDir);
        this.includeMetadata = settings.includeMetadata;
        this.ignoreRepositoryFilteringRulesOn = settings.ignoreRepositoryFilteringRulesOn;
        this.createArchive = settings.createArchive;
        this.time = settings.time;
        this.reposToExport = settings.reposToExport;
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

    public boolean isIgnoreRepositoryFilteringRulesOn() {
        return ignoreRepositoryFilteringRulesOn;
    }

    public void setIgnoreRepositoryFilteringRulesOn(boolean ignoreRepositoryFilteringRulesOn) {
        this.ignoreRepositoryFilteringRulesOn = ignoreRepositoryFilteringRulesOn;
    }

    public boolean isCreateArchive() {
        return createArchive;
    }

    public void setCreateArchive(boolean createArchive) {
        this.createArchive = createArchive;
    }

    public Date getTime() {
        return time;
    }

    public void setTime(Date time) {
        this.time = time;
    }

    public List<LocalRepoDescriptor> getReposToExport() {
        return reposToExport;
    }

    public void setReposToExport(List<LocalRepoDescriptor> reposToExport) {
        this.reposToExport = reposToExport;
    }
}
