/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2011 JFrog Ltd.
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

import com.google.common.collect.Sets;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import org.artifactory.api.common.MultiStatusHolder;
import org.artifactory.repo.RepoPath;
import org.artifactory.sapi.common.ExportSettings;
import org.artifactory.sapi.common.FileExportCallback;

import java.io.File;
import java.util.Date;
import java.util.Set;

/**
 * @author Yoav Landman
 */
@XStreamAlias("export-settings")
public class ExportSettingsImpl extends ImportExportSettingsImpl implements ExportSettings {

    private boolean ignoreRepositoryFilteringRulesOn = false;
    private boolean createArchive = false;
    private Date time;

    /**
     * Flag that indicates if to export m2 compatible meta data
     */
    private boolean m2Compatible = false;

    private boolean incremental;

    /**
     * Callbacks - If we need to perform any special actions before exporting a file
     */
    private Set<FileExportCallback> callbacks;

    public ExportSettingsImpl(File baseDir) {
        super(baseDir);
        time = new Date();
        callbacks = Sets.newHashSet();
    }

    public ExportSettingsImpl(File baseDir, MultiStatusHolder statusHolder) {
        super(baseDir, statusHolder);
        time = new Date();
        callbacks = Sets.newHashSet();
    }

    public ExportSettingsImpl(File baseDir, ExportSettings exportSettings) {
        super(baseDir, exportSettings);
        ExportSettingsImpl settings = (ExportSettingsImpl) exportSettings;
        this.ignoreRepositoryFilteringRulesOn = settings.ignoreRepositoryFilteringRulesOn;
        this.createArchive = settings.createArchive;
        this.time = settings.time;
        this.m2Compatible = settings.m2Compatible;
        this.incremental = settings.incremental;
        this.callbacks = settings.callbacks;
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

    public void addCallback(FileExportCallback callback) {
        if (callbacks == null) {
            callbacks = Sets.newHashSet();
        }
        callbacks.add(callback);
    }

    public void executeCallbacks(RepoPath currentRepoPath) {
        if ((callbacks != null) && !callbacks.isEmpty()) {
            for (FileExportCallback callback : callbacks) {
                callback.callback(this, currentRepoPath);
            }

        }
    }
}