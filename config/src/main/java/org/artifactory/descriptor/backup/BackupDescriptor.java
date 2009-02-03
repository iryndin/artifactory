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
import org.artifactory.common.ArtifactoryHome;
import org.artifactory.descriptor.Descriptor;
import org.artifactory.descriptor.repo.RealRepoDescriptor;

import javax.xml.bind.annotation.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@XmlType(name = "BackupType",
        propOrder = {"key", "enabled",
                "dir", "cronExp", "retentionPeriodHours", "createArchive", "excludedRepositories"})
public class BackupDescriptor implements Descriptor {

    private static final long serialVersionUID = 1L;

    public static final int DEFAULT_RETENTION_PERIOD_HOURS = 168;//7 days

    @XmlID
    @XmlElement(required = true)
    private String key;

    @XmlElement(defaultValue = "true")
    private boolean enabled = true;

    @XmlElement(required = true)
    private String cronExp;

    private File dir;

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

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public File getDir() {
        return dir;
    }

    public void setDir(File dir) {
        this.dir = dir;
    }

    public File getBackupDir() {
        if (backupDir == null) {
            if (dir == null) {
                backupDir = new File(ArtifactoryHome.getBackupDir(), key);
            } else {
                backupDir = dir;
                try {
                    FileUtils.forceMkdir(backupDir);
                } catch (IOException e) {
                    throw new IllegalArgumentException("Backup directory provided in configuration: '" +
                            backupDir.getAbsolutePath() + "' cannot be created or is not a directory.");
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

    public boolean removeExcludedRepository(RealRepoDescriptor realRepo) {
        return excludedRepositories.remove(realRepo);
    }

    public boolean isIncremental() {
        return retentionPeriodHours <= 0;
    }
}