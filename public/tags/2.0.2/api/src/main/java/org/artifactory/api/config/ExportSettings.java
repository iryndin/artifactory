package org.artifactory.api.config;

import com.thoughtworks.xstream.annotations.XStreamAlias;

import java.io.File;
import java.util.Date;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
@XStreamAlias("export-settings")
public class ExportSettings extends BaseSettings {

    private boolean ignoreRepositoryFilteringRulesOn = false;
    private boolean createArchive = false;
    private Date time;
    /**
     * Flag that indicates if to export m2 compatible meta data
     */
    private boolean m2Compatible = false;

    private boolean incremental;

    public ExportSettings(File baseDir) {
        super(baseDir);
        time = new Date();
    }

    public ExportSettings(File baseDir, ExportSettings settings) {
        super(baseDir, settings);
        this.ignoreRepositoryFilteringRulesOn = settings.ignoreRepositoryFilteringRulesOn;
        this.createArchive = settings.createArchive;
        this.time = settings.time;
        this.m2Compatible = settings.m2Compatible;
        this.incremental = settings.incremental;
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

    /**
     * @return True is the export is incremental. Meaning override target only if exported file or folder is newer.
     */
    public boolean isIncremental() {
        return incremental;
    }

    /**
     * Incremental export only writes files and folder that are newer than what's in the target.
     *
     * @param incremental   True to use incremental export.
     */
    public void setIncremental(boolean incremental) {
        this.incremental = incremental;
    }

    public boolean isM2Compatible() {
        return m2Compatible;
    }

    public void setM2Compatible(boolean m2Compatible) {
        this.m2Compatible = m2Compatible;
    }
}