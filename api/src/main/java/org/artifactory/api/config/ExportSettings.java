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

    /**
     * Callback - If we need to perform any special actions
     */
    private ExportCallback callback;

    public ExportSettings(File baseDir) {
        super(baseDir);
        time = new Date();
    }

    public ExportSettings(File baseDir, MultiStatusHolder statusHolder) {
        super(baseDir, statusHolder);
        time = new Date();
    }

    public ExportSettings(File baseDir, ExportSettings settings) {
        super(baseDir, settings);
        this.ignoreRepositoryFilteringRulesOn = settings.ignoreRepositoryFilteringRulesOn;
        this.createArchive = settings.createArchive;
        this.time = settings.time;
        this.m2Compatible = settings.m2Compatible;
        this.incremental = settings.incremental;
        this.callback = settings.callback;
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
     * @param incremental True to use incremental export.
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

    /**
     * Returns the callback object
     *
     * @return Export callback object
     */
    public ExportCallback getCallback() {
        return callback;
    }

    /**
     * Sets the export callback object
     *
     * @param callback Callback object to set
     */
    public void setCallback(ExportCallback callback) {
        this.callback = callback;
    }

    /**
     * Indicates if the callback object is set
     *
     * @return True if the object is set
     */
    public boolean hasCallback() {
        return callback != null;
    }
}