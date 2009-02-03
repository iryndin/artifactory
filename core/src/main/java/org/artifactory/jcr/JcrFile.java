package org.artifactory.jcr;

import static org.apache.jackrabbit.JcrConstants.*;
import org.apache.log4j.Logger;
import static org.artifactory.jcr.ArtifactoryJcrConstants.*;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.nodetype.NodeType;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public class JcrFile extends JcrFsItem {
    @SuppressWarnings({"UNUSED_SYMBOL", "UnusedDeclaration"})
    private final static Logger LOGGER = Logger.getLogger(JcrFile.class);

    public JcrFile(Node fileNode) {
        super(fileNode);
        //Sanity check
        try {
            if (!JcrHelper.isFileNode(fileNode)) {
                throw new RuntimeException("Node is not a file node.");
            }
        } catch (RepositoryException e) {
            throw new RuntimeException("Failed to create Jcr File.", e);
        }
    }

    /**
     * When was a cacheable file last updated
     *
     * @return
     * @throws RepositoryException
     */
    public Date lastUpdated() {
        try {
            return getPropValue(PROP_ARTIFACTORY_LAST_UPDATED).getDate().getTime();
        } catch (RepositoryException e) {
            throw new RuntimeException("Failed to retrieve node file's last updated.", e);
        }
    }

    public Date lastModified() {
        Node resourceNode = getResourceNode();
        try {
            Property prop = resourceNode.getProperty(JCR_LASTMODIFIED);
            return prop.getDate().getTime();
        } catch (RepositoryException e) {
            throw new RuntimeException("Failed to retrieve file node's last modified.", e);
        }
    }

    public long lastModifiedTime() {
        Date date = lastModified();
        return date != null ? date.getTime() : 0;
    }

    //TODO: [by yl] Include mimeType as part of RepoResource where jar, md5, sha1 will also be
    //accounted for. Get rid of org.artifactory.runtime.utils.MimeTypes
    public String mimeType() {
        Node resourceNode = getResourceNode();
        try {
            Property prop = resourceNode.getProperty(JCR_MIMETYPE);
            return prop.getString();
        } catch (RepositoryException e) {
            throw new RuntimeException("Failed to retrieve file node's myme type.", e);
        }
    }

    @SuppressWarnings({"UnnecessaryLocalVariable"})
    public InputStream getStream() {
        Node resNode = getResourceNode();
        try {
            Value attachedDataValue = resNode.getProperty(JCR_DATA).getValue();
            InputStream is = attachedDataValue.getStream();
            return is;
        } catch (RepositoryException e) {
            throw new RuntimeException("Failed to retrieve file node's data stream.", e);
        }
    }

    @SuppressWarnings({"UnnecessaryLocalVariable"})
    public long size() {
        Node resNode = getResourceNode();
        try {
            long size = resNode.getProperty(JCR_DATA).getLength();
            return size;
        } catch (RepositoryException e) {
            throw new RuntimeException("Failed to retrieve file node's size.", e);
        }
    }

    public void xmlStream(OutputStream out) {
        if (!isXmlAware()) {
            throw new RuntimeException("No xml to export.");
        }
        try {
            Node xmlNode = node.getNode(ARTIFACTORY_XML);
            Session session = xmlNode.getSession();
            String absPath = xmlNode.getPath();
            session.exportDocumentView(absPath, out, false, false);
        } catch (Exception e) {
            throw new RuntimeException("Failed to retrieve file node's xml stream.", e);
        }
    }

    public boolean isXmlAware() {
        NodeType[] mixins;
        try {
            mixins = node.getMixinNodeTypes();
        } catch (RepositoryException e) {
            throw new RuntimeException("Failed to get node mixins", e);
        }
        for (NodeType mixin : mixins) {
            if (mixin.getName().equals(MIX_ARTIFACTORY_XML_AWARE)) {
                return true;
            }
        }
        return false;
    }

    public boolean isDirectory() {
        return false;
    }

    protected Value getPropValue(String prop) {
        try {
            return node.getProperty(prop).getValue();
        } catch (RepositoryException e) {
            throw new RuntimeException("Failed to retrieve value for property '" + prop + "'.", e);
        }
    }

    @SuppressWarnings({"UnnecessaryLocalVariable"})
    private Node getResourceNode() {
        try {
            Node resNode = node.getNode(JCR_CONTENT);
            return resNode;
        } catch (RepositoryException e) {
            throw new RuntimeException("Failed to get resource node.", e);
        }
    }
}
