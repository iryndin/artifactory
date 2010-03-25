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

package org.artifactory.schedule;

import org.artifactory.api.context.ArtifactoryContextThreadBinder;
import org.artifactory.common.property.ArtifactorySystemProperties;
import org.artifactory.spring.InternalArtifactoryContext;
import org.artifactory.spring.InternalContextHelper;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author Yossi Shaul
 * @author Fred Simon
 */
class ArtifactoryConcurrentExecutor implements Executor {
    private final InternalArtifactoryContext artifactoryContext;
    private final ArtifactorySystemProperties artifactorySystemProperties;
    private final ExecutorService executor;

    ArtifactoryConcurrentExecutor() {
        CustomizableThreadFactory threadFactory = new CustomizableThreadFactory("art-exec-");
        threadFactory.setThreadPriority(Thread.NORM_PRIORITY - 1);
        executor = new ThreadPoolExecutor(
                0, Integer.MAX_VALUE, 60L, TimeUnit.SECONDS, new SynchronousQueue<Runnable>(), threadFactory);
        artifactoryContext = InternalContextHelper.get();
        artifactorySystemProperties = artifactoryContext.getArtifactoryHome().getArtifactoryProperties();
    }

    public void execute(Runnable task) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        task = new RunnableWrapper(task, authentication);
        executor.execute(task);
    }

    <T> Future<T> submit(Runnable task, T result) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        task = new RunnableWrapper(task, authentication);
        return executor.submit(task, result);
    }

    void shutdown() {
        executor.shutdown();
    }

    class RunnableWrapper implements Runnable {
        private final Runnable delegate;
        private final Authentication authentication;


        RunnableWrapper(Runnable delegate, Authentication authentication) {
            this.delegate = delegate;
            this.authentication = authentication;
        }

        public void run() {
            try {
                ArtifactorySystemProperties.bind(artifactorySystemProperties);
                ArtifactoryContextThreadBinder.bind(artifactoryContext);
                SecurityContextHolder.getContext().setAuthentication(authentication);
                delegate.run();
            } finally {
                SecurityContextHolder.clearContext();
                ArtifactoryContextThreadBinder.unbind();
                ArtifactorySystemProperties.unbind();
            }
        }
    }
}
