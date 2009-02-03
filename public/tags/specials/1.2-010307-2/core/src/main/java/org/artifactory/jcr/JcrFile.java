package org.artifactory.jcr;

import org.apache.commons.io.IOUtils;
import static org.apache.jackrabbit.JcrConstants.*;
import org.apache.jackrabbit.value.DateValue;
import org.apache.log4j.Logger;
import static org.artifactory.jcr.ArtifactoryJcrConstants.*;
import org.artifactory.maven.MavenUtils;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.nodetype.NodeType;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public class JcrFile extends JcrFsItem {
    @SuppressWarnings({"UNUSED_SYMBOL", "UnusedDeclaration"})
    private final static Logger LOGGER = Logger.getLogger(JcrFile.class);

    protected static final int BUFFER_SIZE = 16384;

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

    public void createChecksumIfNeeded() {
        try {
            String name = node.getName();
            //Do not checksum on a checksum file
            if (MavenUtils.isChecksum(name)) {
                return;
            }
            String checksumName = name + ".sha1";
            //Check if there already exists a checksum
            JcrFolder parent = getParent();
            Node parentNode = parent.node;
            boolean exists = parentNode.hasNode(checksumName);
            String checksumPath = parent.absPath() + "/" + checksumName;
            if (exists) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug(checksumPath + " already exists. No need to create it.");
                }
                return;
            }
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            InputStream is = getStream();
            final byte[] buffer = new byte[BUFFER_SIZE];
            //Incrementally update the digest content from the file content
            try {
                while (true) {
                    int bytesRead = is.read(buffer, 0, buffer.length);
                    if (bytesRead == -1) {
                        break;
                    }
                    digest.update(buffer, 0, bytesRead);
                }
            } finally {
                IOUtils.closeQuietly(is);
            }
            //Compute the actual digest
            byte[] bytes = digest.digest();
            String checksum = encode(bytes);
            //Save it
            byte[] checksumAsBytes = checksum.getBytes("ISO-8859-1");
            ByteArrayInputStream bais = new ByteArrayInputStream(checksumAsBytes);
            JcrHelper.importStream(parentNode, checksumName, repoKey(), lastModifiedTime(), bais);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create checksum for resource node.", e);
        }
    }

    /**
     * Encodes a 128 bit or 160-bit byte array into a String.
     *
     * @param binaryData Array containing the digest
     * @return Encoded hex string, or null if encoding failed
     */
    protected String encode(byte[] binaryData) {
        if (binaryData.length != 16 && binaryData.length != 20) {
            int bitLength = binaryData.length * 8;
            throw new IllegalArgumentException(
                    "Unrecognised length for binary data: " + bitLength + " bits");
        }

        String retValue = "";
        for (byte aBinaryData : binaryData) {
            String t = Integer.toHexString(aBinaryData & 0xff);

            if (t.length() == 1) {
                retValue += ("0" + t);
            } else {
                retValue += t;
            }
        }
        return retValue.trim();
    }

    protected Value getPropValue(String prop) {
        try {
            return node.getProperty(prop).getValue();
        } catch (RepositoryException e) {
            throw new RuntimeException("Failed to retrieve value for property '" + prop + "'.", e);
        }
    }

    protected void setPropValue(String prop, Value value) {
        try {
            node.setProperty(prop, value);
        } catch (RepositoryException e) {
            throw new RuntimeException("Failed to set value for property '" + prop + "'.", e);
        }
    }

    public void setLastUpdatedTime(long time) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(time);
        DateValue value = new DateValue(calendar);
        setPropValue(PROP_ARTIFACTORY_LAST_UPDATED, value);
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
