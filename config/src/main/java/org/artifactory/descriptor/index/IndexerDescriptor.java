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
package org.artifactory.descriptor.index;

import org.apache.log4j.Logger;
import org.artifactory.descriptor.Descriptor;
import org.artifactory.descriptor.backup.BackupDescriptor;
import org.artifactory.descriptor.repo.RealRepoDescriptor;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlIDREF;
import javax.xml.bind.annotation.XmlType;
import java.util.ArrayList;
import java.util.List;

@XmlType(name = "IndexerType", propOrder = {"cronExp", "excludedRepositories"})
public class IndexerDescriptor implements Descriptor {
    @SuppressWarnings({"UNUSED_SYMBOL", "UnusedDeclaration"})
    private final static Logger LOGGER = Logger.getLogger(BackupDescriptor.class);

    private static final long serialVersionUID = 1L;

    private String cronExp;

    @XmlIDREF
    @XmlElementWrapper(name = "excludedRepositories")
    @XmlElement(name = "repositoryRef", type = RealRepoDescriptor.class, required = false)
    private List<RealRepoDescriptor> excludedRepositories;

    public IndexerDescriptor() {
        //By Default index every hour
        this.cronExp = "0 0 /1 * * ?";
        this.excludedRepositories = new ArrayList<RealRepoDescriptor>();
    }

    public String getCronExp() {
        return cronExp;
    }

    public void setCronExp(String cronExp) {
        this.cronExp = cronExp;
    }

    public List<RealRepoDescriptor> getExcludedRepositories() {
        return excludedRepositories;
    }

    public void setExcludedRepositories(List<RealRepoDescriptor> excludedRepositories) {
        this.excludedRepositories = excludedRepositories;
    }

}