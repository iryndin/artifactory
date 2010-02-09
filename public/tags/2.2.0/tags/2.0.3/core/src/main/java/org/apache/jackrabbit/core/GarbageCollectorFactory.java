/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.core;

import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.core.persistence.IterablePersistenceManager;
import org.apache.jackrabbit.core.persistence.PersistenceManager;
import org.apache.jackrabbit.core.state.ItemStateException;
import org.apache.jackrabbit.core.version.VersionManagerImpl;
import org.artifactory.jcr.jackrabbit.ArtifactoryGarbageCollector;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.util.ArrayList;
import java.util.List;

/**
 * @author freds
 * @date Mar 12, 2009
 */
public class GarbageCollectorFactory {
    /**
     * Create a data store garbage collector for this repository.
     *
     * @throws org.apache.jackrabbit.core.state.ItemStateException
     *
     * @throws javax.jcr.RepositoryException
     */
    public static ArtifactoryGarbageCollector createDataStoreGarbageCollector(SessionImpl session)
            throws RepositoryException, ItemStateException {
        List<PersistenceManager> pmList = new ArrayList<PersistenceManager>();
        RepositoryImpl rep = (RepositoryImpl) session.getRepository();
        VersionManagerImpl vm = (VersionManagerImpl) rep.getVersionManager();
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
        IterablePersistenceManager[] ipmList = new IterablePersistenceManager[pmList.size()];
        for (int i = 0; i < pmList.size(); i++) {
            pm = pmList.get(i);
            if (!(pm instanceof IterablePersistenceManager)) {
                ipmList = null;
                break;
            }
            ipmList[i] = (IterablePersistenceManager) pm;
        }
        ArtifactoryGarbageCollector gc =
                new ArtifactoryGarbageCollector(session, ipmList, sysSessions);
        gc.addBinaryPropertyNames(new String[]{JcrConstants.JCR_DATA});
        return gc;
    }
}
