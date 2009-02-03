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
package org.artifactory.descriptor.backup;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.artifactory.common.ArtifactoryHome;
import org.artifactory.descriptor.repo.RealRepoDescriptor;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlIDREF;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@XmlType(name = "BackupType",
        propOrder = {"dir", "cronExp", "inPlace", "retentionPeriodHours", "createArchive",
                "excludedRepositories"})
public class BackupDescriptor implements Serializable {
    @SuppressWarnings({"UNUSED_SYMBOL", "UnusedDeclaration"})
    private final static Logger LOGGER = Logger.getLogger(BackupDescriptor.class);

    private static final long serialVersionUID = 1L;

    public static final int DEFAULT_RETENTION_PERIOD_HOURS = 168;//7 days

    private String dir;
    private String cronExp;

    @XmlElement(defaultValue = "false")
    private boolean inPlace;

    @XmlElement(defaultValue = DEFAULT_RETENTION_PERIOD_HOURS + "")
    private int retentionPeriodHours = DEFAULT_RETENTION_PERIOD_HOURS;

    @XmlElement(defaultValue = "false")
    private boolean createArchive;

    @XmlIDREF
    @XmlElementWrapper(name = "excludedRepositories")
    @XmlElement(name = "repositoryRef", type = RealRepoDescriptor.class, required = false)
    private List<RealRepoDescriptor> excludedRepositories = new ArrayList<RealRepoDescriptor>();

    @XmlTransient
    private File backupDir;


    public String getDir() {
        return dir;
    }

    public void setDir(String dir) {
        this.dir = dir;
    }

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
                try {
                    FileUtils.forceMkdir(backupDir);
                } catch (IOException e) {
                    throw new IllegalArgumentException(
                            "Backup directory provided in configuration: '" +
                                    backupDir.getAbsolutePath() +
                                    "' cannot be created or is not a directory.");
                }
            }
        }
        return backupDir;
    }

    public String getCronExp() {
        return cronExp;
    }

    public void setCronExp(String cronExp) {
        this.cronExp = cronExp;
    }

    public boolean isInPlace() {
        return inPlace;
    }

    public void setInPlace(boolean inPlace) {
        this.inPlace = inPlace;
    }

    public int getRetentionPeriodHours() {
        return retentionPeriodHours;
    }

    public void setRetentionPeriodHours(int retentionPeriodHours) {
        this.retentionPeriodHours = retentionPeriodHours;
    }


    public boolean isCreateArchive() {
        return createArchive;
    }

    public void setCreateArchive(boolean createArchive) {
        this.createArchive = createArchive;
    }

    public List<RealRepoDescriptor> getExcludedRepositories() {
        return excludedRepositories;
    }

    public void setExcludedRepositories(List<RealRepoDescriptor> excludedRepositories) {
        this.excludedRepositories = excludedRepositories;
    }

}
