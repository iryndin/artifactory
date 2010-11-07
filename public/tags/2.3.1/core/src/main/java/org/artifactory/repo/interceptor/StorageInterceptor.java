/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2010 JFrog Ltd.
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

import org.artifactory.common.MutableStatusHolder;
import org.artifactory.interceptor.Interceptor;
import org.artifactory.jcr.fs.JcrFsItem;
import org.artifactory.repo.RepoPath;

/**
 * @author yoav
 */
public interface StorageInterceptor extends Interceptor {

    void beforeCreate(JcrFsItem fsItem, MutableStatusHolder statusHolder);

    void afterCreate(JcrFsItem fsItem, MutableStatusHolder statusHolder);

    void beforeDelete(JcrFsItem fsItem, MutableStatusHolder statusHolder);

    void afterDelete(JcrFsItem fsItem, MutableStatusHolder statusHolder);

    void beforeMove(JcrFsItem sourceItem, RepoPath targetRepoPath, MutableStatusHolder statusHolder);

    void afterMove(JcrFsItem sourceItem, JcrFsItem targetItem, MutableStatusHolder statusHolder);

    void beforeCopy(JcrFsItem sourceItem, RepoPath targetRepoPath, MutableStatusHolder statusHolder);

    void afterCopy(JcrFsItem sourceItem, JcrFsItem targetItem, MutableStatusHolder statusHolder);
}