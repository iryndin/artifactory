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
package org.artifactory.repo.index.config;

import org.apache.log4j.Logger;
import org.artifactory.backup.config.Backup;
import org.artifactory.repo.RealRepo;
import org.artifactory.repo.RealRepoBase;
import org.quartz.CronExpression;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlIDREF;
import javax.xml.bind.annotation.XmlType;
import java.io.Serializable;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

@XmlType(name = "IndexerType", propOrder = {"cronExp", "excludedRepositories"})
public class Indexer implements Serializable {
    @SuppressWarnings({"UNUSED_SYMBOL", "UnusedDeclaration"})
    private final static Logger LOGGER = Logger.getLogger(Backup.class);

    private static final long serialVersionUID = 1L;

    private String cronExp;
    private List<RealRepo> excludedRepositories;

    public Indexer() {
        //By Default index every hour
        this.cronExp = "0 0 /1 * * ?";
        this.excludedRepositories = new ArrayList<RealRepo>();
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
                    "Bad indexer cron expression '" + cronExp + "' will be ignored (" +
                            e.getMessage() + ").");
        }
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