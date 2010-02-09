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
package org.artifactory.descriptor.repo;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlIDREF;
import javax.xml.bind.annotation.XmlType;
import java.util.ArrayList;
import java.util.List;

@XmlType(name = "VirtualRepoType",
        propOrder = {"artifactoryRequestsCanRetrieveRemoteArtifacts", "repositories"})
public class VirtualRepoDescriptor extends RepoBaseDescriptor {

    public static final String GLOBAL_VIRTUAL_REPO_KEY = "repo";

    /*@XmlElement(name = "repositories")
    @XmlJavaTypeAdapter(RepositoriesListAdapter.class)*/
    @XmlIDREF
    @XmlElementWrapper(name = "repositories")
    @XmlElement(name = "repositoryRef", type = RepoBaseDescriptor.class, required = false)
    private List<RepoDescriptor> repositories = new ArrayList<RepoDescriptor>();

    @XmlElement(defaultValue = "false", required = false)
    private boolean artifactoryRequestsCanRetrieveRemoteArtifacts;

    public List<RepoDescriptor> getRepositories() {
        return repositories;
    }

    public void setRepositories(List<RepoDescriptor> repositories) {
        this.repositories = repositories;
    }

    public boolean isArtifactoryRequestsCanRetrieveRemoteArtifacts() {
        return artifactoryRequestsCanRetrieveRemoteArtifacts;
    }

    public void setArtifactoryRequestsCanRetrieveRemoteArtifacts(
            boolean artifactoryRequestsCanRetrieveRemoteArtifacts) {
        this.artifactoryRequestsCanRetrieveRemoteArtifacts =
                artifactoryRequestsCanRetrieveRemoteArtifacts;
    }

    public boolean isReal() {
        return false;
    }
}