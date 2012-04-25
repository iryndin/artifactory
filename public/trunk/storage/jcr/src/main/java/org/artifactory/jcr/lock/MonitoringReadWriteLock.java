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

package org.artifactory.jcr.lock;

import java.util.Collection;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * A ReentrantReadWriteLock that exposes information about the owner thread
 *
 * @author Yoav Landman
 */
public class MonitoringReadWriteLock extends ReentrantReadWriteLock {

    @Override
    public final Thread getOwner() {
        return super.getOwner();
    }

    @Override
    public final Collection<Thread> getQueuedWriterThreads() {
        return super.getQueuedWriterThreads();
    }

    @Override
    public final Collection<Thread> getQueuedReaderThreads() {
        return super.getQueuedReaderThreads();
    }

    @Override
    public final Collection<Thread> getQueuedThreads() {
        return super.getQueuedThreads();
    }
}