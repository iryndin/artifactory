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
import org.artifactory.exception.CancelException;
import org.artifactory.interceptor.Interceptors;
import org.artifactory.jcr.JcrService;
import org.artifactory.jcr.fs.JcrFsItem;
import org.artifactory.repo.RepoPath;
import org.artifactory.spring.Reloadable;
import org.springframework.stereotype.Service;

/**
 * @author yoav
 */
@Service
@Reloadable(beanClass = StorageInterceptors.class, initAfter = JcrService.class)
public class StorageInterceptorsImpl extends Interceptors<StorageInterceptor> implements StorageInterceptors {

    public void beforeCreate(JcrFsItem fsItem, MutableStatusHolder statusHolder) {
        try {
            for (StorageInterceptor storageInterceptor : this) {
                storageInterceptor.beforeCreate(fsItem, statusHolder);
            }
        } catch (CancelException e) {
            statusHolder.setError("Create rejected: " + e.getMessage(), e.getErrorCode(), e);
        }
    }

    public void afterCreate(JcrFsItem fsItem, MutableStatusHolder statusHolder) {
        for (StorageInterceptor storageInterceptor : this) {
            storageInterceptor.afterCreate(fsItem, statusHolder);
        }
    }

    public void beforeDelete(JcrFsItem fsItem, MutableStatusHolder statusHolder) {
        try {
            for (StorageInterceptor storageInterceptor : this) {
                storageInterceptor.beforeDelete(fsItem, statusHolder);
            }
        } catch (CancelException e) {
            statusHolder.setError("Delete rejected: " + e.getMessage(), e.getErrorCode(), e);
        }
    }

    public void afterDelete(JcrFsItem fsItem, MutableStatusHolder statusHolder) {
        for (StorageInterceptor storageInterceptor : this) {
            storageInterceptor.afterDelete(fsItem, statusHolder);
        }
    }

    public void beforeMove(JcrFsItem sourceItem, RepoPath targetRepoPath, MutableStatusHolder statusHolder) {
        try {
            for (StorageInterceptor storageInterceptor : this) {
                storageInterceptor.beforeMove(sourceItem, targetRepoPath, statusHolder);
            }
        } catch (CancelException e) {
            statusHolder.setError("Move rejected: " + e.getMessage(), e.getErrorCode(), e);
        }
    }

    public void afterMove(JcrFsItem sourceItem, JcrFsItem targetItem, MutableStatusHolder statusHolder) {
        for (StorageInterceptor storageInterceptor : this) {
            storageInterceptor.afterMove(sourceItem, targetItem, statusHolder);
        }
    }

    public void beforeCopy(JcrFsItem sourceItem, RepoPath targetRepoPath, MutableStatusHolder statusHolder) {
        try {
            for (StorageInterceptor storageInterceptor : this) {
                storageInterceptor.beforeCopy(sourceItem, targetRepoPath, statusHolder);
            }
        } catch (CancelException e) {
            statusHolder.setError("Copy rejected: " + e.getMessage(), e.getErrorCode(), e);
        }
    }

    public void afterCopy(JcrFsItem sourceItem, JcrFsItem targetItem, MutableStatusHolder statusHolder) {
        for (StorageInterceptor storageInterceptor : this) {
            storageInterceptor.afterCopy(sourceItem, targetItem, statusHolder);
        }
    }
}