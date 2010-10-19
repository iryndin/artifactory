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

package org.apache.jackrabbit.core;

import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.core.data.DataStore;
import org.apache.jackrabbit.core.data.db.DbDataStore;
import org.apache.jackrabbit.core.persistence.IterablePersistenceManager;
import org.apache.jackrabbit.core.persistence.PersistenceManager;
import org.apache.jackrabbit.core.persistence.mem.InMemPersistenceManager;
import org.apache.jackrabbit.core.state.ItemStateException;
import org.apache.jackrabbit.core.version.InternalVersionManagerImpl;
import org.artifactory.common.ConstantValues;
import org.artifactory.jcr.JcrSession;
import org.artifactory.jcr.gc.JcrGarbageCollector;
import org.artifactory.jcr.jackrabbit.ArtifactoryBaseDataStore;
import org.artifactory.jcr.jackrabbit.ArtifactoryDbGarbageCollector;
import org.artifactory.jcr.jackrabbit.ArtifactoryGarbageCollector;
import org.artifactory.log.LoggerFactory;
import org.slf4j.Logger;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.util.ArrayList;
import java.util.List;

/**
 * @author freds
 * @author yoavl
 * @date Mar 12, 2009
 */
public abstract class GarbageCollectorFactory {
    private static final Logger log = LoggerFactory.getLogger(GarbageCollectorFactory.class);

    private GarbageCollectorFactory() {
        // utility class
    }

    /**
     * Create a data store garbage collector for this repository.
     *
     * @return the garbage collector for the data store used in this JCR repository, or null if no need for garbage
     *         collection or no datastore found in session
     * @throws org.apache.jackrabbit.core.state.ItemStateException
     *
     * @throws javax.jcr.RepositoryException
     */
    public static JcrGarbageCollector createDataStoreGarbageCollector(JcrSession session)
            throws RepositoryException, ItemStateException {
        RepositoryImpl rep = (RepositoryImpl) session.getRepository();
        DataStore store = rep.getDataStore();
        if (store == null) {
            // GC activated before data store initialized
            log.info("Datastore not yet initialize. Not running garbage collector...");
            return null;
        }
        List<PersistenceManager> pmList = new ArrayList<PersistenceManager>();
        InternalVersionManagerImpl vm = (InternalVersionManagerImpl) rep.getVersionManager();
        PersistenceManager pm = vm.getPersistenceManager();
        pmList.add(pm);
        String[] wspNames = rep.getWorkspaceNames();
        Session[] sysSessions = new Session[wspNames.length];
        for (int i = 0; i < wspNames.length; i++) {
            String wspName = wspNames[i];
            RepositoryImpl.WorkspaceInfo wspInfo = rep.getWorkspaceInfo(wspName);
            sysSessions[i] = rep.getSystemSession(wspName);
            pm = wspInfo.getPersistenceManager();
            pmList.add(pm);
        }
        List<IterablePersistenceManager> ipmList = new ArrayList<IterablePersistenceManager>();
        for (PersistenceManager ipm : pmList) {
            if (ipm instanceof IterablePersistenceManager) {
                ipmList.add((IterablePersistenceManager) ipm);
            } else {
                // In memory PM are not a problem for GC
                if (!(ipm instanceof InMemPersistenceManager)) {
                    // Got an unmanageable persistence manager cannot used it
                    ipmList = null;
                    break;
                }
            }
        }
        IterablePersistenceManager[] ipmArray = null;
        if (ipmList != null) {
            ipmArray = ipmList.toArray(new IterablePersistenceManager[ipmList.size()]);
        }
        JcrGarbageCollector gc = null;
        if (store instanceof ArtifactoryBaseDataStore) {
            gc = new ArtifactoryDbGarbageCollector(session, ipmArray, sysSessions);
            ((ArtifactoryDbGarbageCollector) gc).addBinaryPropertyNames(new String[]{JcrConstants.JCR_DATA});
        } else if (!(store instanceof DbDataStore)) {
            gc = new ArtifactoryGarbageCollector(session, ipmArray, sysSessions);
            ((ArtifactoryGarbageCollector) gc).setSleepBetweenNodes(ConstantValues.gcSleepBetweenNodesMillis.getInt());
        } else {
            log.info("Store " + store.getClass().getName() + " does not support garbage collection");
        }
        return gc;
    }
}
