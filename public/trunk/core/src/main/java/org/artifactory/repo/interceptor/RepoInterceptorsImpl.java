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
import org.artifactory.interceptor.Interceptors;
import org.artifactory.jcr.JcrService;
import org.artifactory.jcr.fs.JcrFsItem;
import org.artifactory.spring.Reloadable;
import org.springframework.stereotype.Service;

/**
 * @author yoav
 */
@Service
@Reloadable(beanClass = RepoInterceptors.class, initAfter = JcrService.class)
public class RepoInterceptorsImpl extends Interceptors<RepoInterceptor> implements RepoInterceptors {

    public void onCreate(JcrFsItem fsItem, StatusHolder statusHolder) {
        for (RepoInterceptor repoInterceptor : this) {
            repoInterceptor.onCreate(fsItem, statusHolder);
        }
    }

    public void onDelete(JcrFsItem fsItem, StatusHolder statusHolder) {
        for (RepoInterceptor repoInterceptor : this) {
            repoInterceptor.onDelete(fsItem, statusHolder);
        }
    }

    public void onMove(JcrFsItem sourceItem, JcrFsItem targetItem, StatusHolder statusHolder) {
        for (RepoInterceptor repoInterceptor : this) {
            repoInterceptor.onMove(sourceItem, targetItem, statusHolder);
        }
    }

    public void onCopy(JcrFsItem sourceItem, JcrFsItem targetItem, StatusHolder statusHolder) {
        for (RepoInterceptor repoInterceptor : this) {
            repoInterceptor.onCopy(sourceItem, targetItem, statusHolder);
        }
    }
}