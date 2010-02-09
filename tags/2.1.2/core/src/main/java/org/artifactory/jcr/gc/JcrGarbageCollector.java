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

package org.artifactory.jcr.gc;

import org.apache.jackrabbit.core.data.DataStore;
import org.apache.jackrabbit.core.state.ItemStateException;
import org.artifactory.api.storage.GarbageCollectorInfo;

import javax.jcr.RepositoryException;
import java.io.IOException;

/**
 * @author freds
 * @date Jun 23, 2009
 */
public interface JcrGarbageCollector {
    /**
     * Scan the repository for binary object to garbage collect
     *
     * @return true if found some things to garbage, false otherwise
     * @throws RepositoryException
     * @throws IllegalStateException
     * @throws IOException
     * @throws ItemStateException
     */
    boolean scan() throws RepositoryException, IllegalStateException, IOException, ItemStateException;

    void stopScan() throws RepositoryException;

    int deleteUnused() throws RepositoryException;

    DataStore getDataStore();

    /**
     * Returns the garbage collection info object
     *
     * @return GarbageCollectorInfo
     */
    GarbageCollectorInfo getInfo();
}
