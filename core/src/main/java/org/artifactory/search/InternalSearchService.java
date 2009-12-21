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

package org.artifactory.search;

import org.artifactory.api.repo.Async;
import org.artifactory.api.repo.Lock;
import org.artifactory.api.repo.RepoPath;
import org.artifactory.api.search.SearchService;
import org.artifactory.jcr.fs.JcrFile;
import org.artifactory.spring.ReloadableBean;

import javax.jcr.RepositoryException;
import javax.jcr.query.QueryResult;
import java.util.List;

/**
 * @author Noam Tenne
 */
public interface InternalSearchService extends SearchService, ReloadableBean {
    void markArchivesForIndexing(boolean force);

    void markArchivesForIndexing(RepoPath searchPath, boolean force);

    @Async(delayUntilAfterCommit = true)
    void index(List<RepoPath> archiveRepoPaths);

    @Lock(transactional = true)
    void index(RepoPath archiveRepoPath);

    @Async(transactional = true, delayUntilAfterCommit = true)
    void asyncIndex(RepoPath archiveRepoPath);

    boolean markArchiveForIndexing(JcrFile newJcrFile, boolean force);

    /**
     * Searches for POM's within a certain path
     *
     * @param repoPath To path to search the POM in
     * @return QueryResult - Search results
     * @throws javax.jcr.RepositoryException
     */
    @Lock(transactional = true)
    QueryResult searchPomInPath(RepoPath repoPath) throws RepositoryException;
}