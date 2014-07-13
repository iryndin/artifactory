/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2012 JFrog Ltd.
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

package org.artifactory.maven.index;

import org.artifactory.api.repo.index.MavenIndexerService;
import org.artifactory.descriptor.repo.RepoDescriptor;
import org.artifactory.repo.RealRepo;
import org.artifactory.sapi.common.Lock;
import org.artifactory.spring.ReloadableBean;

import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * @author yoavl
 */
public interface InternalMavenIndexerService extends MavenIndexerService, ReloadableBean {

    @Lock
    void saveIndexFiles(MavenIndexManager mavenIndexManager);

    @Lock
    void fetchOrCreateIndex(MavenIndexManager mavenIndexManager, Date fireTime, boolean forceRemoteDownload);

    @Lock
    void mergeVirtualRepoIndexes(Set<? extends RepoDescriptor> excludedRepositories, List<RealRepo> repos);

    void index(MavenIndexerRunSettings indexerSettings);
}
