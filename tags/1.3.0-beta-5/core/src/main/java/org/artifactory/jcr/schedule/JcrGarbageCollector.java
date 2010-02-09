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
package org.artifactory.jcr.schedule;

import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.data.GarbageCollector;
import org.apache.log4j.Logger;
import org.artifactory.jcr.JcrService;
import org.artifactory.jcr.JcrSession;
import org.artifactory.maven.WagonManagerTempArtifactsCleaner;
import org.artifactory.schedule.ArtifactoryTimerTask;
import org.artifactory.spring.InternalContextHelper;
import org.springframework.transaction.annotation.Transactional;

import javax.jcr.RepositoryException;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public class JcrGarbageCollector extends ArtifactoryTimerTask {
    private final static Logger LOGGER = Logger.getLogger(WagonManagerTempArtifactsCleaner.class);

    @Override
    @Transactional
    public void onRun() {
        JcrService jcr = InternalContextHelper.get().getJcrService();
        JcrSession session = jcr.getManagedSession();
        GarbageCollector gc = null;
        try {
            SessionImpl internalSession = (SessionImpl) session.getSession();
            gc = internalSession.createDataStoreGarbageCollector();
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
        } catch (Exception e) {
            if (gc != null) {
                try {
                    gc.stopScan();
                } catch (RepositoryException re) {
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("GC scanning could not be stopped.", re);
                    }
                }
            }
            throw new RuntimeException(
                    "Jackrabbit's datastore garbage collector execution failed.", e);
        }
    }
}