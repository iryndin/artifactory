/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2010 JFrog Ltd.
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

package org.artifactory.jcr.version.v211;

import org.apache.commons.lang.StringUtils;
import org.apache.jackrabbit.util.Text;
import org.artifactory.api.build.BuildService;
import org.artifactory.api.repo.exception.RepositoryRuntimeException;
import org.artifactory.jcr.JcrPath;
import org.artifactory.jcr.JcrSession;
import org.artifactory.version.converter.ConfigurationConverter;
import org.jfrog.build.api.Build;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * Adds latest build name and start time properties to each build name node
 *
 * @author Noam Y. Tenne
 */
public class LatestBuildPropertyConverter implements ConfigurationConverter<JcrSession> {

    @SuppressWarnings({"UnusedDeclaration"})
    private static final Logger log = LoggerFactory.getLogger(LatestBuildPropertyConverter.class);

    public void convert(JcrSession config) {
        log.info("Starting latest build property conversion.");
        try {
            addLatestBuildTimeProperty(config);
        } catch (RepositoryException re) {
            throw new RepositoryRuntimeException("Error occurred while adding latest build properties.", re);
        }
        log.info("Finished latest build property conversion.");
        config.save();
    }

    /**
     * Adds the latest build time property to all the build nodes of the given session
     *
     * @param config Session to convert build nodes for
     */
    private void addLatestBuildTimeProperty(JcrSession config) throws RepositoryException {
        if (!config.nodeExists(JcrPath.get().getBuildsJcrRootPath())) {
            log.info("Skipping latest build property conversion: no builds exist.");
            return;
        }
        Node buildsRootNode = config.getNode(JcrPath.get().getBuildsJcrRootPath());

        NodeIterator buildNameNodes = buildsRootNode.getNodes();
        while (buildNameNodes.hasNext()) {
            Node buildNameNode = buildNameNodes.nextNode();
            log.debug("Adding latest build properties to: " + buildNameNode.getPath());
            setLatestBuildPropertiesForName(buildNameNode);
        }
    }

    /**
     * Sets latest build number and started properties on the given build name level node
     *
     * @param buildNameNode Build name level node
     */
    private void setLatestBuildPropertiesForName(Node buildNameNode) throws RepositoryException {
        String latestNumber = null;
        Calendar latestStart = null;

        NodeIterator buildNumberNodes = buildNameNode.getNodes();
        while (buildNumberNodes.hasNext()) {
            Node buildNumberNode = buildNumberNodes.nextNode();

            NodeIterator buildStartedNodes = buildNumberNode.getNodes();
            while (buildStartedNodes.hasNext()) {

                Node buildStartedNode = buildStartedNodes.nextNode();

                String buildStarted = Text.unescapeIllegalJcrChars(buildStartedNode.getName());
                Calendar currentBuildTime = Calendar.getInstance();
                try {
                    currentBuildTime.setTime(new SimpleDateFormat(Build.STARTED_FORMAT).parse(buildStarted));
                    if ((latestStart == null) || latestStart.before(currentBuildTime)) {
                        latestNumber = Text.unescapeIllegalJcrChars(buildNumberNode.getName());
                        latestStart = currentBuildTime;
                    }
                } catch (ParseException e) {
                    log.error("'" + buildStartedNode.getPath() + "' Was excluded from latest build property " +
                            "conversion process: unable to parse build start time.", e);
                }
            }
        }

        if (StringUtils.isNotBlank(latestNumber) && latestStart != null) {
            buildNameNode.setProperty(BuildService.PROP_BUILD_LATEST_NUMBER, latestNumber);
            buildNameNode.setProperty(BuildService.PROP_BUILD_LATEST_START_TIME, latestStart);
        }
    }
}