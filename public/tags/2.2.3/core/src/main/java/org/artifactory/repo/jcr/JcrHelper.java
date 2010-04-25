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

package org.artifactory.repo.jcr;

import org.apache.jackrabbit.spi.commons.conversion.MalformedPathException;
import org.artifactory.api.repo.exception.RepositoryRuntimeException;
import org.artifactory.jcr.JcrSession;
import org.artifactory.jcr.JcrTypes;
import org.artifactory.log.LoggerFactory;
import org.artifactory.util.ExceptionUtils;
import org.artifactory.util.PathUtils;
import org.slf4j.Logger;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import java.io.InputStream;
import java.util.Calendar;

import static org.apache.jackrabbit.JcrConstants.*;

/**
 * @author freds
 * @date Jan 12, 2009
 */
public class JcrHelper {
    private static final Logger log = LoggerFactory.getLogger(JcrHelper.class);

    public static String getAbsolutePath(Node node) {
        try {
            return PathUtils.trimTrailingSlashes(node.getPath());
        } catch (RepositoryException e) {
            throw new RepositoryRuntimeException("Failed to retrieve node's absolute path:" + node, e);
        }
    }

    public static String display(Node node) {
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
        if (node == null) {
            return false;
        }
        String typeName = JcrHelper.getPrimaryTypeName(node);
        return (JcrTypes.NT_ARTIFACTORY_FOLDER.equals(typeName) ||
                JcrTypes.NT_ARTIFACTORY_FILE.equals(typeName));
    }

    public static boolean isFile(Node node) {
        if (node == null) {
            return false;
        }
        String typeName = JcrHelper.getPrimaryTypeName(node);
        return (JcrTypes.NT_ARTIFACTORY_FILE.equals(typeName));
    }

    public static boolean isFolder(Node node) {
        if (node == null) {
            return false;
        }
        String typeName = JcrHelper.getPrimaryTypeName(node);
        return (JcrTypes.NT_ARTIFACTORY_FOLDER.equals(typeName));
    }

    public static boolean isMetadata(Node node) {
        if (node == null) {
            return false;
        }
        String typeName = JcrHelper.getPrimaryTypeName(node);
        return (JcrTypes.NT_ARTIFACTORY_METADATA.equals(typeName));
    }

    private static Node getNodeForLastModified(Node node) throws RepositoryException {
        Node propFrom;
        String typeName = JcrHelper.getPrimaryTypeName(node);
        if (typeName.equals(JcrTypes.NT_ARTIFACTORY_FOLDER)) {
            propFrom = node;
        } else if (typeName.equals(JcrTypes.NT_ARTIFACTORY_FILE)) {
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
     * Check artifactory:name validity
     *
     * @param node
     * @param name
     */
    public static void checkArtifactoryName(Node node, String name) {
        try {
            //Set the name property for indexing and speedy searches
            if (node.hasProperty(JcrTypes.PROP_ARTIFACTORY_NAME)) {
                String artifactoryName = node.getProperty(JcrTypes.PROP_ARTIFACTORY_NAME).getString();
                if (!artifactoryName.equals(name)) {
                    log.error("Item " + display(node) + " does not have a valid name " + artifactoryName);
                }
            } else {
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

    public static boolean setJcrLastModified(Node node, long time) {
        Calendar lastModifiedCalendar = getCalendar(time);
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
            node.setProperty(JcrTypes.PROP_ARTIFACTORY_NAME, name);
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

    public static String getStringProperty(Node node, String propName) {
        return getStringProperty(node, propName, null, true);
    }

    public static String getStringProperty(Node node, String propName, String defaultValue, boolean allowNull) {
        try {
            if (node.hasProperty(propName)) {
                return node.getProperty(propName).getString();
            } else {
                if (!allowNull && defaultValue == null) {
                    throw new RepositoryRuntimeException(
                            "Property " + propName + " does not exists for " + display(node));
                }
                return defaultValue;
            }
        } catch (RepositoryException e) {
            throw new RepositoryRuntimeException(
                    "Failed to retrieve node's property " + propName + "  for " + display(node), e);
        }
    }

    public static Long getLongProperty(Node node, String propName) {
        return getLongProperty(node, propName, null, true);
    }

    public static Long getLongProperty(Node node, String propName, Long defaultValue, boolean allowNull) {
        try {
            if (node.hasProperty(propName)) {
                return node.getProperty(propName).getLong();
            } else {
                if (!allowNull && defaultValue == null) {
                    throw new RepositoryRuntimeException(
                            "Property " + propName + " does not exists for " + display(node));
                }
                return defaultValue;
            }
        } catch (RepositoryException e) {
            throw new RepositoryRuntimeException(
                    "Failed to retrieve node's property " + propName + "  for " + display(node), e);
        }
    }

    public static void setStringProperty(Node node, String propName, String value) {
        try {
            if (value == null) {
                if (node.hasProperty(propName)) {
                    node.getProperty(propName).remove();
                }
            } else {
                node.setProperty(propName, value);
            }
        } catch (RepositoryException e) {
            throw new RepositoryRuntimeException(
                    "Failed to set node's property " + propName + "  for " + display(node), e);
        }
    }

    public static void setLongProperty(Node node, String propName, Long value) {
        try {
            if (value == null) {
                if (node.hasProperty(propName)) {
                    node.getProperty(propName).remove();
                }
            } else {
                node.setProperty(propName, value);
            }
        } catch (RepositoryException e) {
            throw new RepositoryRuntimeException(
                    "Failed to set node's property " + propName + "  for " + display(node), e);
        }
    }

    public static void setDateProperty(Node node, String propName, Long value) {
        try {
            if (value == null) {
                if (node.hasProperty(propName)) {
                    node.getProperty(propName).remove();
                }
            } else {
                Calendar calendar = getCalendar(value);
                node.setProperty(propName, calendar);
            }
        } catch (RepositoryException e) {
            throw new RepositoryRuntimeException(
                    "Failed to set node's property " + propName + "  for " + display(node), e);
        }
    }

    public static Calendar getCalendar(Long value) {
        if (value == null) {
            return null;
        }
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(value);
        return calendar;
    }

    public static InputStream getRawStringStream(Node metadataNode) throws RepositoryException {
        Node stringNode = metadataNode.getNode(JCR_CONTENT);
        Property property = stringNode.getProperty(JCR_DATA);
        log.trace("Read string data '{}' with length: {}.", stringNode.getPath(), property.getLength());
        Value attachedDataValue = property.getValue();
        InputStream is = attachedDataValue.getStream();
        return is;
    }

    public static Node getNode(Node parent, String relPath) {
        try {
            String cleanPath = relPath.startsWith("/") ? relPath.substring(1) : relPath;
            if (!parent.hasNode(cleanPath)) {
                return null;
            }

            return parent.getNode(cleanPath);
        } catch (RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    public static Node getOrCreateNode(Node parent, String relPath, String type, String... mixins) {
        try {
            String cleanPath = relPath.startsWith("/") ? relPath.substring(1) : relPath;
            Node node;
            if (!parent.hasNode(cleanPath)) {
                node = parent.addNode(cleanPath, type);
                log.debug("Created node: {}.", relPath);
                for (String mixin : mixins) {
                    node.addMixin(mixin);
                }
            } else {
                node = parent.getNode(cleanPath);
            }
            return node;
        } catch (RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    public static Node safeGetNode(Node parentNode, String... nodeNames) {
        if (parentNode == null || nodeNames == null || nodeNames.length == 0) {
            return null;
        }
        Node node = null;
        try {
            for (int i = 0; i < nodeNames.length; i++) {
                String nodeName = nodeNames[i];
                if (parentNode.hasNode(nodeName)) {
                    if (i == nodeNames.length - 1) {
                        node = parentNode.getNode(nodeName);
                    } else {
                        parentNode = parentNode.getNode(nodeName);
                    }
                }
            }
        } catch (RepositoryException e) {
            String msg = "Ignoring exception in safe get: " + e.getMessage();
            if (log.isDebugEnabled()) {
                log.warn(msg, e);
            } else {
                log.warn(msg);
            }
        }
        return node;
    }

    public static boolean itemNodeExists(String absPath, JcrSession session) {
        if (absPath == null || !absPath.startsWith("/")) {
            return false;
        }
        try {
            return session.itemExists(absPath);
        } catch (RepositoryRuntimeException e) {
            if (ExceptionUtils.getCauseOfTypes(e, MalformedPathException.class) != null) {
                return false;
            } else {
                throw e;
            }
        }
    }
}
