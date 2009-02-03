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
package org.artifactory.backup.config;

import org.apache.log4j.Logger;
import org.artifactory.ArtifactoryHome;
import org.artifactory.repo.RealRepo;
import org.artifactory.repo.RealRepoBase;
import org.quartz.CronExpression;

import javax.xml.bind.annotation.*;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

@XmlType(name = "BackupType",
        propOrder = {"dir", "cronExp", "retentionPeriodHours", "createArchive",
                "excludedRepositories"})
public class Backup implements Serializable {
    @SuppressWarnings({"UNUSED_SYMBOL", "UnusedDeclaration"})
    private final static Logger LOGGER = Logger.getLogger(Backup.class);

    private static final long serialVersionUID = 1L;

    public static final int DEFAULT_RETENTION_PERIOD_HOURS = 168;//7 days

    private String dir;
    private String cronExp;
    private int retentionPeriodHours = DEFAULT_RETENTION_PERIOD_HOURS;
    private boolean createArchive;
    private List<RealRepo> excludedRepositories = new ArrayList<RealRepo>();

    private transient File backupDir;

    @XmlElement
    public String getDir() {
        return dir;
    }

    public void setDir(String dir) {
        this.dir = dir;
    }

    @XmlTransient
    public File getBackupDir() {
        if (backupDir == null) {
            if (dir == null) {
                try {
                    backupDir = ArtifactoryHome.getOrCreateSubDir("backup");
                } catch (IOException e) {
                    throw new IllegalArgumentException(
                            "Failed to create backup directory: " + backupDir.getAbsolutePath(), e);
                }
            } else {
                backupDir = new File(dir);
                if (!backupDir.exists() || !backupDir.isDirectory() || !backupDir.canWrite())
                    throw new IllegalArgumentException(
                            "Backup directory provided in configuration: '" + backupDir.getAbsolutePath() +
                                    "' does not exists, is not a directory or is read only.");
            }
        }
        return backupDir;
    }

    @XmlElement
    public String getCronExp() {
        return cronExp;
    }

    public void setCronExp(String cronExp) {
        try {
            new CronExpression(cronExp);
            this.cronExp = cronExp;
        } catch (ParseException e) {
            LOGGER.error(
                    "Bad backup cron expression '" + cronExp + "' will be ignored (" +
                            e.getMessage() + ").");
        }
    }

    @XmlElement(defaultValue = DEFAULT_RETENTION_PERIOD_HOURS + "")
    public int getRetentionPeriodHours() {
        return retentionPeriodHours;
    }

    public void setRetentionPeriodHours(int retentionPeriodHours) {
        this.retentionPeriodHours = retentionPeriodHours;
    }

    @XmlElement(defaultValue = "false")
    public boolean isCreateArchive() {
        return createArchive;
    }

    public void setCreateArchive(boolean createArchive) {
        this.createArchive = createArchive;
    }

    @XmlIDREF
    @XmlElementWrapper(name = "excludedRepositories")
    @XmlElement(name = "repositoryRef", type = RealRepoBase.class, required = false)
    public List<RealRepo> getExcludedRepositories() {
        return excludedRepositories;
    }

    public void setExcludedRepositories(List<RealRepo> excludedRepositories) {
        this.excludedRepositories = excludedRepositories;
    }

}
