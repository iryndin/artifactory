/*
 * This file is part of Artifactory.
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

package org.artifactory.descriptor.repo;

import org.artifactory.descriptor.Descriptor;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlIDREF;
import javax.xml.bind.annotation.XmlType;
import java.util.ArrayList;
import java.util.List;

@XmlType(name = "VirtualRepoType",
        propOrder = {"artifactoryRequestsCanRetrieveRemoteArtifacts", "repositories", "keyPair"},
        namespace = Descriptor.NS)
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

    @XmlElement(required = true)
    private String keyPair;

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
        this.artifactoryRequestsCanRetrieveRemoteArtifacts = artifactoryRequestsCanRetrieveRemoteArtifacts;
    }

    public String getKeyPair() {
        return keyPair;
    }

    public void setKeyPair(String keyPair) {
        this.keyPair = keyPair;
    }

    public boolean isReal() {
        return false;
    }

    public boolean removeRepository(RepoDescriptor repo) {
        return repositories.remove(repo);
    }

    public void removeKeyPair() {
        keyPair = null;
    }
}