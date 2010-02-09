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
package org.artifactory.jcr;

import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.data.GarbageCollector;
import org.apache.log4j.Logger;
import org.artifactory.maven.WagonManagerTempArtifactsCleaner;
import org.artifactory.scheduling.ArtifactoryTimerTask;

import javax.jcr.RepositoryException;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public class JcrGarbageCollector extends ArtifactoryTimerTask {
    @SuppressWarnings({"UNUSED_SYMBOL", "UnusedDeclaration"})
    private final static Logger LOGGER = Logger.getLogger(WagonManagerTempArtifactsCleaner.class);

    @SuppressWarnings({"unchecked"})
    public void onRun() {
        JcrWrapper jcr = getArtifactoryContext().getJcr();
        jcr.doInSession(new JcrCallback<Object>() {
            public Object doInJcr(JcrSessionWrapper session) throws RepositoryException {
                activateGC((SessionImpl) session.getSession());
                return null;
            }
        });
    }

    private void activateGC(SessionImpl session) {
        GarbageCollector gc = null;
        try {
            gc = session.createDataStoreGarbageCollector();
            if (gc.getDataStore() == null) {
                if (LOGGER.isInfoEnabled()) {
                    LOGGER.info("Datastore not yet initialize. Not running garbage collector...");
                }
            }
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Runnning Jackrabbit's datastore garbage collector...");
            }
            gc.setSleepBetweenNodes(1000);
            gc.scan();
            gc.stopScan();
            int count = gc.deleteUnused();
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("Jackrabbit's datastore garbage collector deleted " + count +
                        " unreferenced item(s).");
            }
        } catch (Throwable t) {
            if (gc != null) {
                try {
                    gc.stopScan();
                } catch (RepositoryException e) {
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("GC scanning could not be stopped.", e);
                    }
                }
            }
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Jackrabbit's datastore garbage collector execution failed.", t);
            } else {
                LOGGER.warn("Jackrabbit's datastore garbage collector execution failed: " +
                        t.getMessage());
            }
        }
    }
}