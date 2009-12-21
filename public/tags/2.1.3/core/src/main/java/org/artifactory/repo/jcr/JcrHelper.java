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

package org.artifactory.repo.jcr;

import static org.apache.jackrabbit.JcrConstants.*;
import org.artifactory.api.repo.exception.RepositoryRuntimeException;
import org.artifactory.jcr.fs.JcrFile;
import org.artifactory.jcr.fs.JcrFolder;
import org.artifactory.log.LoggerFactory;
import org.artifactory.util.PathUtils;
import org.slf4j.Logger;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import java.util.Calendar;

/**
 * @author freds
 * @date Jan 12, 2009
 */
public class JcrHelper {
    private static final Logger log = LoggerFactory.getLogger(JcrHelper.class);

    public static final String PROP_ARTIFACTORY_NAME = "artifactory:name";
    public static final String NT_UNSTRUCTURED = "nt:unstructured";

    public static String getAbsolutePath(Node node) {
        try {
            return PathUtils.trimTrailingSlashes(node.getPath());
        } catch (RepositoryException e) {
            throw new RepositoryRuntimeException("Failed to retrieve node's absolute path:" + node, e);
        }
    }

    private static String display(Node node) {
        if (node == null) {
            return "NULL node";
        }
        try {
            return node.getPath();
        } catch (RepositoryException ignore) {
            return node.toString();
        }
    }

    public static String getUuid(Node node) {
        try {
            return node.getUUID();
        } catch (RepositoryException e) {
            throw new RepositoryRuntimeException("Failed to retrieve node's uuid:" + display(node), e);
        }
    }

    public static long getJcrCreated(Node node) {
        try {
            //This property is auto-populated on node creation
            if (node.hasProperty(JCR_CREATED)) {
                return node.getProperty(JCR_CREATED).getDate().getTimeInMillis();
            }
        } catch (RepositoryException e) {
            throw new RepositoryRuntimeException("Failed to retrieve node's created date:" + display(node), e);
        }
        return 0;
    }

    public static Node getResourceNode(Node node) {
        try {
            Node resNode = node.getNode(JCR_CONTENT);
            return resNode;
        } catch (RepositoryException e) {
            throw new RepositoryRuntimeException("Failed to get resource node for " + display(node), e);
        }
    }

    public static long getJcrLastModified(Node node) {
        try {
            Node propFrom = getNodeForLastModified(node);
            //This property is auto-populated on node modification
            if (propFrom.hasProperty(JCR_LASTMODIFIED)) {
                return propFrom.getProperty(JCR_LASTMODIFIED).getDate().getTimeInMillis();
            }
        } catch (RepositoryException e) {
            throw new RepositoryRuntimeException("Failed to get last modified date for " + display(node), e);
        }
        return 0;
    }

    public static boolean isFsItemType(Node node) {
        String typeName = JcrHelper.getPrimaryTypeName(node);
        return (JcrFolder.NT_ARTIFACTORY_FOLDER.equals(typeName) ||
                JcrFile.NT_ARTIFACTORY_FILE.equals(typeName));
    }

    private static Node getNodeForLastModified(Node node) throws RepositoryException {
        Node propFrom;
        String typeName = JcrHelper.getPrimaryTypeName(node);
        if (typeName.equals(JcrFolder.NT_ARTIFACTORY_FOLDER)) {
            propFrom = node;
        } else if (typeName.equals(JcrFile.NT_ARTIFACTORY_FILE)) {
            propFrom = node.getNode(JCR_CONTENT);
        } else if (typeName.equals(NT_RESOURCE)) {
            propFrom = node;
        } else {
            throw new RepositoryRuntimeException(
                    "Cannot get last modified date from unknow Node type " + typeName + " for node " +
                            display(node));
        }
        return propFrom;
    }

    /**
     * Should not be needed
     *
     * @param node
     * @param name
     */
    @Deprecated
    public static void checkArtifactoryName(Node node, String name) {
        try {
            //Set the name property for indexing and speedy searches
            if (node.hasProperty(PROP_ARTIFACTORY_NAME)) {
                String artifactoryName = node.getProperty(PROP_ARTIFACTORY_NAME).getString();
                if (!artifactoryName.equals(name)) {
                    // TODO: Send an Asynch message to resave this
                    log.error("Item " + display(node) + " does not have a valid name " + artifactoryName);
                }
            } else {
                // TODO: Send an Asynch message to resave this
                log.error("Item " + display(node) + " does not have a name");
            }
        } catch (RepositoryException e) {
            throw new RepositoryRuntimeException("Failed to get Artifactory property for " + display(node), e);
        }
    }

    public static long getLength(Node resNode) {
        try {
            return resNode.getProperty(org.apache.jackrabbit.JcrConstants.JCR_DATA).getLength();
        } catch (RepositoryException e) {
            throw new RepositoryRuntimeException("Failed to retrieve file node's size for " + display(resNode), e);
        }
    }

    public static String getMimeType(Node resNode) {
        try {
            return resNode.getProperty(org.apache.jackrabbit.JcrConstants.JCR_MIMETYPE).getString();
        } catch (RepositoryException e) {
            throw new RepositoryRuntimeException("Failed to retrieve file node's mime type for " + display(resNode), e);
        }
    }

    public static String getPrimaryTypeName(Node node) {
        try {
            return node.getPrimaryNodeType().getName();
        } catch (RepositoryException e) {
            throw new RepositoryRuntimeException(
                    "Failed to get the primary type for the node at '" + display(node) + "'.", e);
        }
    }

    public static boolean setLastModified(Node node, long time) {
        Calendar lastModifiedCalendar = Calendar.getInstance();
        lastModifiedCalendar.setTimeInMillis(time);
        try {
            Node propFrom = getNodeForLastModified(node);
            propFrom.setProperty(JCR_LASTMODIFIED, lastModifiedCalendar);
        } catch (RepositoryException e) {
            throw new RepositoryRuntimeException("Failed to set file node " + display(node) + " last modified time.",
                    e);
        }
        return true;
    }

    public static void setArtifactoryName(Node node, String name) {
        try {
            node.setProperty(JcrHelper.PROP_ARTIFACTORY_NAME, name);
        } catch (RepositoryException e) {
            throw new RepositoryRuntimeException("Failed to set artifactory name on node " + display(node), e);
        }
    }

    public static void setMimeType(Node node, String mimeType) {
        try {
            Node resourceNode = getResourceNode(node);
            resourceNode.setProperty(JCR_MIMETYPE, mimeType);
        } catch (RepositoryException e) {
            throw new RepositoryRuntimeException("Failed to set mime type on node " + display(node), e);
        }
    }
}
