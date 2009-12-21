/*
 * This file is part of Artifactory.
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

package org.artifactory.log.logback;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.util.StatusPrinter;
import org.artifactory.common.ArtifactoryHome;

/**
 * @author Yoav Landman
 */
public abstract class LogbackContextHelper {

    public static LoggerContext configure(LoggerContext lc, ArtifactoryHome home) {
        try {
            JoranConfigurator configurator = new JoranConfigurator();
            lc.stop();
            configurator.setContext(lc);
            //Set the artifactory.home so that tokens in the logback config file are extracted
            lc.putProperty(ArtifactoryHome.SYS_PROP, home.getHomeDir().getAbsolutePath());
            configurator.doConfigure(home.getLogbackConfig());
            StatusPrinter.printInCaseOfErrorsOrWarnings(lc);
        } catch (JoranException je) {
            StatusPrinter.print(lc);
        }
        return lc;
    }
}
