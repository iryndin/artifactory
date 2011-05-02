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

package org.artifactory.traffic;

import com.google.common.collect.Maps;
import org.apache.jackrabbit.util.ISO8601;
import org.apache.jackrabbit.value.DateValue;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.api.repo.exception.RepositoryRuntimeException;
import org.artifactory.api.search.JcrQuerySpec;
import org.artifactory.common.ConstantValues;
import org.artifactory.concurrent.ExpiringDelayed;
import org.artifactory.descriptor.config.CentralConfigDescriptor;
import org.artifactory.jcr.JcrPath;
import org.artifactory.jcr.JcrService;
import org.artifactory.jcr.JcrSession;
import org.artifactory.jcr.JcrTypes;
import org.artifactory.log.LoggerFactory;
import org.artifactory.repo.jcr.JcrHelper;
import org.artifactory.repo.service.InternalRepositoryService;
import org.artifactory.schedule.TaskService;
import org.artifactory.schedule.quartz.QuartzTask;
import org.artifactory.spring.InternalArtifactoryContext;
import org.artifactory.spring.InternalContextHelper;
import org.artifactory.spring.Reloadable;
import org.artifactory.traffic.entry.TrafficEntry;
import org.artifactory.traffic.entry.TransferEntry;
import org.artifactory.traffic.mbean.Traffic;
import org.artifactory.traffic.mbean.TrafficMBean;
import org.artifactory.traffic.read.TrafficReader;
import org.artifactory.util.LoggingUtils;
import org.artifactory.version.CompoundVersionDetails;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.QueryResult;
import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.EnumMap;
import java.util.List;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.TimeUnit;

/**
 * @author yoavl
 */
@Service
@Reloadable(beanClass = InternalTrafficService.class, initAfter = {InternalRepositoryService.class, TaskService.class})
public class TrafficServiceImpl implements InternalTrafficService {
    private static final Logger log = LoggerFactory.getLogger(TrafficServiceImpl.class);

    public static final String LOG_FOLDER = "traffic";

    private final String entryPathBase = getLogFolderPath() + "/" + Long.toString(System.currentTimeMillis());

    String NODE_ARTIFACTORY_LOG_ENTRY = JcrTypes.ARTIFACTORY_PREFIX + "logEntry";

    @Autowired
    private JcrService jcr;

    @Autowired
    private TaskService taskService;

    private int entriesRetentionSecs;

    private DelayQueue<DelayedTrafficEntry> entriesToHandle = new DelayQueue<DelayedTrafficEntry>();

    private EnumMap<TrafficCollectorResolution, TrafficCollector> collectors;

    private boolean active;


    public void init() {
        //Create the traffic log folder
        jcr.getOrCreateUnstructuredNode(getLogFolderPath());

        //Init the collectors
        collectors = Maps.newEnumMap(TrafficCollectorResolution.class);
        if (ConstantValues.test.getBoolean()) {
            createCollector(TrafficCollectorResolution.SECOND,
                    Calendar.MILLISECOND);
        } else {
            //The usual collectors
            createCollector(TrafficCollectorResolution.MINUTE,
                    Calendar.MILLISECOND, Calendar.SECOND);
            createCollector(TrafficCollectorResolution.HOUR,
                    Calendar.MILLISECOND, Calendar.SECOND, Calendar.MINUTE);
            createCollector(TrafficCollectorResolution.DAY,
                    Calendar.MILLISECOND, Calendar.SECOND, Calendar.MINUTE, Calendar.HOUR, Calendar.HOUR_OF_DAY);
            createCollector(TrafficCollectorResolution.WEEK,
                    Calendar.MILLISECOND, Calendar.SECOND, Calendar.MINUTE, Calendar.HOUR, Calendar.HOUR_OF_DAY,
                    Calendar.DAY_OF_WEEK);
            createCollector(TrafficCollectorResolution.MONTH,
                    Calendar.MILLISECOND, Calendar.SECOND, Calendar.MINUTE, Calendar.HOUR, Calendar.HOUR_OF_DAY,
                    Calendar.DAY_OF_WEEK, Calendar.DAY_OF_MONTH, Calendar.WEEK_OF_MONTH);
        }

        //Register a mbean
        InternalArtifactoryContext context = InternalContextHelper.get();
        Traffic traffic = new Traffic(context.beanForType(InternalTrafficService.class));
        context.registerArtifactoryMBean(traffic, TrafficMBean.class, null);

        //Cache the retention period value and log dir
        //For performance, but also required to be done once, so it's not reevaluated when invoked from the traffic
        //mbean, where no artifactory properties or artifactory context are available on the current thread.
        entriesRetentionSecs = ConstantValues.trafficEntriesRetentionSecs.getInt();

        active = ConstantValues.trafficCollectionActive.getBoolean();
        if (active) {
            //Init the global entries collector
            QuartzTask entriesCollector = new QuartzTask(TrafficEntriesCollector.class,
                    TimeUnit.SECONDS.toMillis(ConstantValues.trafficCollectionIntervalSecs.getInt()));
            entriesCollector.setSingleton(true);
            taskService.startTask(entriesCollector);
        }
    }

    public void reload(CentralConfigDescriptor oldDescriptor) {
        //nop
    }

    public void destroy() {
        //nop
    }

    public void convert(CompoundVersionDetails source, CompoundVersionDetails target) {
        //When conversion is needed, remove all old stats
    }

    public void handleTrafficEntry(TrafficEntry entry) {
        if (active) {
            //Add the entry to the processing queue
            entriesToHandle.add(new DelayedTrafficEntry(entry));
        }
    }

    private void handleTrafficEntryInternal(TrafficEntry entry, Session session) {
        //Store a log entry and notify the queue
        String entryPath = generateEntryPath();
        try {
            Node rootNode = session.getRootNode();
            while (rootNode.hasNode((entryPath))) {
                //We are extremely(!) lucky
                entryPath = generateEntryPath();
            }
            final Node entryNode = JcrHelper.getOrCreateNode(rootNode, entryPath, NODE_ARTIFACTORY_LOG_ENTRY);
            final Calendar calendar = Calendar.getInstance();
            calendar.setTime(entry.getDate());
            entryNode.setProperty(JcrTypes.PROP_ARTIFACTORY_TIMESTAMP, new DateValue(calendar));
            //Store the serialized entry
            entryNode.setProperty(JcrTypes.PROP_ARTIFACTORY_TRAFFIC_ENTRY, entry.toString());
            log.trace("#### Saved {}", entry);
        } catch (Exception e) {
            LoggingUtils.warnOrDebug(log, "Could not log traffic", e);
        }
        if (entry instanceof TransferEntry) {
            TrafficLogger.logTransferEntry((TransferEntry) entry);
        }
    }

    public void collect() {
        //Process any entries that are due (non-tx)
        ArrayList<DelayedTrafficEntry> entries = new ArrayList<DelayedTrafficEntry>();
        int queued = entriesToHandle.size();
        int readyCount = entriesToHandle.drainTo(entries);
        JcrSession session = null;
        try {
            session = jcr.getUnmanagedSession();
            if (readyCount > 0) {
                long start = System.currentTimeMillis();
                for (DelayedTrafficEntry delayedEntry : entries) {
                    handleTrafficEntryInternal(delayedEntry.getEntry(), session);
                }
                session.save();
                log.debug("Processed {} pending traffic entries out of {} in {}ms.", new Object[]{readyCount,
                        queued, System.currentTimeMillis() - start});
            } else {
                log.debug("No eligible pending traffic entries to process ({} in queue).", queued);
            }

            //Select all entries up to the last round collection point
            Calendar entriesCollection = Calendar.getInstance();
            entriesCollection.clear(Calendar.MILLISECOND);

            //Get the last time data collection on entries has been done
            Calendar entriesLastCollected = getLastCollected(null, session);

            if (log.isDebugEnabled()) {
                //Select the entries of this collection
                TrafficEntriesIterator entriesIterator = getDatabaseEntries(entriesLastCollected, entriesCollection,
                        session);
                log.debug("Collected {} from {} to {}",
                        new Object[]{entriesIterator.size(),
                                entriesLastCollected != null ? entriesLastCollected.getTime() : "any",
                                entriesCollection.getTime()});
            }

            //Notify collectors - call each collector with the collection time so that he knows if it's time for him to
            //kick-in and collect the data
            for (TrafficCollector collector : collectors.values()) {
                collector.collect(entriesCollection, me(), session);
            }

            //Finally, update the global last collected indicator in the store
            updateLastCollected(null, entriesCollection, session);
            session.save();
        } finally {
            if (session != null) {
                session.logout();
            }
        }
    }

    /**
     * Clean up entries according to the retention period.
     * <p/>
     * A strong assumption is that, for all collectors:
     * <pre>retention period - 2 * collector collection interval > 0</pre>
     */
    public void cleanup() {
        final Calendar cleanupMark = getCleanupMark();
        JcrSession session = null;
        try {
            session = jcr.getUnmanagedSession();
            //Never throw away uncollected data - only remove down to lastCollected
            final Calendar lastCollected = getLastCollected(null, session);
            if (lastCollected != null && cleanupMark.after(lastCollected)) {
                cleanupMark.setTime(lastCollected.getTime());
            }
            final TrafficEntriesIterator entriesIterator = getDatabaseEntries(null, cleanupMark, session);
            log.debug("Found {} traffic entries to remove (up to {}).", entriesIterator.size(),
                    cleanupMark.getTime());
            while (entriesIterator.hasNext()) {
                entriesIterator.next();
                entriesIterator.remove();
                session.save();
            }
        } finally {
            if (session != null) {
                session.logout();
            }
        }
    }

    /**
     * Returns traffic entries
     *
     * @param from Traffic start time
     * @param to   Traffic end time
     * @return List<TrafficEntry> taken from the traffic log files or the database
     */
    public List<TrafficEntry> getEntryList(Calendar from, Calendar to) {
        List<TrafficEntry> entryList = new ArrayList<TrafficEntry>();

        //Traffic entries are retained in the data base for a limited amount of time. Obtain the retention start time
        Calendar retentionStart = getCleanupMark();

        boolean fromIsBeforeRetentionStart =
                from == null || TrafficUtils.dateEqualsBefore(from.getTime(), retentionStart.getTime());
        boolean toIsAfterRetentionStart = to == null || to.after(retentionStart);

        /**
         * If the requested traffic window start time is equals or before the retention period start time, fall back
         * to the log files to obtain the traffic data for the period which isn't covered in the database.
         */
        if (fromIsBeforeRetentionStart) {
            List<TrafficEntry> logFileEntries = getLogFileEntries(from, retentionStart);
            entryList.addAll(logFileEntries);

            /**
             * Set from to the retention start period, so if we query the database as well as the log files, we won't
             * Have overlapping periods and entry duplication.
             */
            from = retentionStart;
        }

        /**
         * If the requested traffic window end time is after the retention period start time, query the database to
         * obtain the traffic data.
         */
        if (toIsAfterRetentionStart) {
            JcrSession session = null;
            try {
                session = jcr.getUnmanagedSession();
                TrafficEntriesIterator entries = getDatabaseEntries(from, to, session);
                while (entries.hasNext()) {
                    entryList.add(entries.next());
                }
            } finally {
                if (session != null) {
                    session.logout();
                }
            }
        }
        return entryList;
    }

    /**
     * Returns traffic entries from the traffic log files
     *
     * @param from Traffic start time
     * @param to   Traffic end time
     * @return List<TrafficEntry> taken from the traffic log files
     */
    public List<TrafficEntry> getLogFileEntries(Calendar from, Calendar to) {
        TrafficReader trafficReader = new TrafficReader(getLogDir());
        List<TrafficEntry> logFileEntries = trafficReader.getEntries(from, to);
        return logFileEntries;
    }

    /**
     * Returns recent traffic entries that are retained in the database
     *
     * @param from    Traffic start time
     * @param to      Traffic end time
     * @param session
     * @return TrafficEntriesIterator taken from the database
     */
    public TrafficEntriesIterator getDatabaseEntries(Calendar from, Calendar to, JcrSession session) {
        try {
            StringBuilder query = new StringBuilder().append("//element(*, ").append(NODE_ARTIFACTORY_LOG_ENTRY)
                    .append(")");
            if (from != null) {
                query.append("[@").append(JcrTypes.PROP_ARTIFACTORY_TIMESTAMP).append(" >= xs:dateTime('")
                        .append(ISO8601.format(from)).append("')");
            }
            if (to != null) {
                if (from != null) {
                    query.append(" and ");
                } else {
                    query.append("[");
                }
                query.append("@").append(JcrTypes.PROP_ARTIFACTORY_TIMESTAMP).append(" < xs:dateTime('")
                        .append(ISO8601.format(to)).append("')");
            }
            if (from != null || to != null) {
                query.append("]");
            }
            query.append(" order by @" + JcrTypes.PROP_ARTIFACTORY_TIMESTAMP + " ascending");
            //log.debug("Finding traffic entries using query: {}.", query);
            QueryResult queryResult = jcr.executeQuery(JcrQuerySpec.xpath(query.toString()).noLimit(), session);
            final NodeIterator nodeIterator = queryResult.getNodes();
            final TrafficEntriesIterator trafficEntriesIterator = new TrafficEntriesIterator(nodeIterator);
            return trafficEntriesIterator;
        } catch (RepositoryException e) {
            throw new RepositoryRuntimeException("Could not collect traffic entries.", e);
        }
    }

    /**
     * Returns the last time entries data was collected and reported for the given resolution.
     *
     * @param resolution An optional resolution to get the last collected for. When null gets the global last collected
     *                   that happens on all entries by an external time interval job.
     * @return The last collected value, null if never collected before.
     */
    public Calendar getLastCollected(TrafficCollectorResolution resolution, JcrSession session) {
        final Node logFolder = getLogFolderNode(session);
        Calendar lastCollected = null;
        try {
            String propName = JcrTypes.PROP_ARTIFACTORY_LAST_COLLECTED;
            if (resolution != null) {
                propName = JcrTypes.PROP_ARTIFACTORY_LAST_COLLECTED + resolution.getName();
            }
            if (logFolder.hasProperty(propName)) {
                lastCollected = logFolder.getProperty(propName).getDate();
            }
        } catch (RepositoryException e) {
            throw new RepositoryRuntimeException("Unexpected error while getting last collected.", e);
        }
        return lastCollected;
    }

    public void updateLastCollected(TrafficCollectorResolution resolution, Calendar lastCollected, JcrSession session) {
        final Node logFolder = getLogFolderNode(session);
        try {
            String propName = JcrTypes.PROP_ARTIFACTORY_LAST_COLLECTED;
            if (resolution != null) {
                propName = JcrTypes.PROP_ARTIFACTORY_LAST_COLLECTED + resolution.getName();
            }
            logFolder.setProperty(propName, new DateValue(lastCollected));
        } catch (RepositoryException e) {
            throw new RepositoryRuntimeException("Unexpected error while updating last collected.", e);
        }
    }

    public void addTrafficCollectorListener(TrafficCollectorResolution resolution, TrafficCollectorListener listener) {
        final TrafficCollector collector = collectorByResolution(resolution);
        collector.addListener(listener);
    }

    public void removeTrafficCollectorListener(TrafficCollectorResolution resolution,
            TrafficCollectorListener listener) {
        final TrafficCollector collector = collectorByResolution(resolution);
        collector.removeListener(listener);
    }

    public void notifyCollectionListener(TrafficCollectorListener listener, TrafficCollectionEvent event) {
        listener.onCollect(event);
    }

    /**
     * Get an iterator for traffic entries for the specified time window (edges inlusive)
     */
    private TrafficCollector collectorByResolution(TrafficCollectorResolution resolution) {
        final TrafficCollector collector = collectors.get(resolution);
        if (collector == null) {
            throw new IllegalArgumentException("Could not find a collector for '" + resolution + "' resolution.");
        }
        return collector;
    }

    private String generateEntryPath() {
        long nanos = System.nanoTime();
        if (nanos < 0) {
            //Might be negative
            nanos = -nanos;
        }
        final String entryPath = new StringBuilder(entryPathBase).append(nanos).toString();
        //Remove the leading '/'
        return entryPath.substring(1);
    }

    private Node getLogFolderNode(JcrSession session) {
        Node rootNode = session.getRootNode();
        final Node logFolder = jcr.getOrCreateUnstructuredNode(rootNode, getLogFolderPath());
        return logFolder;
    }

    private String getLogFolderPath() {
        return JcrPath.get().getLogJcrPath(LOG_FOLDER);
    }

    private InternalTrafficService me() {
        return ContextHelper.get().beanForType(InternalTrafficService.class);
    }

    private void createCollector(TrafficCollectorResolution resolution, int... fieldsToElapse) {
        collectors.put(resolution, new TrafficCollectorImpl(resolution, fieldsToElapse));
    }

    /**
     * Returns the database traffic retention start mark
     *
     * @return Retention start date
     */
    private Calendar getCleanupMark() {
        final Calendar cleanupMark = Calendar.getInstance();
        cleanupMark.add(Calendar.SECOND, -entriesRetentionSecs);
        return cleanupMark;
    }

    /**
     * @return Log dir file
     */
    private File getLogDir() {
        return ContextHelper.get().getArtifactoryHome().getLogDir();
    }

    private static class DelayedTrafficEntry extends ExpiringDelayed {
        private final TrafficEntry entry;

        private static final int SECS_TO_EXPIRY = ConstantValues.trafficCollectionIntervalSecs.getInt();

        public DelayedTrafficEntry(TrafficEntry entry) {
            super(calcExpiryTime());
            this.entry = entry;
        }

        public TrafficEntry getEntry() {
            return entry;
        }

        @Override
        public String getSubject() {
            return entry.toString();
        }

        @Override
        public String toString() {
            return getSubject();
        }

        private static long calcExpiryTime() {
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.SECOND, SECS_TO_EXPIRY);
            //Delay handling to the next round minute
            calendar.clear(Calendar.MILLISECOND);
            calendar.clear(Calendar.SECOND);
            return calendar.getTimeInMillis();
        }
    }
}