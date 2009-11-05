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

package org.artifactory.schedule;


import org.artifactory.api.context.ArtifactoryContextThreadBinder;
import org.artifactory.common.property.ArtifactorySystemProperties;
import org.artifactory.spring.InternalArtifactoryContext;
import org.artifactory.spring.InternalContextHelper;
import org.springframework.scheduling.concurrent.ConcurrentTaskExecutor;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public class CachedThreadPoolTaskExecutor extends ConcurrentTaskExecutor {
    private InternalArtifactoryContext artifactoryContext;
    private ArtifactorySystemProperties artifactorySystemProperties;

    public CachedThreadPoolTaskExecutor() {
        super(Executors.newCachedThreadPool());
        artifactoryContext = InternalContextHelper.get();
        artifactorySystemProperties = artifactoryContext.getArtifactoryHome().getArtifactoryProperties();
    }

    @Override
    public ExecutorService getConcurrentExecutor() {
        return (ExecutorService) super.getConcurrentExecutor();
    }

    @Override
    public void execute(Runnable task) {
        task = new RunnableWrapper(task);
        super.execute(task);
    }

    class RunnableWrapper implements Runnable {
        private final Runnable delegate;

        RunnableWrapper(Runnable delegate) {
            this.delegate = delegate;
        }

        public void run() {
            try {
                ArtifactoryContextThreadBinder.bind(artifactoryContext);
                ArtifactorySystemProperties.bind(artifactorySystemProperties);
                delegate.run();
            } finally {
                ArtifactoryContextThreadBinder.unbind();
                ArtifactorySystemProperties.unbind();
            }
        }
    }
}
