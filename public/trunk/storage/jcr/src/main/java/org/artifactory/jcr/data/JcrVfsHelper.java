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

package org.artifactory.jcr.data;

import org.artifactory.api.security.AuthorizationService;
import org.artifactory.checksum.ChecksumType;
import org.artifactory.io.checksum.Checksum;
import org.artifactory.jcr.spring.StorageContextHelper;
import org.artifactory.log.LoggerFactory;
import org.artifactory.sapi.common.RepositoryRuntimeException;
import org.artifactory.sapi.data.MutableVfsNode;
import org.artifactory.sapi.data.VfsNode;
import org.artifactory.sapi.data.VfsNodeFilter;
import org.artifactory.sapi.data.VfsNodeType;
import org.artifactory.sapi.data.VfsProperty;
import org.artifactory.util.PathUtils;
import org.slf4j.Logger;

import javax.annotation.Nonnull;
import javax.jcr.Node;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeType;
import java.util.Arrays;
import java.util.Calendar;

import static org.artifactory.storage.StorageConstants.PROP_ARTIFACTORY_CREATED;
import static org.artifactory.storage.StorageConstants.PROP_ARTIFACTORY_CREATED_BY;

/**
 * Date: 8/3/11
 * Time: 11:29 PM
 *
 * @author Fred Simon
 */
public abstract class JcrVfsHelper {
    private static final Logger log = LoggerFactory.getLogger(JcrVfsHelper.class);

    static final VfsNodeFilter ALWAYS_TRUE = new VfsNodeFilter() {
        public boolean accept(VfsNode node) {
            return true;
        }
    };

    @Nonnull
    public static VfsNodeType findNodeType(Node node) {
        String typeName;
        String[] mixinNames;
        try {
            typeName = node.getPrimaryNodeType().getName();
            NodeType[] mixinNodeTypes = node.getMixinNodeTypes();
            mixinNames = new String[mixinNodeTypes.length];
            for (int i = 0; i < mixinNodeTypes.length; i++) {
                NodeType mixinNodeType = mixinNodeTypes[i];
                mixinNames[i] = mixinNodeType.getName();
            }
            log.debug("Found the following mixin: " + Arrays.toString(mixinNames));
        } catch (RepositoryException e) {
            throw new RepositoryRuntimeException(
                    "Extracting type name for node " + node + " failed with: " + e.getMessage(), e);
        }
        VfsNodeType[] vfsNodeTypes = VfsNodeType.values();
        for (VfsNodeType vfsNodeType : vfsNodeTypes) {
            if (vfsNodeType.storageTypeName.equals(typeName)) {
                return vfsNodeType;
            }
        }
        throw new RepositoryRuntimeException(
                "Trying to get the type " + typeName + " for node " + node + " which is unknown!");
    }


    public static MutableVfsNode getOrCreateVfsNode(Node parentNode, String relPath, VfsNodeType type) {
        try {
            relPath = PathUtils.trimLeadingSlashes(relPath);
            Node node;
            if (parentNode.hasNode(relPath)) {
                node = parentNode.getNode(relPath);
                // TODO: Assert the type match
                return new VfsNodeJcrImpl(node);
            } else {
                node = createNode(parentNode, relPath, type);
                return new VfsNodeJcrImpl(type, node);
            }
        } catch (RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    @Nonnull
    private static Node createNode(Node parent, String relPath, VfsNodeType subNodeType) {
        Node node;
        try {
            node = parent.addNode(relPath, subNodeType.storageTypeName);
            for (String mixinName : subNodeType.mixinNames) {
                node.addMixin(mixinName);
            }
            AuthorizationService authorizationService = StorageContextHelper.get().getAuthorizationService();
            Calendar lastModified = Calendar.getInstance();
            node.setProperty(PROP_ARTIFACTORY_CREATED, lastModified);
            node.setProperty(PROP_ARTIFACTORY_CREATED_BY, authorizationService.currentUsername());
        } catch (RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
        return node;
    }

    public static VfsProperty.VfsValueType getValueTypeFromJcrType(int jcrType) {
        switch (jcrType) {
            case PropertyType.STRING:
                return VfsProperty.VfsValueType.STRING;
            case PropertyType.LONG:
                return VfsProperty.VfsValueType.LONG;
            case PropertyType.DATE:
                return VfsProperty.VfsValueType.DATE;
            default:
                return VfsProperty.VfsValueType.STRING;
        }
    }

    public static Checksum[] getChecksumsToCompute() {
        ChecksumType[] checksumTypes = ChecksumType.values();
        Checksum[] checksums = new Checksum[checksumTypes.length];
        for (int i = 0; i < checksumTypes.length; i++) {
            checksums[i] = new Checksum(checksumTypes[i]);
        }
        return checksums;
    }

}
