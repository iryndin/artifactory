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

    public ExportSettings(File baseDir) {
        super(baseDir);
    }

    public ExportSettings(File baseDir, ExportSettings settings) {
        super(baseDir, settings);
        this.ignoreRepositoryFilteringRulesOn = settings.ignoreRepositoryFilteringRulesOn;
        this.createArchive = settings.createArchive;
        this.time = settings.time;
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

}
