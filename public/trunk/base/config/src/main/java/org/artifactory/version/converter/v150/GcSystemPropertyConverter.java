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

package org.artifactory.version.converter.v150;

import org.artifactory.common.ConstantValues;
import org.artifactory.version.converter.XmlConverter;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * Converts the gc.interval.secs system property to a cron expression.</br> <ul> <li>If the property is at it's default
 * value: create a cron for 4AM daily.</li> <li>If the seconds amount to less than an hour: create an hourly cron.</li>
 * <li>If the seconds amount to more than an hour but less than or equal to a day: create an hourly cron for that number
 * of hours.</li> <li>If the seconds amount to more than a day: create a cron to run every number of days specified. if
 * the amount also leaves a residual number of hours, define the cron to run at that hour.</li> </ul>
 *
 * @author Noam Y. Tenne
 */
public class GcSystemPropertyConverter implements XmlConverter {

    private static final Logger log = LoggerFactory.getLogger(GcSystemPropertyConverter.class);

    public void convert(Document doc) {
        log.info("Converting garbage collector system property to a cron expression based configuration descriptor.");
        String gcCronExp;
        if (ConstantValues.gcIntervalSecs.getDefValue().equals(ConstantValues.gcIntervalSecs.getString())) {
            log.info("Garbage collector system property is set to the default value: creating default " +
                    "cron configuration for every round hour devisible by 4.");
            gcCronExp = "0 0 /4 * * ?";
        } else {
            long intervalHours = TimeUnit.SECONDS.toHours(ConstantValues.gcIntervalSecs.getLong());
            if (intervalHours < 1) {
                log.info("Garbage collector system property is set to an interval below 1 hour: creating cron " +
                        "configuration as hourly.");
                gcCronExp = "0 0 /1 * * ?";
            } else if (intervalHours >= 24) {
                long days = intervalHours / 24;
                long hourMod = intervalHours % 24;
                gcCronExp = "0 0 " + hourMod + " /" + days + " * ?";
                log.info("Garbage collector system property is set to an interval longer than a day: creating cron " +
                        "configuration run every {} day and {} hours.", days, hourMod);
            } else {
                gcCronExp = "0 0 /" + intervalHours + " * * ?";
                log.info("Garbage collector system property is set to an interval shorter than a day: creating cron " +
                        "configuration run every {} hours.", intervalHours);
            }
        }
        Element rootElement = doc.getRootElement();
        Namespace namespace = rootElement.getNamespace();

        Element gcConfigCronExpElement = new Element("cronExp", namespace);
        gcConfigCronExpElement.setText(gcCronExp);

        Element gcConfigElement = new Element("gcConfig", namespace);
        gcConfigElement.addContent(gcConfigCronExpElement);

        rootElement.addContent(gcConfigElement);

        log.info("Finished converting the garbage collector system property.");
    }
}
