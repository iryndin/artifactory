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

package org.artifactory.support.core.collectors.logs;

import org.artifactory.common.ArtifactoryHome;
import org.artifactory.support.config.descriptor.ConfigDescriptorConfiguration;
import org.artifactory.support.core.collectors.AbstractSpecificContentCollector;
import org.artifactory.support.core.exceptions.BundleConfigurationException;
import org.artifactory.support.utils.DatePatternsHolder;
import org.artifactory.support.config.systemlogs.SystemLogsConfiguration;
import org.artifactory.support.core.exceptions.IllegalConditionException;
import org.artifactory.support.core.exceptions.TempDirAccessException;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;

/**
 * System logs collector
 *
 * @author Michael Pasternak
 */
@Scope(BeanDefinition.SCOPE_SINGLETON)
@Service
public class LogsCollector extends AbstractSpecificContentCollector<SystemLogsConfiguration> {

    private static final Logger log = LoggerFactory.getLogger(LogsCollector.class);

    public LogsCollector() {
        super("system-logs");
    }

    /**
     * Collects SystemLogs
     *
     * @param configuration {@link SystemLogsConfiguration}
     * @param tmpDir output directory for produced content
     *
     * @return operation result
     */
    @Override
    protected boolean doCollect(SystemLogsConfiguration configuration, File tmpDir) {
        try {
            Files.walk(getLogsDirectory().toPath(), FileVisitOption.FOLLOW_LINKS)
                    .filter (Files::isRegularFile)
                    .filter (f -> isCollectible(f, configuration))
                    .forEach(f -> copyToTempDir(f, tmpDir));
            getLog().info("Collection of " + getContentName() + " was successfully accomplished");
            return true;
        } catch (IOException | TempDirAccessException e) {
            return failure(e);
        }
    }

    /**
     * Checks whether given file is eligible for collections
     *
     * @param filePath
     * @param configuration {@link SystemLogsConfiguration}
     *
     * @return boolean
     */
    private boolean isCollectible(Path filePath, SystemLogsConfiguration configuration) {
        getLog().debug("Initiating collect eligibility check for file '{}'", filePath.getFileName());

        String fileName = filePath.getFileName().toString();

        Matcher matcher1 = DatePatternsHolder.getMatcher1(fileName);
        Matcher matcher2 = DatePatternsHolder.getMatcher2(fileName);
        Matcher matcher3 = DatePatternsHolder.getMatcher3(fileName);
        Matcher matcher4 = DatePatternsHolder.getMatcher4(fileName);
        Matcher matcherDateZip = DatePatternsHolder.getDateZipPattern(fileName);


        if (matcher1.find()) {
            getLog().debug("File '{}' matches pattern 1", filePath.getFileName());
            String date = matcher1.group(0);
            return isDateInRequestedRange(date, configuration,
                    DatePatternsHolder.DatePattern.PATTERN_1);
        } else if (matcher2.find()) {
            getLog().debug("File '{}' matches pattern 2", filePath.getFileName());
            String date = matcher2.group(0);
            return isDateInRequestedRange(date, configuration,
                    DatePatternsHolder.DatePattern.PATTERN_2);
        } else if (matcher3.find()) {
            getLog().debug("File '{}' matches pattern 3", filePath.getFileName());
            String date = matcher3.group(0);
            return isDateInRequestedRange(date, configuration,
                    DatePatternsHolder.DatePattern.PATTERN_3);
        } else if (matcher4.find()) {
            getLog().debug("File '{}' matches pattern 4", filePath.getFileName());
            String date = matcher4.group(0);
            return isDateInRequestedRange(date, configuration,
                    DatePatternsHolder.DatePattern.PATTERN_4);
        } else if(matcherDateZip.find()) {
            getLog().debug("File '{}' matches pattern log.zip", filePath.getFileName());
            return isDateInRequestedZipRange(filePath, configuration, DatePatternsHolder.DatePattern.PATTERN_ZIP_LOG);
        }

        getLog().debug("File '{}' doesn't match any known date pattern, " +
                "using default fallback (true)", fileName);
        return true;
    }

    /**
     * Checks zipped artifactory.log based on 'created' attribute
     * to avoid attaching heavy legacy zipped logs
     *
     * @param filePath
     * @param configuration
     *
     * @return should be collected or not
     */
    private boolean isDateInRequestedZipRange(Path filePath, SystemLogsConfiguration configuration,
            DatePatternsHolder.DatePattern datePattern) {

        Date start, end;

        if(configuration.getDaysCount() != null) {
            int dayCount = configuration.getDaysCount().intValue();
            end = new Date();
            start = new DateTime(end).minusDays(dayCount).toDate();
        } else {
            start = configuration.getStartDate();
            end = configuration.getEndDate();
        }

        try {
            BasicFileAttributes attr = Files.readAttributes(filePath, BasicFileAttributes.class);
            if (attr != null) {
                FileTime time = attr.creationTime();
                if (time != null) {
                    if(time.compareTo(FileTime.fromMillis(start.getTime())) >= 0 &&
                            time.compareTo(FileTime.fromMillis(end.getTime())) <= 0) {
                        return true;
                    }
                    return false;
                } else {
                    getLog().debug("Created attribute for '{}' was not located", filePath);
                }
            } else {
                getLog().debug("No attributes for '{}' were located", filePath);
            }
        } catch (IOException e) {
            getLog().warn("Reading '{}' attributes has failed: {}", filePath, e.getMessage());
            getLog().debug("Cause: {}", e);
        }
        return true;
    }

    /**
     * Checks whether log file date falls into {@link SystemLogsConfiguration}
     *
     * @param date log file date
     * @param configuration {@link SystemLogsConfiguration}
     * @param datePattern {@link DatePatternsHolder.DatePattern} to work with
     *
     * @return boolean
     */
    private boolean isDateInRequestedRange(String date,
            SystemLogsConfiguration configuration, DatePatternsHolder.DatePattern datePattern) {
        getLog().debug("Initiating range check for date '{}'", date);

        Date logFileDate = null;

        for(String patternTemplate : datePattern.getPatternTemplates() ) {
            DateFormat df = new SimpleDateFormat(patternTemplate);
            try {
                logFileDate = df.parse(date);
                getLog().debug("Date '{}' matches template '{}'", date, patternTemplate);
                break;
            } catch (ParseException e) {
                getLog().debug("Cannot parse date '{}' with template '{}'", date, patternTemplate);
            }
        }

        if (logFileDate == null) {
            getLog().debug(
                    "Could not parse date '{}' using any known template for pattern '{}', " +
                            "assuming positive answer",
                    date, datePattern.getPattern()
            );
            return true;
        }

        if (configuration.getDaysCount() != null) { // days offset
            DateTime dateTime = new DateTime(logFileDate);
            if(DateTime.now().minusDays(configuration.getDaysCount()
                    .intValue()).compareTo(dateTime) <= 0)
                return true;
            return false;
        } else if (configuration.getStartDate() != null) { // date range
            Date endDate = configuration.getEndDate() != null ?
                    configuration.getEndDate() : new Date();
            if(configuration.getStartDate().compareTo(logFileDate) <= 0 &&
                    endDate.compareTo(logFileDate) >=0) {
                return true;
            }
            return false;
        } else {
            throw new IllegalConditionException(
                    "Either days offset or StartDate must be specified"
            );
        }
    }

    /**
     * @return artifactory logs folder location
     */
    private File getLogsDirectory() {
        return ArtifactoryHome.get().getLogDir();
    }

    @Override
    public Logger getLog() {
        return log;
    }

    /**
     * Makes sure configuration is valid
     *
     * @param configuration configuration to check
     * @throws org.artifactory.support.core.exceptions.BundleConfigurationException
     *         if configuration is invalid
     */
    @Override
    protected void doEnsureConfiguration(SystemLogsConfiguration configuration)
            throws BundleConfigurationException {
        if (configuration.getDaysCount() != null) {
            ensureDateRange(
                    configuration.getDaysCount()
            );
        } else if (configuration.getStartDate() != null &&
            configuration.getEndDate() != null) {
                ensureDateRange(
                configuration.getStartDate(),
                configuration.getEndDate()
            );
        } else {
            throw new BundleConfigurationException(
                    "SystemLogsConfiguration is incomplete, either DaysCount or StartDate+EndDate must be specified"
            );
        }
    }

    /**
     * Makes sure date range is legal
     *
     * @param startDate
     * @param endDate
     *
     * @throws BundleConfigurationException thrown when illegal configuration is found
     */
    private void ensureDateRange(Date startDate, Date endDate)
            throws BundleConfigurationException {
        if(startDate == null || endDate == null)
            throw new BundleConfigurationException(
                    "Date range is illegal, " +
                            "startDate/endDate cannot be empty"
            );

        if(startDate.getTime() > endDate.getTime())
            throw new BundleConfigurationException(
                    "Date range is illegal, " +
                            "startDate cannot be greater than endDate"
            );
    }

    /**
     * Makes sure date range is legal
     *
     * @param daysCount
     *
     * @throws BundleConfigurationException thrown when illegal configuration is found
     */
    private void ensureDateRange(Integer daysCount)
            throws BundleConfigurationException {
        if (daysCount == null)
            throw new BundleConfigurationException(
                    "Date range is illegal, " +
                            "daysCount cannot be empty"
            );

        if (daysCount.intValue() <= 0)
            throw new BundleConfigurationException(
                    "Date range is illegal, " +
                            "daysCount cannot be negative number or zero"
            );
    }

}
