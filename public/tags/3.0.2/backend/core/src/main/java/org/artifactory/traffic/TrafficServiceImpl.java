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
import org.artifactory.traffic.entry.TrafficEntry;
import org.artifactory.traffic.entry.TransferEntry;
import org.artifactory.traffic.mbean.Traffic;
import org.artifactory.traffic.read.TrafficReader;
import org.artifactory.version.CompoundVersionDetails;
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
}