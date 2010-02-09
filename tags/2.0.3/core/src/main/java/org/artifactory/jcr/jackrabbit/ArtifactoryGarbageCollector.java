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
package org.artifactory.jcr.jackrabbit;

import org.apache.jackrabbit.core.NodeId;
import org.apache.jackrabbit.core.PropertyId;
import org.apache.jackrabbit.core.RepositoryImpl;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.persistence.IterablePersistenceManager;
import org.apache.jackrabbit.core.state.ItemStateException;
import org.apache.jackrabbit.core.state.NoSuchItemStateException;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.core.state.PropertyState;
import org.apache.jackrabbit.core.value.InternalValue;
import org.apache.jackrabbit.spi.Name;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Workspace;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Garbage collector for DataStore. This implementation is iterates through all nodes and reads the binary properties.
 * To detect nodes that are moved while the scan runs, event listeners are started. Like the well known garbage
 * collection in Java, the items that are still in use are marked. Currently this achieved by updating the modified date
 * of the entries. Newly added entries are detected because the modified date is changed when they are added.
 * <p/>
 * Example code to run the data store garbage collection:
 * <pre>
 * GarbageCollector gc = ((SessionImpl)session).createDataStoreGarbageCollector();
 * gc.scan();
 * gc.stopScan();
 * gc.deleteUnused();
 * </pre>
 */
public class ArtifactoryGarbageCollector {
    private static final Logger log = LoggerFactory.getLogger(ArtifactoryGarbageCollector.class);

    private final Collection<String> binaryPropertyNames = new HashSet<String>();

    private final ArtifactoryDbDataStoreImpl store;

    private long startScanTimestamp;

    private long stopScanTimestamp;

    private final IterablePersistenceManager[] pmList;

    private final SessionWrapper[] sessionList;

    private boolean persistenceManagerScan;

    private long initialSize;
    private int initialCount;
    private long totalBinaryPropertiesCount;
    private long totalBinaryPropertiesQueryTime;
    private long dataStoreQueryTime;

    /**
     * Create a new garbage collector. This method is usually not called by the application, it is called by
     * SessionImpl.createDataStoreGarbageCollector().
     *
     * @param list the persistence managers
     */
    public ArtifactoryGarbageCollector(SessionImpl session, IterablePersistenceManager[] list, Session[] sessionList) {
        RepositoryImpl rep = (RepositoryImpl) session.getRepository();
        store = (ArtifactoryDbDataStoreImpl) rep.getDataStore();
        this.pmList = list;
        this.persistenceManagerScan = list != null;
        this.sessionList = new SessionWrapper[sessionList.length];
        for (int i = 0; i < sessionList.length; i++) {
            this.sessionList[i] = new SessionWrapper(sessionList[i]);
        }
        this.startScanTimestamp = 0;
        this.stopScanTimestamp = 0;
    }

    /**
     * Add all the property names that can have a BINARY type. ATTENTION: When using this if a property with binary type
     * is not listed, the garbage collector will delete the data.
     *
     * @param propNames array of property names
     */
    public void addBinaryPropertyNames(String[] propNames) {
        Collections.addAll(binaryPropertyNames, propNames);
    }

    /**
     * Scan the repository. The garbage collector will iterate over all nodes in the repository and update the last
     * modified date. If all persistence managers implement the IterablePersistenceManager interface, this mechanism
     * will be used; if not, the garbage collector will scan the repository using the JCR API starting from the root
     * node.
     *
     * @return the total size of referenced binary properties
     * @throws javax.jcr.RepositoryException
     * @throws IllegalStateException
     * @throws java.io.IOException
     * @throws org.apache.jackrabbit.core.state.ItemStateException
     *
     */
    public long scan() throws RepositoryException,
            IllegalStateException, IOException, ItemStateException {
        long now = System.currentTimeMillis();
        if (startScanTimestamp == 0) {
            startScanTimestamp = now;
            dataStoreQueryTime = store.scanDataStore();
            initialSize = store.getDataStoreSize();
            initialCount = store.nbElementsToClean();
        }
        if (pmList == null || !persistenceManagerScan) {
            return scanningSessionList();
        } else {
            return scanPersistenceManagers();
        }
    }

    private long scanningSessionList() throws RepositoryException, IOException {
        totalBinaryPropertiesQueryTime = 0L;
        totalBinaryPropertiesCount = 0L;
        for (SessionWrapper aSessionList : sessionList) {
            totalBinaryPropertiesCount += aSessionList.findBinaryProperties();
            totalBinaryPropertiesQueryTime += aSessionList.binaryPropertiesQueryTime;
        }
        log.debug("Binary properties query execution time for took {} ms and found {} nodes",
                new Object[]{totalBinaryPropertiesQueryTime, totalBinaryPropertiesCount});
        long result = 0L;
        for (SessionWrapper aSessionList : sessionList) {
            result += aSessionList.readBinaryProperties();
        }
        return result;
    }

    private class SessionWrapper {
        private final Session session;
        private long binaryPropertiesQueryTime;
        private NodeIterator results;
        private long totalNotTouched;

        SessionWrapper(Session session) {
            this.session = session;
        }

        long findBinaryProperties() throws RepositoryException, IllegalStateException, IOException {
            long start = System.currentTimeMillis();
            Workspace workspace = session.getWorkspace();
            QueryManager queryManager = workspace.getQueryManager();
            String queryString = getSqlQuery();
            Query queryBinaryProperties = queryManager.createQuery(queryString, Query.SQL);
            QueryResult queryResult = queryBinaryProperties.execute();
            results = queryResult.getNodes();
            binaryPropertiesQueryTime = System.currentTimeMillis() - start;
            long size = results.getSize();
            log.debug("GC query execution time for {} took {} ms and found {} items",
                    new Object[]{queryString, binaryPropertiesQueryTime, size});
            return size;
        }

        String getXPathQuery() {
            StringBuilder xpathBuilder = new StringBuilder("/jcr:root//*[");
            boolean first = true;
            for (String propertyName : binaryPropertyNames) {
                if (!first) {
                    xpathBuilder.append(" or ");
                }
                xpathBuilder.append("@").append(propertyName);
                first = false;
            }
            xpathBuilder.append("]");
            String xpathQuery = xpathBuilder.toString();
            return xpathQuery;
        }

        private String getSqlQuery() {
            StringBuilder sqlBuilder = new StringBuilder("select * from nt:base where ");
            boolean first = true;
            for (String propertyName : binaryPropertyNames) {
                if (!first) {
                    sqlBuilder.append(" or ");
                }
                sqlBuilder.append(propertyName).append(" IS NOT NULL");
                first = false;
            }
            String xpathQuery = sqlBuilder.toString();
            return xpathQuery;
        }

        long readBinaryProperties() throws RepositoryException, IOException {
            long result = 0L;
            while (results.hasNext()) {
                result += binarySize(results.nextNode());
            }
            return result;
        }

        void debugResults() throws RepositoryException {
            totalNotTouched = 0L;
            StringBuilder debugBuilder = new StringBuilder();
            while (results.hasNext()) {
                int before = store.nbElementsToClean();
                Node node = results.nextNode();
                debugBuilder.append("Node ").append(node.getUUID()).append(" '").append(node.getPath()).append("'");
                for (String propertyName : binaryPropertyNames) {
                    Property p = node.getProperty(propertyName);
                    debugBuilder.append(" ").append(p.getName()).append("=").append(p.getLength());
                }
                boolean touched = (before - store.nbElementsToClean()) > 0;
                debugBuilder.append(" touched ").append(touched).append("\n");
                if (!touched) {
                    totalNotTouched++;
                }
            }
            debugBuilder.append("\n").append("Total not touched ").append(totalNotTouched);
            log.debug(debugBuilder.toString());
        }
    }

    /**
     * Enable or disable using the IterablePersistenceManager interface to scan the items. This is important for clients
     * that need the complete Node implementation in the ScanEventListener callback.
     *
     * @param allow true if using the IterablePersistenceManager interface is allowed
     */
    public void setPersistenceManagerScan(boolean allow) {
        persistenceManagerScan = allow;
    }

    /**
     * Check if using the IterablePersistenceManager interface is allowed.
     *
     * @return true if using IterablePersistenceManager is possible.
     */
    public boolean getPersistenceManagerScan() {
        return persistenceManagerScan;
    }

    private long scanPersistenceManagers() throws ItemStateException, RepositoryException {
        long result = 0L;
        for (IterablePersistenceManager pm : pmList) {
            Iterator it = pm.getAllNodeIds(null, 0);
            while (it.hasNext()) {
                NodeId id = (NodeId) it.next();
                try {
                    NodeState state = pm.load(id);
                    Set propertyNames = state.getPropertyNames();
                    for (Object propertyName : propertyNames) {
                        Name name = (Name) propertyName;
                        PropertyId pid = new PropertyId(id, name);
                        PropertyState ps = pm.load(pid);
                        if (ps.getType() == PropertyType.BINARY) {
                            InternalValue[] values = ps.getValues();
                            for (InternalValue value : values) {
                                result += value.getBLOBFileValue().getLength();
                            }
                        }
                    }
                } catch (NoSuchItemStateException e) {
                    // the node may have been deleted or moved in the meantime
                    // ignore it
                }
            }
        }
        return result;
    }

    public void stopScan() throws RepositoryException {
        checkScanStarted();
        // TODO: In parallel GC wait for all queries
        stopScanTimestamp = System.currentTimeMillis();
    }

    public long unusedCount() {
        return store.nbElementsToClean();
    }

    public long getDataStoreSize() {
        return store.getDataStoreSize();
    }

    public long deleteUnused() throws RepositoryException {
        checkScanStarted();
        checkScanStopped();
        int elToClean = store.nbElementsToClean();
        if (elToClean > 0) {
            long start = System.currentTimeMillis();
            long result = store.cleanUnreferencedItems();
            long end = System.currentTimeMillis();
            log.info("Artifactory Jackrabbit's datastore garbage collector report:\n" +
                    "Total execution:         " + (end - startScanTimestamp) + "ms :\n" +
                    "Data Store Query:        " + dataStoreQueryTime + "ms\n" +
                    "Binary Properties Query: " + totalBinaryPropertiesQueryTime + "ms\n" +
                    "Total Scanning:          " + (stopScanTimestamp - startScanTimestamp) + "ms\n" +
                    "Deletion:                " + (end - start) + "ms\n" +
                    "Initial element count:   " + initialCount + "\n" +
                    "Initial total size:      " + initialSize + "\n" +
                    "Elements cleaned:        " + elToClean + "\n" +
                    "Total size cleaned:      " + result + "\n" +
                    "Current total size:      " + store.getDataStoreSize()
            );
            return result;
        }
        return 0L;
    }

    private void checkScanStarted() throws RepositoryException {
        if (startScanTimestamp == 0) {
            throw new RepositoryException("scan must be called first");
        }
    }

    private void checkScanStopped() throws RepositoryException {
        if (stopScanTimestamp == 0) {
            throw new RepositoryException("stopScan must be called first");
        }
    }

    private long binarySize(final Node n) throws RepositoryException,
            IllegalStateException, IOException {
        long result = 0;
        for (String propertyName : binaryPropertyNames) {
            if (n.hasProperty(propertyName)) {
                Property p = n.getProperty(propertyName);
                if (p.getType() == PropertyType.BINARY) {
                    if (p.getDefinition().isMultiple()) {
                        long[] lengths = p.getLengths();
                        for (long length : lengths) {
                            result += length;
                        }
                    } else {
                        result += p.getLength();
                    }
                } else {
                    log.error("Declared binary property name " + propertyName + " is not a binary property for node " +
                            n.getPath());
                }
            }
        }
        return result;
    }

    public ArtifactoryDbDataStoreImpl getDataStore() {
        return store;
    }

    public long getInitialSize() {
        return initialSize;
    }

    public long getStartScanTimestamp() {
        return startScanTimestamp;
    }

    public long getStopScanTimestamp() {
        return stopScanTimestamp;
    }
}