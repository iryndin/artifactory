/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2014 JFrog Ltd.
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

package org.artifactory.schedule;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.artifactory.api.context.ArtifactoryContextThreadBinder;
import org.artifactory.common.ArtifactoryHome;
import org.artifactory.storage.spring.ArtifactoryStorageContext;
import org.artifactory.storage.spring.StorageContextHelper;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Fixed size execution service with bounded authentication
 *
 * @author Shay Yaakov
 */
public class ArtifactorySingleThreadExecutor extends AbstractExecutorService {

    private final ArtifactoryStorageContext storageContext;
    private final ExecutorService executor;

    public ArtifactorySingleThreadExecutor() {
        ThreadFactory threadFactory = new ThreadFactoryBuilder().setNameFormat("art-fixed-%s").build();
        executor = new ThreadPoolExecutor(1, 1,
                0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(),
                threadFactory);
        storageContext = StorageContextHelper.get();
    }

    @Override
    public void shutdown() {
        executor.shutdown();
    }

    @Override
    public List<Runnable> shutdownNow() {
        return executor.shutdownNow();
    }

    @Override
    public boolean isShutdown() {
        return executor.isShutdown();
    }

    @Override
    public boolean isTerminated() {
        return executor.isTerminated();
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return executor.isShutdown();
    }

    @Override
    public void execute(Runnable task) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        task = new RunnableWrapper(task, authentication);
        executor.execute(task);
    }

    class RunnableWrapper implements Runnable {
        private final Runnable delegate;
        private final Authentication authentication;

        RunnableWrapper(Runnable delegate, Authentication authentication) {
            this.delegate = delegate;
            this.authentication = authentication;
        }

        @Override
        public void run() {
            try {
                ArtifactoryContextThreadBinder.bind(storageContext);
                ArtifactoryHome.bind(storageContext.getArtifactoryHome());
                SecurityContextHolder.getContext().setAuthentication(authentication);
                delegate.run();
            } finally {
                // in case an async operation is fired while shutdown (i.e gc) the context holder strategy is
                // cleared and NPE can happen after the async finished (or is finishing). see RTFACT-2812
                if (storageContext.isReady()) {
                    SecurityContextHolder.clearContext();
                }
                ArtifactoryContextThreadBinder.unbind();
                ArtifactoryHome.unbind();
            }
        }
    }
}
