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

package org.artifactory.support.config.systemlogs;

import org.artifactory.support.config.CollectConfiguration;
import org.artifactory.support.core.exceptions.BundleConfigurationException;
import org.joda.time.DateTime;

import java.util.Date;

/**
 * Provides configuration for system logs collection
 *
 * @author Michael Pasternak
 */
public class SystemLogsConfiguration extends CollectConfiguration {

    private Date startDate; // TODO: consider using ZonedDateTime (if needed)
    private Date endDate;   // TODO: consider using ZonedDateTime (if needed)
    private Integer daysCount;

    /**
     * we enable all collectors by default (see in #RTFACT-8106),
     * if no date range is specified, we'll collect logs for last
     * day only
     */
    public SystemLogsConfiguration() {
        endDate = new Date();
        startDate = new DateTime(endDate).minusDays(1).toDate();
    }

    /**
     * @param enabled whether SystemLogs configuration is enabled
     * @param startDate collect logs from
     * @param endDate collect logs to
     */
    public SystemLogsConfiguration(boolean enabled, Date startDate, Date endDate) {
        super(enabled);

        this.startDate = startDate;
        this.endDate = endDate;
        this.daysCount = null;
    }

    /**
     * @param enabled whether SystemLogs configuration is enabled
     * @param daysCount amount of days eligible for logs collection
     */
    public SystemLogsConfiguration(boolean enabled, Integer daysCount) {
        super(enabled);

        this.daysCount = daysCount;
        this.startDate = null;
        this.endDate = null;
    }

    /**
     * @return StartDate
     */
    public Date getStartDate() {
        return startDate;
    }

    /**
     * @return EndDate
     */
    public Date getEndDate() {
        return endDate;
    }

    /**
     * @return DaysCount
     */
    public Integer getDaysCount() {
        return daysCount;
    }
}
