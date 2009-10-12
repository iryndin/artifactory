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

package org.artifactory.repo.interceptor;

import org.artifactory.api.common.StatusHolder;
import org.artifactory.jcr.fs.JcrFsItem;
import org.artifactory.search.InternalSearchService;
import org.artifactory.spring.InternalArtifactoryContext;
import org.artifactory.spring.InternalContextHelper;

/**
 * Interceptor which handles archive indexing calculation upon creation
 *
 * @author Noam Tenne
 */
public class ArchiveIndexingInterceptor implements RepoInterceptor {

    /**
     * If the newly created item is a file, this method will mark it up for content indexing.
     *
     * @param fsItem       Newly created item
     * @param statusHolder StatusHolder
     */
    public void onCreate(JcrFsItem fsItem, StatusHolder statusHolder) {
        if (fsItem.isFile()) {
            InternalArtifactoryContext context = InternalContextHelper.get();
            InternalSearchService searchService = context.beanForType(InternalSearchService.class);
            searchService.markArchiveForIndexing(fsItem.getRepoPath(), true);
        }
    }

    /**
     * No implementation needed
     *
     * @param fsItem       Deleted item
     * @param statusHolder StatusHolder
     */
    public void onDelete(JcrFsItem fsItem, StatusHolder statusHolder) {
    }
}
