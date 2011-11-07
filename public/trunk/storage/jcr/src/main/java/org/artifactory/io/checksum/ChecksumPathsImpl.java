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

package org.artifactory.io.checksum;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.apache.commons.lang.ArrayUtils;
import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.core.NodeImpl;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.data.DataIdentifier;
import org.apache.jackrabbit.core.data.DataStoreException;
import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.id.PropertyId;
import org.apache.jackrabbit.core.persistence.IterablePersistenceManager;
import org.apache.jackrabbit.core.state.NoSuchItemStateException;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.core.state.PropertyState;
import org.apache.jackrabbit.core.util.db.ArtifactoryConnectionHelper;
import org.apache.jackrabbit.core.util.db.DbUtility;
import org.apache.jackrabbit.core.value.BLOBFileValue;
import org.apache.jackrabbit.core.value.InternalValue;
import org.apache.jackrabbit.spi.Name;
import org.artifactory.api.storage.GarbageCollectorInfo;
import org.artifactory.common.ArtifactoryHome;
import org.artifactory.common.ConstantValues;
import org.artifactory.descriptor.config.CentralConfigDescriptor;
import org.artifactory.jcr.JcrService;
import org.artifactory.jcr.JcrSession;
import org.artifactory.jcr.jackrabbit.ArtifactoryDataStore;
import org.artifactory.jcr.jackrabbit.ArtifactoryDbDataRecord;
import org.artifactory.jcr.jackrabbit.ExtendedDbDataStore;
import org.artifactory.jcr.spring.StorageContextHelper;
import org.artifactory.jcr.utils.JcrUtils;
import org.artifactory.spring.Reloadable;
import org.artifactory.util.ExceptionUtils;
import org.artifactory.util.PathUtils;
import org.artifactory.version.CompoundVersionDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.jcr.Item;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Keeps bi-directional mappings between checksums and paths
 *
 * @author Yoav Landman
 */
@Reloadable(beanClass = ChecksumPaths.class, initAfter = JcrService.class)
public class ChecksumPathsImpl implements JcrChecksumPaths {

    private static final Logger log = LoggerFactory.getLogger(ChecksumPathsImpl.class);

    private static final String DELETE_FOR_CONSISTENCY_FIX_FILENAME = ".deleteForConsistencyFix";
    private static final int UNINITIALIZED_TS = -1;

    private ArtifactoryConnectionHelper connHelper;
    private String tableName;
    private boolean ready;
    private BinaryNodeListener binaryNodeListener;
    private AtomicLong nextTimestamp = new AtomicLong(UNINITIALIZED_TS);

    private String SQL_CREATE_TABLE;
    private String SQL_DROP_TABLE;
    private String SQL_CREATE_CS_IDX;
    private String SQL_CREATE_FNAME_IDX;
    private String SQL_CREATE_PATH_IDX;
    private String SQL_CREATE_BINNODE_IDX;
    private String SQL_CREATE_TS_IDX;
    private String SQL_RETRIEVE_MAX_TS;

    private String SQL_INSERT_CSPATH;
    private String SQL_CLEAR_CSNODE;
    private String SQL_RETRIEVE_DELETED_BINNODES;
    private String SQL_RETRIEVE_DELETED_CHECKSUM;
    private String SQL_RETRIEVE_CSPATHS_BY_CHECKSUM;
    private String SQL_RETRIEVE_ACTIVE_CSPATHS_BY_CHECKSUM;
    private String SQL_RETRIEVE_ALL_CSPATHS;
    private String SQL_RETRIEVE_NONDELETED_CSPATHS;
    private String SQL_RETRIEVE_NONDELETED_SIZE;
    private String SQL_RETRIEVE_PATHS;
    private String SQL_DELETE_BINNODES;
    private String SQL_DELETE_ALL;

    @Override
    public void init() {
        //Get a reference to the DS connection helper and create the table if needed
        boolean createTable;
        try {
            ExtendedDbDataStore dataStore = JcrUtils.getArtifactoryDataStore();
            String tablePrefix = dataStore.getDataStoreTablePrefix();
            tableName = tablePrefix + "CSPATHS";
            initQueries(dataStore);
            connHelper = dataStore.getConnectionHelper();
            createTable = !connHelper.tableExists(tableName);
            //For v1 - drop the table is exist and leave
            if (ConstantValues.gcUseV1.getBoolean()) {
                if (!createTable) {
                    log.info("Using legacy garbage collector. Removing the checksumPaths table...");
                    connHelper.update(SQL_DROP_TABLE);
                    log.info("ChecksumPaths table removed.");
                }
                return;
            }
            if (createTable) {
                log.info("Creating the checksumPaths table...");
                connHelper.update(SQL_CREATE_TABLE);
                connHelper.update(SQL_CREATE_CS_IDX);
                connHelper.update(SQL_CREATE_FNAME_IDX);
                connHelper.update(SQL_CREATE_PATH_IDX);
                connHelper.update(SQL_CREATE_BINNODE_IDX);
                connHelper.update(SQL_CREATE_TS_IDX);
                log.info("ChecksumPaths table has been created.");
            }
        } catch (Exception e) {
            throw new RuntimeException("Could not create/drop the checksumPaths table.", e);
        }
        boolean fixConsistency = createConsistencyFixFile();
        //When fixConsistency is on or when table is new, delete the table content and populate it from binary props
        //Protect against double init caused by converters
        if (createTable || fixConsistency) {
            nextTimestamp.compareAndSet(UNINITIALIZED_TS, 0);
            if (fixConsistency && createTable) {
                log.warn("Fix consistency cannot run the first time the checksumPaths table is created.");
                fixConsistency = false;
            }
            initBinNodes(fixConsistency);
        } else {
            nextTimestamp.compareAndSet(UNINITIALIZED_TS, getLastTimestamp() + 1);
        }
        ready = true;
    }

    @Override
    public void reload(CentralConfigDescriptor oldDescriptor) {
    }

    @Override
    public void destroy() {
        ready = false;
    }

    @Override
    public void convert(CompoundVersionDetails source, CompoundVersionDetails target) {
    }

    @Override
    public void setBinaryNodeListener(BinaryNodeListener binaryNodeListener) {
        if (ready) {
            throw new IllegalStateException(
                    "Checksum paths are initialized - too late for registering a binary node listener.");
        }
        this.binaryNodeListener = binaryNodeListener;
    }

    @Override
    public void addChecksumPath(ChecksumPathInfo info) {
        addChecksumPath(info, false);
    }

    public void addChecksumPath(ChecksumPathInfo info, boolean initializing) {
        if (!ready && !initializing) {
            if (log.isDebugEnabled()) {
                log.debug("Not ready - skipping add checksum path (" + info.getChecksum() + "/" + info.getSize() + "@" +
                        info.getPath() + ").");
            }
            return;
        }
        try {
            String fileName = PathUtils.getFileName(info.getPath());
            connHelper.update(SQL_INSERT_CSPATH, info.getChecksum(), info.getPath(), fileName, info.getSize(),
                    info.getBinaryNodeId(), nextTimestamp.getAndIncrement());
        } catch (Exception e) {
            throw new RuntimeException(
                    "Could not insert new checksum path (" + info.getChecksum() + "/" + info.getSize() + "@" +
                            info.getPath() + ").", e);
        }
    }

    /**
     * Removes (marks clear) the checksum for all entries identified by binaryNodeId
     *
     * @param binaryNodeId
     */
    @Override
    public void deleteChecksumPath(String binaryNodeId) {
        if (!ready) {
            if (log.isDebugEnabled()) {
                log.debug("Not ready - skipping clear checksum path for binary nodeId " + binaryNodeId + ".");
            }
            return;
        }
        try {
            connHelper.update(SQL_CLEAR_CSNODE, binaryNodeId, nextTimestamp.getAndIncrement());
        } catch (Exception e) {
            throw new RuntimeException("Could not clear checksum path for binary nodeId " + binaryNodeId + ".", e);
        }
    }

    /**
     * There are 3 major steps for the cleanup: <ol> <li>Find all deleted binnodes (records in the table with no
     * checksum)</li> <li>For each deleted binnode id, delete all records that has the same binnode id and lower or
     * equal timestamp. After this step the table contains only live records (except for those which might be added
     * after the gc stared)</li> <li>Select all the records from the datastore table that doesn't have a matching record
     * (by checksum) in the cspaths table. Those should be deleted.</li> </ol>
     *
     * @return Result of the cleanup
     */
    @Override
    public GarbageCollectorInfo cleanupDeleted() {
        boolean started = false;
        try {
            GarbageCollectorInfo info = new GarbageCollectorInfo();
            info.startScanTimestamp = System.currentTimeMillis();
            started = txBegin();
            ResultSet rs = null;
            try {
                //Avoid removing any record touched after the initial select
                long processingStart = System.nanoTime();
                dumpChecksumPathsTable();
                //Select all binnodes that were historically deleted (will also include active nodes)
                rs = connHelper.select(SQL_RETRIEVE_DELETED_BINNODES);
                Map<String, Long> deadBinaryNodes = new HashMap<String, Long>();
                while (rs.next()) {
                    //BINNODE, TS
                    String binaryNodeId = rs.getString(1);
                    long timestamp = rs.getLong(2);
                    //Collect potentially deleted checksums - keep the latest per checksum
                    Long lastCreated = deadBinaryNodes.get(binaryNodeId);
                    log.trace("Found deleted node marker {} timestamp @{}.", binaryNodeId, timestamp);
                    if (lastCreated == null || lastCreated < timestamp) {
                        deadBinaryNodes.put(binaryNodeId, timestamp);
                        log.trace("Registered deleted node {}.", binaryNodeId);
                    }
                }
                for (Map.Entry<String, Long> deadNodeEntry : deadBinaryNodes.entrySet()) {
                    //Clean up the cspaths table history
                    int deleted =
                            connHelper.update(SQL_DELETE_BINNODES, deadNodeEntry.getKey(), deadNodeEntry.getValue());
                    log.debug("Deleted {} binary nodes history records for {}.", deleted, deadNodeEntry.getKey());
                }
                //Now, delete any checksum that is not in the cspaths table
                long totalSize = 0;
                int cleanedChecksums = 0;
                ArtifactoryDataStore dataStore = JcrUtils.getArtifactoryDataStore();
                //Check if the checksum is in the db
                ResultSet liveChecksumsRs = null;
                try {
                    liveChecksumsRs = connHelper.select(SQL_RETRIEVE_DELETED_CHECKSUM);
                    while (liveChecksumsRs.next()) {
                        //Clean up the datastore and add to the live checksums count
                        String checksum = liveChecksumsRs.getString(1);
                        long size = liveChecksumsRs.getLong(2);
                        ArtifactoryDbDataRecord dataRecord = dataStore.getCachedRecord(checksum);
                        if (dataRecord == null) {
                            dataRecord = ArtifactoryDbDataRecord.createForDeletion(dataStore, checksum, size);
                            dataRecord = dataStore.addRecord(dataRecord);
                        }
                        if (dataRecord.markForDeletion(processingStart)) {
                            long sizeDeleted = dataStore.deleteRecord(dataRecord);
                            if (sizeDeleted == -1) {
                                //Deletion failed
                                log.debug("Could not delete artifact record {}", checksum);
                            } else {
                                cleanedChecksums++;
                                totalSize += size;
                                log.debug("Deleted artifact record: {}.", checksum);
                            }
                        } else {
                            log.debug("Skipping deletion for in-use artifact record: {}.", checksum);
                        }
                    }
                } finally {
                    DbUtility.close(liveChecksumsRs);
                }
                dumpChecksumPathsTable();
                if (started) {
                    txEnd(true);
                }
                info.totalSizeCleaned = totalSize;
                info.uniqueChecksumsCleaned = cleanedChecksums;
                //Count the number of node ids cleaned
                info.timelineRowsCleaned = deadBinaryNodes.size();
                info.gcEnd = System.currentTimeMillis();
                return info;
            } finally {
                DbUtility.close(rs);
            }
        } catch (Exception e) {
            if (started) {
                txEnd(false);
            }
            throw new RuntimeException("Could not clean up checksum paths.", e);
        }
    }


    @Override
    public ImmutableCollection<ChecksumPathInfo> getActiveChecksumPaths(@Nonnull String checksum) {
        ResultSet rs = null;
        try {
            rs = connHelper.select(SQL_RETRIEVE_ACTIVE_CSPATHS_BY_CHECKSUM, checksum);
            return checksumPathsFromResultSet(rs);
        } catch (Exception e) {
            throw new RuntimeException("Could not select active checksum paths (checksum=" + checksum + ").", e);
        } finally {
            DbUtility.close(rs);
        }
    }

    /**
     * Gets the active and deleted checksum paths - mostly for debugging
     *
     * @param checksum
     * @return
     */
    @Override
    public ImmutableCollection<ChecksumPathInfo> getChecksumPaths(@Nullable String checksum) {
        ResultSet rs = null;
        try {
            //When checksum is null return all
            if (checksum == null) {
                rs = connHelper.select(SQL_RETRIEVE_ALL_CSPATHS);
            } else {
                rs = connHelper.select(SQL_RETRIEVE_CSPATHS_BY_CHECKSUM, checksum);
            }
            return checksumPathsFromResultSet(rs);
        } catch (Exception e) {
            throw new RuntimeException("Could not select checksum paths (checksum=" + checksum + ").", e);
        } finally {
            DbUtility.close(rs);
        }
    }

    @Override
    public ImmutableCollection<ChecksumPathInfo> getAllActiveChecksumPaths() {
        ResultSet rs = null;
        try {
            rs = connHelper.select(SQL_RETRIEVE_NONDELETED_CSPATHS);
            return checksumPathsFromResultSet(rs);
        } catch (Exception e) {
            throw new RuntimeException("Could not select all checksum paths.", e);
        } finally {
            DbUtility.close(rs);
        }
    }

    @Override
    public long getActiveSize() {
        ResultSet rs = null;
        try {
            rs = connHelper.select(SQL_RETRIEVE_NONDELETED_SIZE);
            if (rs.next()) {
                return rs.getLong(1);
            } else {
                return 0;
            }
        } catch (Exception e) {
            throw new RuntimeException("Could not get total size.", e);
        } finally {
            DbUtility.close(rs);
        }
    }

    @Override
    public ImmutableCollection<String> getFileOrPathsLike(List<String> fileExpressions, List<String> pathExpressions) {
        ResultSet rs = null;
        StringBuilder likeSectionBuilder = new StringBuilder();

        appendLikeExpressions(fileExpressions, likeSectionBuilder, "SRC.FILENAME");
        appendLikeExpressions(pathExpressions, likeSectionBuilder, "SRC.PATH");

        String[] likeExpressions = ((String[]) ArrayUtils.addAll(getLikeExpressions(fileExpressions),
                getLikeExpressions(pathExpressions)));

        String query = String.format(SQL_RETRIEVE_PATHS, likeSectionBuilder.toString());
        try {
            rs = connHelper.select(query, likeExpressions);
            Set<String> resultPaths = Sets.newHashSet();
            while (rs.next()) {
                resultPaths.add(rs.getString(1));
            }
            return ImmutableList.copyOf(resultPaths);
        } catch (Exception e) {
            throw new RuntimeException("Could not select items (file likes =" + fileExpressions + ", path likes=" +
                    pathExpressions + ").", e);
        } finally {
            DbUtility.close(rs);
        }
    }

    private String[] getLikeExpressions(List<String> likeExpressions) {
        if (likeExpressions == null) {
            return new String[0];
        }

        return likeExpressions.toArray(new String[likeExpressions.size()]);
    }

    @Override
    public boolean txBegin() {
        if (connHelper != null && !connHelper.isTxActive()) {
            connHelper.txBegin();
            return true;
        }
        return false;
    }

    @Override
    public void txEnd(boolean commit) {
        if (connHelper != null) {
            connHelper.txEnd(commit);
        }
    }

    private void appendLikeExpressions(List<String> likeExpressions, StringBuilder likeSectionBuilder,
            String columnName) {
        if (likeExpressions != null && !likeExpressions.isEmpty()) {
            if (likeSectionBuilder.length() > 0) {
                likeSectionBuilder.append(" AND ");
            }
            likeSectionBuilder.append("(");
            int likeExpressionCount = likeExpressions.size();
            for (int i = 0; i < likeExpressionCount; i++) {
                likeSectionBuilder.append(columnName).append(" LIKE ?");
                if (i != (likeExpressionCount - 1)) {
                    likeSectionBuilder.append(" OR ");
                }
            }
            likeSectionBuilder.append(")");
        }
    }

    /**
     * This is called mainly for 2 reasons: first time the table is created or on fix consistency. But it may also be
     * called twice because of low level conversion.
     */
    private void initBinNodes(boolean fixConsistency) {
        JcrService jcrService = StorageContextHelper.get().getJcrService();
        JcrSession session = null;
        try {
            session = jcrService.getUnmanagedSession();
            ArtifactoryDataStore dataStore = (ArtifactoryDataStore) JcrUtils.getDataDtore(session);
            long start = System.currentTimeMillis();
            SessionImpl sessionImpl = (SessionImpl) session.getSession();
            long nodesCount = 0;
            List<String> bereavedNodePaths = Lists.newArrayList();
            long totalBinarySize = 0;
            IterablePersistenceManager[] persistenceManagers = JcrUtils.getIterablePersistenceManagers(session);
            log.info("Initializing binary nodes...");
            if (fixConsistency) {
                //First gc to make sure all pending deletion items were removed
                jcrService.garbageCollect(false);
                log.info("Note: Binary nodes consistency fix may take some time depending on your repository size.");
            }
            // Cleanup the table here (the init might be called twice)
            connHelper.update(SQL_DELETE_ALL);
            Name name = sessionImpl.getQName(JcrConstants.JCR_DATA);
            for (IterablePersistenceManager pm : persistenceManagers) {
                Iterable<NodeId> nodeIds = pm.getAllNodeIds(null, 0);
                for (NodeId nodeId : nodeIds) {
                    if ((++nodesCount % 1000L) == 0) {
                        log.info("Initialized {} binary nodes...", nodesCount);
                    }
                    try {
                        NodeState state = pm.load(nodeId);
                        if (state.hasPropertyName(name)) {
                            PropertyId pid = new PropertyId(nodeId, name);
                            PropertyState ps = pm.load(pid);
                            InternalValue[] values = ps.getValues();
                            String path;
                            try {
                                path = PathUtils.getAncesstor(
                                        sessionImpl.getJCRPath(
                                                sessionImpl.getHierarchyManager().getPath(state.getId())), 1);
                                if (binaryNodeListener != null) {
                                    try {
                                        NodeImpl node = sessionImpl.getNodeById(nodeId);
                                        binaryNodeListener.nodeVisited(node, fixConsistency);
                                    } catch (Exception e) {
                                        log.warn("Could not process binary node listener event on {}: {}", nodeId,
                                                e.getMessage());
                                        if (log.isDebugEnabled()) {
                                            log.warn("Error processing binary node listener event on {}.", nodeId, e);
                                        }
                                    }
                                }
                            } catch (RepositoryException e) {
                                log.warn("Could not get path for node {}: {}", nodeId, e.getMessage());
                                if (log.isDebugEnabled()) {
                                    log.warn("Error while getting path for node {}.", nodeId, e);
                                }
                                continue;
                            }
                            int dataStoreMinRecordLength = dataStore.getMinRecordLength();
                            for (InternalValue value : values) {
                                BLOBFileValue binary = value.getBLOBFileValue();
                                DataIdentifier identifier = binary.getDataIdentifier();
                                //Will blow-up when no actual binary found -> bereaved
                                long size = binary.getSize();
                                //Skip legacy binaries stored in db-bundles
                                if (size != -1 && size < dataStoreMinRecordLength) {
                                    log.debug("Skipping bundle-stored node '{}' of size {}.", path, size);
                                    continue;
                                }
                                // TODO: check for size == -1 before checking the identifier
                                boolean bereaved = false;
                                if (identifier != null) {
                                    File storeFile = null;
                                    try {
                                        storeFile = dataStore.getOrCreateFile(identifier, size);
                                        bereaved = !storeFile.exists() || size != storeFile.length();
                                        if (!bereaved) {
                                            //Add the cspath
                                            String checksum = identifier.toString();
                                            String id = nodeId.toString();
                                            ChecksumPathInfo info = new ChecksumPathInfo(path, checksum, size, id);
                                            addChecksumPath(info, true);
                                            totalBinarySize += size;
                                        }
                                    } catch (DataStoreException e) {
                                        bereaved = true;
                                        log.debug("Bereaved node found - no size for binary value.", e);
                                    } finally {
                                        if (dataStore.isStoreBinariesAsBlobs() && storeFile != null &&
                                                storeFile.exists()) {
                                            if (!storeFile.delete()) {
                                                log.warn("Failed to delete cache file: '{}'.",
                                                        storeFile.getAbsolutePath());
                                            }
                                        }
                                    }
                                } else {
                                    //As added precaution consider nodes with no identifier as bereaved only if no size
                                    if (size == -1) {
                                        bereaved = true;
                                        log.debug("Bereaved node found - no identifier.");
                                    } else {
                                        log.debug("Node has identifier and size is {}. Keeping the node '{}'.",
                                                size, path);
                                    }
                                }
                                if (bereaved) {
                                    log.debug("Node '{}' is bereaved ({}).", path, state.getId());
                                    //The binary property has not been found - node is up for cleanup
                                    bereavedNodePaths.add(path);
                                    if (fixConsistency) {
                                        log.warn("Node '{}'is bereaved and will be discarded.", path);
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        Throwable cause = ExceptionUtils
                                .getCauseOfTypes(e, ItemNotFoundException.class, NoSuchItemStateException.class);
                        if (cause == null) {
                            throw new RuntimeException("Binary node scanning terminated.", e);
                        }
                        //The node may have been deleted or moved in the meantime - ignore it
                    }
                }
            }
            //Remove bereaved nodes
            if (fixConsistency && bereavedNodePaths.size() > 0) {
                cleanBereavedNodes(session, bereavedNodePaths);
            }
            long totalTime = System.currentTimeMillis() - start;
            log.info("Initialized {} binary nodes ({} ms). Total size: {} bytes.", new Object[]{nodesCount, totalTime,
                    totalBinarySize});
        } catch (Exception e) {
            throw new RuntimeException("Could not init binary nodes.", e);
        } finally {
            if (session != null) {
                session.logout();
            }
        }
    }

    private long getLastTimestamp() {
        ResultSet rs = null;
        try {
            rs = connHelper.select(SQL_RETRIEVE_MAX_TS);
            if (rs.next()) {
                return rs.getLong(1);
            } else {
                return UNINITIALIZED_TS;
            }
        } catch (Exception e) {
            throw new RuntimeException("Could not select max timestamp.", e);
        } finally {
            DbUtility.close(rs);
        }
    }

    private void cleanBereavedNodes(JcrSession session, @Nonnull List<String> bereavedNodePaths)
            throws RepositoryException {
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
        session.save();
        log.info("Cleaned up {} bereaved node(s).", bereavedNodePaths.size());
    }

    private ImmutableCollection<ChecksumPathInfo> checksumPathsFromResultSet(ResultSet rs) throws SQLException {
        List<ChecksumPathInfo> results = Lists.newArrayList();
        while (rs.next()) {
            //CHECKSUM, PATH, BSIZE, BINNODE, TS
            String rsChecksum = rs.getString(1);
            String path = rs.getString(2);
            long size = rs.getLong(3);
            String binaryNodeId = rs.getString(4);
            long timestamp = rs.getLong(5);
            ChecksumPathInfo result = new ChecksumPathInfo(path, rsChecksum, size, binaryNodeId, timestamp);
            results.add(result);
        }
        return ImmutableList.copyOf(results);
    }

    private void initQueries(ExtendedDbDataStore dataStore) {
        Properties dbprops = getDbProperties(dataStore.getDatabaseType());

        SQL_CREATE_TABLE = getProperty(dbprops, "createTable",
                "CREATE TABLE ${tableName} (CHECKSUM CHAR(40), PATH VARCHAR(1024), FILENAME VARCHAR(255), BSIZE BIGINT, BINNODE CHAR(36), TS BIGINT)");
        SQL_DROP_TABLE = getProperty(dbprops, "dropTable", "DROP TABLE ${tableName}");
        SQL_CREATE_CS_IDX =
                getProperty(dbprops, "createChecksumIndex", "CREATE INDEX CS_IDX ON ${tableName} (CHECKSUM)");
        SQL_CREATE_FNAME_IDX =
                getProperty(dbprops, "createFileNameIndex", "CREATE INDEX FNAME_IDX ON ${tableName} (FILENAME)");
        SQL_CREATE_PATH_IDX = getProperty(dbprops, "createPathIndex", "CREATE INDEX PATH_IDX ON ${tableName} (PATH)");
        SQL_CREATE_BINNODE_IDX =
                getProperty(dbprops, "createBinNodeIndex", "CREATE INDEX BINNODE_IDX ON ${tableName} (BINNODE)");
        SQL_CREATE_TS_IDX =
                getProperty(dbprops, "createCreatedIndex",
                        "CREATE UNIQUE INDEX TS_IDX ON ${tableName} (TS DESC)");

        SQL_INSERT_CSPATH = getProperty(dbprops, "insertCsPath",
                "INSERT INTO ${tableName} (CHECKSUM, PATH, FILENAME, BSIZE, BINNODE, TS) VALUES (?, ?, ?, ?, ?, ?)");
        SQL_CLEAR_CSNODE =
                getProperty(dbprops, "clearNode", "INSERT INTO ${tableName} (BINNODE, TS) VALUES (?, ?)");

        SQL_RETRIEVE_DELETED_BINNODES =
                getProperty(dbprops, "retrieveDeletedBinnodes",
                        "SELECT BINNODE, TS FROM ${tableName} WHERE CHECKSUM IS NULL");
        SQL_RETRIEVE_DELETED_CHECKSUM =
                getProperty(dbprops, "retrieveDeletedChecksums",
                        "SELECT ID, LENGTH FROM " + dataStore.getTableName() + " DS " +
                                "WHERE NOT EXISTS (SELECT 1 FROM ${tableName} WHERE CHECKSUM=DS.ID)");
        SQL_RETRIEVE_CSPATHS_BY_CHECKSUM = getProperty(dbprops, "retrievCsPathsByChecksum",
                "SELECT CHECKSUM, PATH, BSIZE, BINNODE, TS FROM ${tableName} WHERE CHECKSUM=?");
        SQL_RETRIEVE_ACTIVE_CSPATHS_BY_CHECKSUM = getProperty(dbprops, "retrievActiveCsPaths",
                "SELECT CHECKSUM, PATH, BSIZE, BINNODE, TS FROM ${tableName} SRC " +
                        "WHERE SRC.CHECKSUM=? AND SRC.TS IN " +
                        "(SELECT MAX(TS) FROM ${tableName} TGT WHERE TGT.BINNODE=SRC.BINNODE)");
        SQL_RETRIEVE_ALL_CSPATHS = getProperty(dbprops, "retrievAllCsPaths",
                "SELECT CHECKSUM, PATH, BSIZE, BINNODE, TS FROM ${tableName}");
        SQL_RETRIEVE_PATHS = getProperty(dbprops, "retrievPaths", "SELECT PATH FROM ${tableName} SRC WHERE %s");
        SQL_RETRIEVE_MAX_TS =
                getProperty(dbprops, "retrieveMaxCreated", "SELECT MAX(TS) FROM ${tableName}");

        SQL_DELETE_BINNODES = getProperty(dbprops, "deleteBinNodes", "DELETE FROM ${tableName} WHERE BINNODE=? " +
                "AND TS <= ?");
        SQL_DELETE_ALL = getProperty(dbprops, "deleteAll", "DELETE FROM ${tableName}");
        SQL_RETRIEVE_NONDELETED_CSPATHS =
                getProperty(dbprops, "retrieveNonDeletedCsPaths",
                        "SELECT CHECKSUM, PATH, BSIZE, BINNODE, TS FROM ${tableName} SRC " +
                                "WHERE SRC.CHECKSUM IS NOT NULL AND SRC.TS IN " +
                                "(SELECT MAX(TS) FROM ${tableName} TGT WHERE TGT.BINNODE=SRC.BINNODE)");
        SQL_RETRIEVE_NONDELETED_SIZE =
                getProperty(dbprops, "retrieveNonDeletedSize",
                        "SELECT SUM(T.BSIZE) FROM (SELECT DISTINCT CHECKSUM, BSIZE FROM ${tableName} SRC " +
                                "WHERE SRC.CHECKSUM IS NOT NULL AND SRC.TS IN " +
                                "(SELECT MAX(TS) FROM ${tableName} TGT WHERE TGT.BINNODE=SRC.BINNODE)) AS T");
        //Select sum of all sizes where checksums are unique and are the latest entry
    }

    private static Properties getDbProperties(String type) {
        InputStream in = ChecksumPathsImpl.class.getResourceAsStream("/" + type + ".properties");
        Properties props = new Properties();
        if (in == null) {
            log.debug("Could not find db-specific properties for '{}'.", type);
        } else {
            try {
                try {
                    props.load(in);
                } finally {
                    in.close();
                }
            } catch (IOException e) {
                log.error("Could not read properties '{}.properties'", type, e);
            }
        }
        return props;
    }

    protected String getProperty(Properties dbprops, String key, String defaultValue) {
        String property = dbprops.getProperty(key, defaultValue);
        return property.replace("${tableName}", tableName).trim();
    }

    /**
     * Creates/recreates the file that disables fix consistency. Also checks we have proper write access to the data
     * folder.
     *
     * @return true if the deleteForConsistencyFix file did not exist and was created
     */
    public static boolean createConsistencyFixFile() {
        File deleteForConsistencyFix = getConsistencyFixFile();
        if (deleteForConsistencyFix.exists()) {
            return false;
        }
        try {
            boolean created = deleteForConsistencyFix.createNewFile();
            if (created) {
                //Protect against accidental deletion when pointing at missing data folder
                return !ArtifactoryHome.get().isNewDataDir();
            } else {
                log.error("File '{}' was not created. Already exists?", deleteForConsistencyFix.getAbsolutePath());
            }
        } catch (IOException e) {
            throw new RuntimeException(
                    "Could not create file: '" + deleteForConsistencyFix.getAbsolutePath() + "'.", e);
        }
        return false;
    }

    public static File getConsistencyFixFile() {
        return new File(ArtifactoryHome.get().getDataDir(), DELETE_FOR_CONSISTENCY_FIX_FILENAME);
    }

    @Override
    public void dumpChecksumPathsTable() {
        if (!log.isTraceEnabled()) {
            return;
        }
        ImmutableCollection<ChecksumPathInfo> checksumPaths = getChecksumPaths(null);
        StringBuilder sb = new StringBuilder(String.format(
                "%-44s%-10s %-40s%-20s%s%n", "Checksum", "Size", "Node ID", "Timestamp", "Path"));
        for (ChecksumPathInfo cp : checksumPaths) {
            sb.append(String.format("%-44s%10s  %-40s%-20s%s%n", cp.getChecksum(), cp.getSize(),
                    cp.getBinaryNodeId(), cp.getTimestamp(), cp.getPath()));
        }
        log.trace("Dumping Checksum Paths Table ({} records):\n {}", checksumPaths.size(), sb);
    }
}
