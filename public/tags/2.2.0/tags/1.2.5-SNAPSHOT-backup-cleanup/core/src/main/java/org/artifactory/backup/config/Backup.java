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
import org.artifactory.backup.BackupHelper;
import org.quartz.CronExpression;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import java.io.Serializable;
import java.text.ParseException;

@XmlType(name = "BackupType", propOrder = {"dir", "cronExp", "retentionPeriodHours"})
public class Backup implements Serializable {
    @SuppressWarnings({"UNUSED_SYMBOL", "UnusedDeclaration"})
    private final static Logger LOGGER = Logger.getLogger(Backup.class);

    private static final long serialVersionUID = 1L;

    public static final int DEFAULT_RETENTION_PERIOD_HOURS = 168;//7 days

    private String dir;
    private String cronExp;
    private int retentionPeriodHours = DEFAULT_RETENTION_PERIOD_HOURS;

    @XmlElement
    public String getDir() {
        if (dir == null) {
            dir = BackupHelper.DEFAULT_BACKUP_DIR;
        }
        return dir;
    }

    public void setDir(String dir) {
        this.dir = dir;
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
}
