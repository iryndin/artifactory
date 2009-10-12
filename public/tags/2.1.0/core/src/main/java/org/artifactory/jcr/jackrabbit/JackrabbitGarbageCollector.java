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

package org.artifactory.jcr.jackrabbit;

import org.apache.jackrabbit.core.data.DataStore;
import org.apache.jackrabbit.core.data.GarbageCollector;
import org.apache.jackrabbit.core.data.ScanEventListener;
import org.apache.jackrabbit.core.state.ItemStateException;
import org.artifactory.jcr.gc.GarbageCollectorInfo;
import org.artifactory.jcr.gc.JcrGarbageCollector;

import javax.jcr.RepositoryException;
import java.io.IOException;

/**
 * @author freds
 * @date Jun 23, 2009
 */
public class JackrabbitGarbageCollector implements JcrGarbageCollector {
    private final GarbageCollector gc;

    public JackrabbitGarbageCollector(GarbageCollector gc) {
        this.gc = gc;
    }

    public void setSleepBetweenNodes(int millis) {
        gc.setSleepBetweenNodes(millis);
    }

    public boolean scan() throws RepositoryException, IllegalStateException, IOException, ItemStateException {
        gc.scan();
        // Always true with the Jackrabbit GC
        return true;
    }

    public void stopScan() throws RepositoryException {
        gc.stopScan();
    }

    public int deleteUnused() throws RepositoryException {
        return gc.deleteUnused();
    }

    public DataStore getDataStore() {
        return gc.getDataStore();
    }

    public GarbageCollectorInfo getInfo() {
        return null;
    }

    public void setTestDelay(int testDelay) {
        gc.setTestDelay(testDelay);
    }

    public void setScanEventListener(ScanEventListener callback) {
        gc.setScanEventListener(callback);
    }

    public void setPersistenceManagerScan(boolean allow) {
        gc.setPersistenceManagerScan(allow);
    }

    public boolean getPersistenceManagerScan() {
        return gc.getPersistenceManagerScan();
    }
}