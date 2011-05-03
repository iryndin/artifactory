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

package org.artifactory.descriptor.index;

import org.artifactory.descriptor.Descriptor;
import org.artifactory.descriptor.repo.RepoBaseDescriptor;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlIDREF;
import javax.xml.bind.annotation.XmlType;
import java.util.SortedSet;

@XmlType(name = "IndexerType", propOrder = {"enabled", "indexingIntervalHours", "excludedRepositories"},
        namespace = Descriptor.NS)
public class IndexerDescriptor implements Descriptor {

    private static final long serialVersionUID = 1L;

    private boolean enabled;

    private int indexingIntervalHours;

    @XmlIDREF
    @XmlElementWrapper(name = "excludedRepositories")
    @XmlElement(name = "repositoryRef", type = RepoBaseDescriptor.class, required = false)
    private SortedSet<? extends RepoBaseDescriptor> excludedRepositories;

    public IndexerDescriptor() {
        //By Default index once a day
        this.indexingIntervalHours = 24;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getIndexingIntervalHours() {
        return indexingIntervalHours;
    }

    public void setIndexingIntervalHours(int indexingIntervalHours) {
        this.indexingIntervalHours = indexingIntervalHours;
    }

    public SortedSet<? extends RepoBaseDescriptor> getExcludedRepositories() {
        return excludedRepositories;
    }

    public void setExcludedRepositories(SortedSet<? extends RepoBaseDescriptor> excludedRepositories) {
        this.excludedRepositories = excludedRepositories;
    }

    @SuppressWarnings({"SuspiciousMethodCalls"})
    public boolean removeExcludedRepository(RepoBaseDescriptor repoBaseDescriptor) {
        return excludedRepositories.remove(repoBaseDescriptor);
    }
}