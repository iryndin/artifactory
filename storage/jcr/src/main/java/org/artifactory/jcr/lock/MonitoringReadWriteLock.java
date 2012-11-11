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

import EDU.oswego.cs.dl.util.concurrent.ReentrantWriterPreferenceReadWriteLock;

import java.util.Collection;
import java.util.Collections;

/**
 * A WriterPreferenceReadWriteLock that exposes information about the owner thread
 *
 * @author Yoav Landman
 */
public class MonitoringReadWriteLock extends ReentrantWriterPreferenceReadWriteLock {

    public MonitoringReadWriteLock() {
        super();
    }

    public final Thread getOwner() {
        return super.activeWriter_;
    }

    public final Collection<Thread> getQueuedWriterThreads() {
        return Collections.emptyList();
    }

    public final Collection<Thread> getQueuedReaderThreads() {
        return Collections.emptyList();
    }

    public boolean isWriteLockedByCurrentThread() {
        return Thread.currentThread() == super.activeWriter_;
    }

    public boolean willOrIsWriteLock() {
        return super.activeWriter_ != null || super.waitingWriters_ != 0;
    }
}