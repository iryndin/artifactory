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
 *
 * Based on: /org/apache/jackrabbit/jackrabbit-core/1.6.0/jackrabbit-core-1.6.0.jar!
 * /org/apache/jackrabbit/core/data/GarbageCollector.class
 */

package org.artifactory.jcr.jackrabbit;

import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.core.RepositoryImpl;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.id.PropertyId;
import org.apache.jackrabbit.core.persistence.IterablePersistenceManager;
import org.apache.jackrabbit.core.state.ItemStateException;
import org.apache.jackrabbit.core.state.NoSuchItemStateException;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.core.state.PropertyState;
import org.apache.jackrabbit.core.value.BLOBFileValue;
import org.apache.jackrabbit.core.value.InternalValue;
import org.apache.jackrabbit.spi.Name;
import org.artifactory.api.storage.GarbageCollectorInfo;
import org.artifactory.common.ConstantValues;
import org.artifactory.jcr.JcrSession;
import org.artifactory.jcr.gc.JcrGarbageCollector;
import org.artifactory.log.LoggerFactory;
import org.artifactory.schedule.TaskInterruptedException;
import org.artifactory.util.ExceptionUtils;
import org.slf4j.Logger;

import javax.jcr.*;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
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
 *
 * @deprecated Was used when v1 GC existed. Remove after other refactoring completed
 */
//TODO: [by YS] Remove this class
@Deprecated
public class ArtifactoryDbGarbageCollector implements JcrGarbageCollector {
    private static final Logger log = LoggerFactory.getLogger(ArtifactoryDbGarbageCollector.class);

    private final Set<String> binaryPropertyNames = new HashSet<String>();

    private final ArtifactoryDataStore store;

    private final IterablePersistenceManager[] pmList;

    private final SessionImpl session;
    private final SessionWrapper[] sessionList;

    /**
     * Node paths with no binary data, used only when fix consistency is active
     */
    private final List<String> bereavedNodePaths;

    private boolean persistenceManagerScan;

    private GarbageCollectorInfo info;

    /**
     * Create a new garbage collector. This method is usually not called by the application, it is called by
     * SessionImpl.createDataStoreGarbageCollector().
     *
     * @param list           the persistence managers
     * @param sessionList    system sessions - one per workspace
     * @param fixConsistency
     */
    public ArtifactoryDbGarbageCollector(Session session, IterablePersistenceManager[] list, Session[] sessionList,
            boolean fixConsistency) {
        if (session instanceof SessionImpl) {
            this.session = (SessionImpl) session;
        } else if (session instanceof JcrSession) {
            this.session = (SessionImpl) ((JcrSession) session).getSession();
        } else {
            throw new RuntimeException(
                    "Could not find " + SessionImpl.class.getName() + " Jackarabbit session from " + session);
        }
        RepositoryImpl rep = (RepositoryImpl) session.getRepository();
        store = (ArtifactoryDataStore) rep.getDataStore();
        this.pmList = list;
        this.persistenceManagerScan = list != null;
        this.sessionList = new SessionWrapper[sessionList.length];
        for (int i = 0; i < sessionList.length; i++) {
            this.sessionList[i] = new SessionWrapper(sessionList[i]);
        }
        this.info = new GarbageCollectorInfo();
        info.bereavedNodesCount = 0;
        info.startScanTimestamp = 0;
        info.stopScanTimestamp = 0;
        if (fixConsistency) {
            this.bereavedNodePaths = new ArrayList<String>();
        } else {
            this.bereavedNodePaths = null;
        }
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
    @Override
    public boolean scan() throws RepositoryException,
            IllegalStateException, IOException, ItemStateException {
        // This test should always be true since the flow should be:
        // 1. create a new GC object
        // 2. mark/scan/delete
        // 3. drop the GC object
        if (info.startScanTimestamp != 0) {
            throw new IllegalStateException("Cannot execute multiple time the same Artifactory Garbage Collector!");
        }
        info.startScanTimestamp = System.currentTimeMillis();
        //info.dataStoreQueryTime = store.scanDataStore(System.nanoTime());
        //info.initialSize = store.getDataStoreSize();
        info.initialCount = store.getDataStoreNbElements();
        if (ConstantValues.gcUseIndex.getBoolean() || pmList == null || !persistenceManagerScan) {
            scanningSessionList();
        } else {
            scanPersistenceManagers();
        }
        return info.totalSizeFromBinaryProperties > 0L;
    }

    private void scanningSessionList() throws RepositoryException, IOException {
        info.totalBinaryPropertiesQueryTime = 0L;
        for (SessionWrapper sessionWrapper : sessionList) {
            sessionWrapper.findBinaryProperties();
            info.totalBinaryPropertiesQueryTime += sessionWrapper.binaryPropertiesQueryTime;
        }
        info.totalSizeFromBinaryProperties = 0L;
        for (SessionWrapper sessionWrapper : sessionList) {
            info.totalSizeFromBinaryProperties += sessionWrapper.markActiveJcrDataNodes();
        }
        //Remove bereaved nodes
        if (bereavedNodePaths != null) {
            cleanBereavedNodes();
        }
    }

    private void cleanBereavedNodes() throws RepositoryException {
        for (String bereaved : bereavedNodePaths) {
            Item item;
            try {
                item = session.getItem(bereaved);
            } catch (Exception e) {
                log.warn(
                        "Bereaved path item {} could not be retrieved (container node was probably removed first): {}.",
                        bereaved, e.getMessage());
                if (log.isDebugEnabled()) {
                    log.debug("Bereaved path item {} retrieval error.", bereaved, e);
                }
                continue;
            }
            log.warn("Removing binary node with no matching datastore data: {}.", item.getPath());
            if (JcrConstants.JCR_CONTENT.equals(item.getName())) {
                //If we are a jcr:content (of a file node container), remove the container node
                Node parent = item.getParent();
                parent.remove();
            } else {
                item.remove();
            }
        }
        if (bereavedNodePaths.size() > 0) {
            session.save();
        }
    }

    private class SessionWrapper {
        private final Session session;
        private long binaryPropertiesQueryTime;
        private NodeIterator results;

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
            sqlBuilder.append(" order by jcr:score");
            String xpathQuery = sqlBuilder.toString();
            return xpathQuery;
        }

        long markActiveJcrDataNodes() throws RepositoryException, IOException {
            long start = System.nanoTime();
            long result = 0L;
            while (results.hasNext()) {
                final Node node = results.nextNode();
                result += binarySize(node);
            }
            if (log.isTraceEnabled()) {
                log.trace(
                        "markActiveJcrDataNodes took " + ((long) ((System.nanoTime() - start) / 1000000L)) + "ms for " +
                                result + "bytes in " + results.getSize() + " elements");
            }
            return result;
        }

        void debugResults() throws RepositoryException {
            StringBuilder debugBuilder = new StringBuilder();
            while (results.hasNext()) {
                Node node = results.nextNode();
                debugBuilder.append("Node ").append(node.getIdentifier()).append(" '").append(node.getPath())
                        .append("'");
                for (String propertyName : binaryPropertyNames) {
                    Property p = node.getProperty(propertyName);
                    debugBuilder.append(" ").append(p.getName()).append("=").append(p.getLength());
                }
                debugBuilder.append("\n");
            }
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

    private void scanPersistenceManagers() throws ItemStateException, RepositoryException {
        info.totalBinaryPropertiesQueryTime = 0L;
        info.totalSizeFromBinaryProperties = 0L;
        long start = System.currentTimeMillis();
        Name[] binaryProps = new Name[binaryPropertyNames.size()];
        int i = 0;
        for (String binaryPropertyName : binaryPropertyNames) {
            binaryProps[i++] = session.getQName(binaryPropertyName);
        }
        long nodesCount = 0;
        for (IterablePersistenceManager pm : pmList) {
            Iterable<NodeId> ids = pm.getAllNodeIds(null, 0);
            for (NodeId id : ids) {
                if ((++nodesCount % 500L) == 0) {
                    if (ArtifactoryDataStore.pauseOrBreak()) {
                        throw new TaskInterruptedException();
                    }
                }
                try {
                    NodeState state = pm.load(id);
                    for (Name name : binaryProps) {
                        if (state.hasPropertyName(name)) {
                            PropertyId pid = new PropertyId(id, name);
                            PropertyState ps = pm.load(pid);
                            InternalValue[] values = ps.getValues();
                            for (InternalValue value : values) {
                                BLOBFileValue blobFileValue = value.getBLOBFileValue();
                                long length = 0;
                                try {
                                    if (blobFileValue.getDataIdentifier() != null) {
                                        length = store.getRecord(blobFileValue.getDataIdentifier()).getLength();
                                    }
                                } catch (MissingOrInvalidDataStoreRecordException e) {
                                    log.info("Node '{}' is bereaved", state.getId());
                                    doBereaved(session.getJCRPath(session.getHierarchyManager().getPath(state.getId())),
                                            name.getLocalName());
                                }
                                if (length > 0) {
                                    info.totalSizeFromBinaryProperties += length;
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    Throwable cause = ExceptionUtils
                            .getCauseOfTypes(e, ItemNotFoundException.class, NoSuchItemStateException.class);
                    if (cause == null) {
                        throw new RepositoryException(e);
                    }
                    // the node may have been deleted or moved in the meantime
                    // ignore it
                }
            }
        }
        //Remove bereaved nodes
        if (bereavedNodePaths != null) {
            cleanBereavedNodes();
        }
        info.totalBinaryPropertiesQueryTime = System.currentTimeMillis() - start;
    }

    @Override
    public void stopScan() throws RepositoryException {
        checkScanStarted();
        info.stopScanTimestamp = System.currentTimeMillis();
    }

    @Override
    public int deleteUnused() throws RepositoryException {
        checkScanStarted();
        checkScanStopped();
        long start = System.currentTimeMillis();

        //Do the actual clean
        long[] result = store.cleanUnreferencedItems();
        info.cleanedElementsCount = (int) result[0];
        info.totalSizeCleaned = result[1];
        //info.printV1CollectionInfo(start, store.getDataStoreSize());
        return info.cleanedElementsCount;
    }

    private void checkScanStarted() throws RepositoryException {
        if (info.startScanTimestamp == 0) {
            throw new RepositoryException("scan must be called first");
        }
    }

    private void checkScanStopped() throws RepositoryException {
        if (info.stopScanTimestamp == 0) {
            throw new RepositoryException("stopScan must be called first");
        }
    }

    /**
     * Read the length of all binary properties on the node. getLength on property never throws the DataStoreException
     * but -1 instead.
     *
     * @param n
     * @return
     * @throws RepositoryException
     * @throws IllegalStateException
     * @throws IOException
     */
    private long binarySize(final Node n) throws RepositoryException {
        long result = 0;
        for (String propertyName : binaryPropertyNames) {
            try {
                if (n.hasProperty(propertyName)) {
                    Property p = n.getProperty(propertyName);
                    // [by fsi] Stupid test since prop names should always be for binary entry
                    if (p.getType() == PropertyType.BINARY) {
                        if (p.getDefinition().isMultiple()) {
                            long[] lengths = p.getLengths();
                            for (long length : lengths) {
                                if (length < 0) {
                                    doBereaved(n.getPath(), propertyName);
                                } else {
                                    result += length;
                                }
                            }
                        } else {
                            long length;
                            length = p.getLength();
                            if (length < 0) {
                                doBereaved(n.getPath(), propertyName);
                            } else {
                                result += length;
                            }
                        }
                    } else {
                        String nodePath = n.getPath();
                        log.error("Declared binary property name " + propertyName +
                                " is not a binary property for node " + nodePath);
                    }
                }
            } catch (ItemNotFoundException e) {
                // The node was already deleted, ignore
            } catch (InvalidItemStateException e) {
                // The node was already deleted or moved, ignore
            } catch (RepositoryException e) {
                log.warn("Repository access error during read of garbage collection", e);
                throw e;
            }
        }
        return result;
    }

    private void doBereaved(String nodePath, String propertyName) {
        info.bereavedNodesCount++;
        String msg = "Could not read binary property " + propertyName + " on '" + nodePath +
                "' due to previous error!";
        log.warn(msg);
        if (bereavedNodePaths != null) {
            //The datastore record of the binary data has not been found - schedule node for cleanup
            bereavedNodePaths.add(nodePath);
            log.warn("Cannot determine the length of property {}. Node {} will be discarded.",
                    propertyName, nodePath);
        }
    }

    @Override
    public ArtifactoryDataStore getDataStore() {
        return store;
    }

    @Override
    public GarbageCollectorInfo getInfo() {
        return info;
    }

    public long getInitialSize() {
        return info.initialSize;
    }

    public long getStartScanTimestamp() {
        return info.startScanTimestamp;
    }

    public long getStopScanTimestamp() {
        return info.stopScanTimestamp;
    }

    public long getTotalSizeFromBinaryProperties() {
        return info.totalSizeFromBinaryProperties;
    }
}
