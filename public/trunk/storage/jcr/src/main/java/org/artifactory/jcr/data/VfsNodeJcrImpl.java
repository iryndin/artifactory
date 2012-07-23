package org.artifactory.jcr.data;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.apache.jackrabbit.JcrConstants;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.checksum.ChecksumInfo;
import org.artifactory.checksum.ChecksumStorageHelper;
import org.artifactory.checksum.ChecksumType;
import org.artifactory.checksum.ChecksumsInfo;
import org.artifactory.jcr.spring.StorageContextHelper;
import org.artifactory.jcr.utils.JcrHelper;
import org.artifactory.log.LoggerFactory;
import org.artifactory.sapi.common.RepositoryRuntimeException;
import org.artifactory.sapi.data.BinaryContent;
import org.artifactory.sapi.data.MutableVfsNode;
import org.artifactory.sapi.data.MutableVfsProperty;
import org.artifactory.sapi.data.VfsNode;
import org.artifactory.sapi.data.VfsNodeFilter;
import org.artifactory.sapi.data.VfsNodeType;
import org.artifactory.sapi.data.VfsProperty;
import org.slf4j.Logger;

import javax.annotation.Nonnull;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.apache.jackrabbit.JcrConstants.*;
import static org.artifactory.storage.StorageConstants.PROP_ARTIFACTORY_LAST_MODIFIED;
import static org.artifactory.storage.StorageConstants.PROP_ARTIFACTORY_LAST_MODIFIED_BY;

/**
 * Date: 8/4/11
 * Time: 11:06 PM
 *
 * @author Fred Simon
 */
public class VfsNodeJcrImpl implements MutableVfsNode {
    private static final Logger log = LoggerFactory.getLogger(VfsNodeJcrImpl.class);

    private final VfsNodeType nodeType;
    private final Node jcrNode;

    public VfsNodeJcrImpl(Node jcrNode) {
        this(JcrVfsHelper.findNodeType(jcrNode), jcrNode);
    }

    public VfsNodeJcrImpl(VfsNodeType nodeType, Node jcrNode) {
        this.nodeType = nodeType;
        this.jcrNode = jcrNode;
    }

    @Override
    public String getName() {
        try {
            return jcrNode.getName();
        } catch (RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    @Override
    public String absolutePath() {
        return JcrHelper.getAbsolutePath(jcrNode);
    }

    @Override
    public String storageId() {
        return JcrHelper.getUuid(jcrNode);
    }

    @Override
    public VfsNodeType nodeType() {
        return nodeType;
    }

    @Override
    public VfsNode findSubNode(String relPath) {
        try {
            Node subNode = jcrNode.getNode(relPath);
            return new VfsNodeJcrImpl(subNode);
        } catch (Exception e) {
            String msg = "Trying to load a sub node " + relPath + " from parent " + this + " failed with: " + e.getMessage();
            if (log.isDebugEnabled()) {
                log.debug(msg, e);
            } else {
                log.info(msg);
            }
            return null;
        }
    }

    @Override
    public boolean hasChild(String relPath) {
        try {
            return jcrNode.hasNode(relPath);
        } catch (RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    @Override
    public boolean hasChildren() {
        try {
            return jcrNode.hasNodes();
        } catch (RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    @Override
    public Iterable<VfsNode> children() {
        return children(JcrVfsHelper.ALWAYS_TRUE);
    }

    @Override
    public Iterable<VfsNode> children(VfsNodeFilter filter) {
        try {
            List<VfsNode> result = Lists.newArrayList();
            NodeIterator nodeIterator = jcrNode.getNodes();
            while (nodeIterator.hasNext()) {
                Node subNode = nodeIterator.nextNode();
                VfsNodeJcrImpl child = new VfsNodeJcrImpl(subNode);
                if (filter.accept(child)) {
                    result.add(child);
                }
            }
            return result;
        } catch (Exception e) {
            String msg = "Trying to load sub nodes from parent " + this + " failed with: " + e.getMessage();
            if (log.isDebugEnabled()) {
                log.debug(msg, e);
            } else {
                log.info(msg);
            }
            return Collections.emptyList();
        }
    }

    @Override
    public boolean hasProperty(String key) {
        try {
            return jcrNode.hasProperty(key);
        } catch (RepositoryException e) {
            log.debug(e.getMessage(), e);
            return false;
        }
    }

    @Override
    public Iterable<String> propertyKeys() {
        Set<String> result = Sets.newHashSet();
        try {
            PropertyIterator properties = jcrNode.getProperties();
            while (properties.hasNext()) {
                Property property = properties.nextProperty();
                result.add(property.getName());
            }
        } catch (RepositoryException e) {
            log.error(e.getMessage(), e);
        }
        return result;
    }

    @Override
    public Iterable<VfsProperty> properties() {
        return null;
    }

    @Override
    public VfsProperty getProperty(String key) {
        try {
            return new VfsPropertyJcrImpl(jcrNode.getProperty(key));
        } catch (RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    @Override
    public boolean hasContent() {
        try {
            return jcrNode.hasNode(JcrConstants.JCR_CONTENT);
        } catch (RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    @Override
    public BinaryContent content() {
        if (!hasContent()) {
            return null;
        }
        try {
            Node resourceNode = jcrNode.getNode(JcrConstants.JCR_CONTENT);
            ChecksumsInfo checksums = new ChecksumsInfo();
            for (ChecksumType type : ChecksumType.values()) {
                String actual = getStringProperty(ChecksumStorageHelper.getActualPropName(type));
                String original = getStringProperty(ChecksumStorageHelper.getOriginalPropName(type));
                checksums.addChecksumInfo(new ChecksumInfo(type, original, actual));
            }
            String mimeType = resourceNode.getProperty(JcrConstants.JCR_MIMETYPE).getString();
            String encoding = resourceNode.getProperty(JcrConstants.JCR_ENCODING).getString();
            return new BinaryContentJcrImpl(
                    mimeType, encoding, checksums,
                    resourceNode.getProperty(JcrConstants.JCR_DATA).getBinary());
        } catch (RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    @Override
    public String getStringProperty(String key) {
        try {
            if (jcrNode.hasProperty(key)) {
                Property property = jcrNode.getProperty(key);
                if (property.isMultiple()) {
                    Value[] values = property.getValues();
                    if (values.length > 0) {
                        return values[0].getString();
                    }
                } else {
                    return property.getString();
                }
            }
        } catch (RepositoryException e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }

    @Override
    public Long getLongProperty(String key) {
        try {
            if (jcrNode.hasProperty(key)) {
                Property property = jcrNode.getProperty(key);
                if (property.isMultiple()) {
                    Value[] values = property.getValues();
                    if (values.length > 0) {
                        return values[0].getLong();
                    }
                } else {
                    return property.getLong();
                }
            }
        } catch (RepositoryException e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }

    @Override
    public Calendar getDateProperty(String key) {
        try {
            if (jcrNode.hasProperty(key)) {
                Property property = jcrNode.getProperty(key);
                if (property.isMultiple()) {
                    Value[] values = property.getValues();
                    if (values.length > 0) {
                        return values[0].getDate();
                    }
                } else {
                    return property.getDate();
                }
            }
        } catch (RepositoryException e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }

    @Override
    public Boolean getBooleanProperty(String key) {
        try {
            if (jcrNode.hasProperty(key)) {
                Property property = jcrNode.getProperty(key);
                if (property.isMultiple()) {
                    Value[] values = property.getValues();
                    if (values.length > 0) {
                        return values[0].getBoolean();
                    }
                } else {
                    return property.getBoolean();
                }
            }
        } catch (RepositoryException e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }

    @Override
    public void moveTo(VfsNode newParentNode) {
        try {
            Session session = ((VfsNodeJcrImpl) newParentNode).getJcrNode().getSession();
            session.move(jcrNode.getPath(), newParentNode.absolutePath() + "/" + jcrNode.getName());
        } catch (RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    // Mutable part

    @Override
    public void delete() {
        try {
            jcrNode.remove();
            jcrNode.getSession().save();
            //            JcrSession session = StorageContextHelper.get().getJcrService().getManagedSession();
            //            JcrHelper.delete(jcrNode.getPath(), session);
        } catch (RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    @Override
    @SuppressWarnings({"unchecked", "RedundantCast"})
    public Iterable<MutableVfsNode> mutableChildren() {
        return (Iterable<MutableVfsNode>) ((List) children());
    }

    @Override
    @Nonnull
    public MutableVfsNode getOrCreateSubNode(String relPath, VfsNodeType subNodeType) {
        return JcrVfsHelper.getOrCreateVfsNode(jcrNode, relPath, subNodeType);
    }

    @Override
    @Nonnull
    public MutableVfsProperty setProperty(String key, String... values) {
        if (values.length == 0) {
            throw new IllegalArgumentException(
                    "Cannot set property " + key + " on node " + this + " without any value");
        }
        try {
            if (values.length == 1) {
                return new VfsPropertyJcrImpl(jcrNode.setProperty(key, values[0]),
                        VfsProperty.VfsValueType.STRING, VfsProperty.VfsPropertyType.AUTO);
            } else {
                return new VfsPropertyJcrImpl(jcrNode.setProperty(key, values),
                        VfsProperty.VfsValueType.STRING, VfsProperty.VfsPropertyType.MULTI_VALUE);
            }
        } catch (RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    @Override
    @Nonnull
    public MutableVfsProperty setProperty(String key, Long value) {
        try {
            return new VfsPropertyJcrImpl(jcrNode.setProperty(key, value),
                    VfsProperty.VfsValueType.LONG, VfsProperty.VfsPropertyType.SINGLE);
        } catch (RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    @Override
    @Nonnull
    public MutableVfsProperty setProperty(String key, Calendar value) {
        try {
            return new VfsPropertyJcrImpl(jcrNode.setProperty(key, value),
                    VfsProperty.VfsValueType.DATE, VfsProperty.VfsPropertyType.SINGLE);
        } catch (RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    @Override
    public void setContent(BinaryContent content) {
        try {
            Node resourceNode;
            if (jcrNode.hasNode(JCR_CONTENT)) {
                resourceNode = jcrNode.getNode(JCR_CONTENT);
            } else {
                resourceNode = jcrNode.addNode(JCR_CONTENT, NT_RESOURCE);
            }
            resourceNode.setProperty(JCR_MIMETYPE, content.getMimeType());
            resourceNode.setProperty(JCR_ENCODING, content.getEncoding());
            BinaryContentJcrImpl binaryContent;
            if (content instanceof BinaryContentJcrImpl) {
                binaryContent = ((BinaryContentJcrImpl) content);
            } else {
                //Do this after xml import: since Jackrabbit 1.4
                //org.apache.jackrabbit.core.value.BLOBInTempFile.BLOBInTempFile will close the stream
                binaryContent = new BinaryContentJcrImpl(content,
                        resourceNode.getSession().getValueFactory().createBinary(content.getStream()));
            }
            resourceNode.setProperty(JCR_DATA, binaryContent.getBinary());
            // make sure the stream is closed. Jackrabbit doesn't close the stream if the file is small (violating the contract)
            content.checkClosed();
            ChecksumsInfo checksums = content.getChecksums();
            if (checksums.getChecksumInfo(ChecksumType.sha1) == null) {
                for (ChecksumType type : ChecksumType.values()) {
                    ChecksumInfo checksumInfo = checksums.getChecksumInfo(type);
                    String actual = checksumInfo != null ? checksumInfo.getActual() : null;
                    jcrNode.setProperty(ChecksumStorageHelper.getActualPropName(type), actual);
                }
            }
            setDirty();
        } catch (RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    void setDirty() throws RepositoryException {
        AuthorizationService authorizationService = StorageContextHelper.get().getAuthorizationService();
        Calendar lastModified = Calendar.getInstance();
        jcrNode.setProperty(PROP_ARTIFACTORY_LAST_MODIFIED, lastModified);
        jcrNode.setProperty(PROP_ARTIFACTORY_LAST_MODIFIED_BY, authorizationService.currentUsername());
    }

    public Node getJcrNode() {
        return jcrNode;
    }

    @Override
    public String toString() {
        return "VfsNodeJcrImpl{" +
                "nodeType=" + nodeType +
                ", absolutePath=" + absolutePath() +
                '}';
    }
}
