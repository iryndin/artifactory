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

package org.artifactory.engine;

import org.artifactory.api.repo.Async;
import org.artifactory.api.request.DownloadService;
import org.artifactory.spring.ReloadableBean;

import java.util.concurrent.CountDownLatch;

/**
 * @author freds
 * @date Oct 28, 2008
 */
public interface InternalDownloadService extends DownloadService, ReloadableBean {

    /**
     * Will cause the provided latch to unlock notifying download waiters on a remote repo. We do this after transaction
     * so that new download thread will not override the yet-to-be-committed vfs file.
     *
     * @param latch The latch to release
     */
    @Async(delayUntilAfterCommit = true)
    void releaseDownloadWaiters(CountDownLatch latch);
}
