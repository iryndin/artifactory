/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2011 JFrog Ltd.
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

import org.apache.jackrabbit.core.persistence.IterablePersistenceManager;
import org.apache.jackrabbit.core.persistence.PersistenceManager;
import org.apache.jackrabbit.core.persistence.mem.InMemPersistenceManager;
import org.apache.jackrabbit.core.version.InternalVersionManagerImpl;
import org.artifactory.jcr.JcrSession;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Yoav Landman
 */
public abstract class JcrCoreUtils {
    protected JcrCoreUtils() {
        // utility class
    }

    public static IterablePersistenceManager[] getIterablePersistenceManagers(JcrSession session) {
        RepositoryImpl rep = (RepositoryImpl) session.getRepository();
        List<PersistenceManager> pmList = new ArrayList<PersistenceManager>();
        InternalVersionManagerImpl vm = rep.getRepositoryContext().getInternalVersionManager();
        PersistenceManager pm = vm.getPersistenceManager();
        pmList.add(pm);
        String[] wspNames = rep.getWorkspaceNames();
        for (String wspName : wspNames) {
            try {
                RepositoryImpl.WorkspaceInfo wspInfo = rep.getWorkspaceInfo(wspName);
                pm = wspInfo.getPersistenceManager();
            } catch (RepositoryException e) {
                throw new RuntimeException("Could not get persistence managers.", e);
            }
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
        return ipmArray;
    }

    public static Session[] getSystemSessions(JcrSession session) {
        RepositoryImpl rep = (RepositoryImpl) session.getRepository();
        String[] wspNames = rep.getWorkspaceNames();
        Session[] sysSessions = new Session[wspNames.length];
        for (int i = 0; i < wspNames.length; i++) {
            String wspName = wspNames[i];
            try {
                sysSessions[i] = rep.getSystemSession(wspName);
            } catch (RepositoryException e) {
                throw new RuntimeException("Could not get session list.", e);
            }
        }
        return sysSessions;
    }
}
