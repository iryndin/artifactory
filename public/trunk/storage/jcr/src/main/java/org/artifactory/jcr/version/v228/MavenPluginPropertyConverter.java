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

package org.artifactory.jcr.version.v228;

import org.apache.commons.io.IOUtils;
import org.apache.jackrabbit.JcrConstants;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.io.checksum.BinaryNodeListener;
import org.artifactory.io.checksum.JcrChecksumPaths;
import org.artifactory.log.LoggerFactory;
import org.artifactory.maven.MavenModelUtils;
import org.artifactory.mime.MavenNaming;
import org.artifactory.storage.StorageConstants;
import org.artifactory.version.converter.ConfigurationConverter;
import org.slf4j.Logger;

import javax.jcr.Binary;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * This converter adds maven plugin JCR property to all file nodes of maven plugin poms. To save node traversal it is
 * done as a listener of {@link org.artifactory.io.checksum.ChecksumPathsImpl}.
 *
 * @author Yossi Shaul
 */
public class MavenPluginPropertyConverter implements BinaryNodeListener, ConfigurationConverter<Session> {
    private static final Logger log = LoggerFactory.getLogger(MavenPluginPropertyConverter.class);

    /**
     * Just register the listener to convert the nodes during checksum paths scanning
     */
    @Override
    public void convert(Session config) {
        JcrChecksumPaths jcrChecksumPaths = ContextHelper.get().beanForType(JcrChecksumPaths.class);
        if (jcrChecksumPaths != null) {
            jcrChecksumPaths.setBinaryNodeListener(this);
        } else {
            log.error("Couldn't find checksums path bean. Converter will not execute.");
        }
    }

    @Override
    public void nodeVisited(Node node, boolean fixConsistency) {
        // only perform action when initializing the table in the first time (fixConsistency = false)
        if (fixConsistency) {
            return;
        }
        InputStream binaryInputStream = null;
        String nodePath = "Unknown";
        try {
            nodePath = node.getPath();
            log.trace("Scanning node '{}'", nodePath);

            // first check if it's a binary node of artifactory pom file
            Node parent = node.getParent();
            if (!isBinaryOfPomFile(parent)) {
                log.trace("Node '{}' is not of pom file", nodePath);
                return;
            }
            if (!node.hasProperty(JcrConstants.JCR_DATA)) {
                log.trace("Node '{}' is a binary node", nodePath);
                return;
            }

            Binary binary = node.getProperty(JcrConstants.JCR_DATA).getBinary();
            binaryInputStream = binary.getStream();
            if (isMavenPluginPom(binaryInputStream)) {
                log.debug("Marking '{}' as maven plugin", parent.getPath());
                parent.setProperty(StorageConstants.PROP_ARTIFACTORY_MAVEN_PLUGIN, true);
                parent.save();
            }
        } catch (Exception e) {
            log.error("Failed to set maven plugin property on '{}': {}", nodePath, e.getMessage());
        } finally {
            IOUtils.closeQuietly(binaryInputStream);
        }
    }

    private boolean isMavenPluginPom(InputStream in) {
        try {
            MavenXpp3Reader reader = new MavenXpp3Reader();
            Model model = reader.read(new InputStreamReader(in, MavenModelUtils.UTF8));
            return "maven-plugin".equals(model.getPackaging());
        } catch (Exception e) {
            log.warn("Failed to parse pom stream: " + e.getMessage());
            return false;
        }
    }

    private boolean isBinaryOfPomFile(Node parent) throws RepositoryException {
        if (parent != null) {
            String path = parent.getPath();
            return MavenNaming.isPom(path);
        }
        return false;
    }
}
