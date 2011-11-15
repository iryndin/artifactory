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

import org.artifactory.descriptor.config.CentralConfigDescriptor;
import org.artifactory.jcr.JcrService;
import org.artifactory.jcr.JcrSession;
import org.artifactory.sapi.common.RepositoryRuntimeException;
import org.artifactory.sapi.data.BinaryContent;
import org.artifactory.sapi.data.MutableBinaryContent;
import org.artifactory.sapi.data.MutableVfsNode;
import org.artifactory.sapi.data.VfsNode;
import org.artifactory.sapi.data.VfsNodeType;
import org.artifactory.spring.Reloadable;
import org.artifactory.version.CompoundVersionDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Nonnull;
import javax.jcr.Binary;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import java.io.InputStream;

/**
 * Date: 8/3/11
 * Time: 5:02 PM
 *
 * @author Fred Simon
 */
@Service
@Reloadable(beanClass = InternalVirtualFsDataService.class, initAfter = {JcrService.class})
public class VirtualFsDataServiceImpl implements InternalVirtualFsDataService {
    @Autowired
    private JcrService jcrService;

    public void init() {
    }

    public void reload(CentralConfigDescriptor oldDescriptor) {
    }

    public void destroy() {
    }

    public void convert(CompoundVersionDetails source, CompoundVersionDetails target) {
    }

    public VfsNode findByPath(String absolutePath) {
        JcrSession session = jcrService.getManagedSession();
        if (pathExists(absolutePath, session)) {
            return new VfsNodeJcrImpl(session.getNode(absolutePath));
        }
        return null;
    }

    @Override
    public boolean pathExists(String absolutePath) {
        JcrSession session = jcrService.getManagedSession();
        return pathExists(absolutePath, session);
    }

    public VfsNode findByStorageId(String storageId) {
        return new VfsNodeJcrImpl(jcrService.getManagedSession().getNodeByUUID(storageId));
    }

    public boolean delete(VfsNode node) {
        return jcrService.delete(node.absolutePath()) > 0;
    }

    @Nonnull
    public MutableVfsNode getOrCreate(String absolutePath) {
        return getOrCreate(absolutePath, VfsNodeType.UNSTRUCTURED);
    }

    @Nonnull
    public MutableVfsNode getOrCreate(String absolutePath, VfsNodeType type) {
        return JcrVfsHelper.getOrCreateVfsNode(jcrService.getManagedSession().getRootNode(), absolutePath, type);
    }

    @Nonnull
    public MutableVfsNode getOrCreate(VfsNode parent, String relPath, VfsNodeType type) {
        JcrSession session = jcrService.getManagedSession();
        Node parentNode;
        if (parent instanceof VfsNodeJcrImpl) {
            parentNode = ((VfsNodeJcrImpl) parent).getJcrNode();
        } else {
            parentNode = session.getNode(parent.absolutePath());
        }
        return JcrVfsHelper.getOrCreateVfsNode(parentNode, relPath, type);
    }

    @Nonnull
    public BinaryContent createBinary(String mimeType, String content) {
        return createBinary(mimeType, content, "utf-8");
    }

    @Nonnull
    public MutableBinaryContent createBinary(String mimeType, String content, String encoding) {
        try {
            BinaryContent vfsBinary = new BinaryContentStringImpl(mimeType, encoding, content);
            Binary jcrBinary = jcrService.getManagedSession().getValueFactory().createBinary(vfsBinary.getStream());
            return new BinaryContentJcrImpl(vfsBinary, jcrBinary);
        } catch (RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    @Nonnull
    public MutableBinaryContent createBinary(String mimeType, InputStream stream) {
        try {
            BinaryContent vfsBinary = new BinaryContentStreamImpl(mimeType, "utf-8", stream);
            Binary jcrBinary = jcrService.getManagedSession().getValueFactory().createBinary(vfsBinary.getStream());
            return new BinaryContentJcrImpl(vfsBinary, jcrBinary);
        } catch (RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    public MutableVfsNode makeMutable(VfsNode vfsNode) {
        return (MutableVfsNode) vfsNode;
    }

    private boolean pathExists(String absolutePath, JcrSession session) {
        return session.itemExists(absolutePath);
    }
}
