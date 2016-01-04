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
 */

package org.artifactory.traffic;

import com.google.common.collect.Lists;
import org.artifactory.addon.AddonsManager;
import org.artifactory.addon.ha.HaCommonAddon;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.common.ConstantValues;
import org.artifactory.descriptor.config.CentralConfigDescriptor;
import org.artifactory.mbean.MBeanRegistrationService;
import org.artifactory.repo.service.InternalRepositoryService;
import org.artifactory.schedule.TaskService;
import org.artifactory.spring.InternalArtifactoryContext;
import org.artifactory.spring.InternalContextHelper;
import org.artifactory.spring.Reloadable;
import org.artifactory.spring.ReloadableBean;
import org.artifactory.storage.db.servers.model.ArtifactoryServer;
import org.artifactory.storage.db.servers.service.ArtifactoryServersCommonService;
import org.artifactory.traffic.entry.TrafficEntry;
import org.artifactory.traffic.entry.TransferEntry;
import org.artifactory.traffic.mbean.Traffic;
import org.artifactory.traffic.read.TrafficReader;
import org.artifactory.version.CompoundVersionDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.Calendar;
import java.util.List;

/**
 * Traffic service persists the traffic (download/upload) in Artifactory and can retrieve it by date range.
 *
 * @author Yoav Landman
 */
@Service
@Reloadable(beanClass = InternalTrafficService.class, initAfter = {InternalRepositoryService.class, TaskService.class})
public class TrafficServiceImpl implements InternalTrafficService, ReloadableBean {

    private boolean active;

    @Autowired
    private ArtifactoryServersCommonService serversService;

    @Override
    public void init() {
        //Register a mbean
        InternalArtifactoryContext context = InternalContextHelper.get();
        Traffic traffic = new Traffic(context.beanForType(InternalTrafficService.class));
        ContextHelper.get().beanForType(MBeanRegistrationService.class).register(traffic);

        active = ConstantValues.trafficCollectionActive.getBoolean();

    }

    @Override
    public void reload(CentralConfigDescriptor oldDescriptor) {
        //nop
    }

    @Override
    public void destroy() {
        //nop
    }

    @Override
    public void convert(CompoundVersionDetails source, CompoundVersionDetails target) {
        //When conversion is needed, remove all old stats
    }

    @Override
    public void handleTrafficEntry(TrafficEntry entry) {
        if (active) {
            if (entry instanceof TransferEntry) {
                TrafficLogger.logTransferEntry((TransferEntry) entry);
            }
        }
    }

    public void validateDateRange(Calendar startDate, Calendar endDate) {
        if (startDate.after(endDate)) {
            throw new IllegalArgumentException("The start date cannot be later than the end date");
        }
    }

    /**
     * Returns traffic entries
     *
     * @param from Traffic start time
     * @param to   Traffic end time
     * @return List<TrafficEntry> taken from the traffic log files or the database
     */
    @Override
    public List<TrafficEntry> getEntryList(Calendar from, Calendar to) {
        List<TrafficEntry> logFileEntries = getLogFileEntries(from, to);
        return logFileEntries;
    }

    /**
     * Returns traffic entries from the traffic log files
     *
     * @param from Traffic start time
     * @param to   Traffic end time
     * @return List<TrafficEntry> taken from the traffic log files
     */
    public List<TrafficEntry> getLogFileEntries(Calendar from, Calendar to) {
        File logDir = ContextHelper.get().getArtifactoryHome().getLogDir();
        TrafficReader trafficReader = new TrafficReader(logDir);
        List<TrafficEntry> logFileEntries = trafficReader.getEntries(from, to);
        return logFileEntries;
    }

    /**
     * Returns transfer usage
     *
     * @param startTime  Traffic start time in long
     * @param endTime    Traffic end time in long
     * @param ipToFilter filter the traffic by list of ip
     * @return TransferUsage taken from the traffic log files or the database
     */
    @Override
    public TransferUsage getTrafficUsageWithFilterCurrentNode(long startTime, long endTime,
            List<String> ipToFilter) {
        Calendar from = Calendar.getInstance();
        from.setTimeInMillis(startTime);
        Calendar to = Calendar.getInstance();
        to.setTimeInMillis(endTime);
        validateDateRange(from, to);
        List<TrafficEntry> allTrafficEntry = getEntryList(from, to);
        TransferUsage transferUsage = orderEntriesByFilter(allTrafficEntry, ipToFilter);
        return transferUsage;
    }

    @Override
    public boolean isActive() {
        return active;
    }

    @Override
    public TransferUsage getTrafficUsageWithFilter(long startTime, long endTime, List<String> ipsToFilter) {
        List<TransferUsage> transferUsageList = Lists.newArrayList();
        TransferUsage currentNodeUsage = getTrafficUsageWithFilterCurrentNode(startTime, endTime, ipsToFilter);
        transferUsageList.add(currentNodeUsage);

        HaCommonAddon haAddon = ContextHelper.get().beanForType(AddonsManager.class).addonByType(HaCommonAddon.class);
        if (haAddon.isHaEnabled() && haAddon.isHaConfigured()) {
            List<ArtifactoryServer> allOtherActiveHaServers = serversService.getOtherActiveMembers();
            List<TransferUsage> responseList = (List<TransferUsage>) haAddon.propagateTrafficCollector(startTime, endTime, ipsToFilter,
                    allOtherActiveHaServers, TransferUsage.class);
            if (responseList == null) {
               return null;
            }
            transferUsageList.addAll(responseList);
        }
        return aggregateUsage(transferUsageList);
    }

    private TransferUsage aggregateUsage(List<TransferUsage> transferUsageList) {
        TransferUsage finalTransferUsage = new TransferUsage();
        for (TransferUsage transferUsage : transferUsageList) {
            finalTransferUsage.setDownload(finalTransferUsage.getDownload() + transferUsage.getDownload());
            finalTransferUsage.setUpload(finalTransferUsage.getUpload() + transferUsage.getUpload());
            finalTransferUsage.setExcludedDownload(
                    finalTransferUsage.getExcludedDownload() + transferUsage.getExcludedDownload());
            finalTransferUsage.setExcludedUpload(
                    finalTransferUsage.getExcludedUpload() + transferUsage.getExcludedUpload());
        }
        return finalTransferUsage;
    }

    private TransferUsage orderEntriesByFilter(List<TrafficEntry> allTrafficEntry,
            List<String> ipToFilter) {
        TransferUsage transferUsage = new TransferUsage();
        List<TrafficEntry> trafficEntriesExcludedUsage = Lists.newArrayList();
        List<TrafficEntry> trafficEntriesUsage = Lists.newArrayList();
        if (ipToFilter == null || ipToFilter.isEmpty()) {
            trafficEntriesUsage.addAll(allTrafficEntry);
        } else {
            for (TrafficEntry trafficEntry : allTrafficEntry) {
                if (isExcludedTraffic(trafficEntry, ipToFilter)) {
                    trafficEntriesExcludedUsage.add(trafficEntry);
                } else {
                    trafficEntriesUsage.add(trafficEntry);
                }
            }

        }
        fillUsage(trafficEntriesExcludedUsage, transferUsage, true);
        fillUsage(trafficEntriesUsage, transferUsage, false);
        return transferUsage;
    }

    private void fillUsage(List<TrafficEntry> trafficEntriesUsage, TransferUsage transferUsage,
            boolean isExcludedUsage) {
        long uploadTransferUsage = 0;
        long downloadTransferUsage = 0;

        for (TrafficEntry trafficEntry : trafficEntriesUsage) {
            long contentLength = ((TransferEntry) trafficEntry).getContentLength();
            if (trafficEntry.getAction() == TrafficAction.UPLOAD) {
                uploadTransferUsage += contentLength;
            } else {
                downloadTransferUsage += contentLength;
            }
        }
        if (isExcludedUsage) {
            transferUsage.setExcludedUpload(uploadTransferUsage);
            transferUsage.setExcludedDownload(downloadTransferUsage);
        } else {
            transferUsage.setUpload(uploadTransferUsage);
            transferUsage.setDownload(downloadTransferUsage);
        }
    }

    private boolean isExcludedTraffic(TrafficEntry trafficEntry, List<String> ipToFilter) {
        TransferEntry transferEntry = ((TransferEntry) trafficEntry);
        for (String ipAddress : ipToFilter) {
            if (transferEntry.getUserAddress().equals(ipAddress)) {
                return true;
            }
        }
        return false;
    }
}