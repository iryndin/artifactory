/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.artifactory.jcr.fs;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import static org.apache.jackrabbit.JcrConstants.JCR_CREATED;
import static org.apache.jackrabbit.JcrConstants.JCR_LASTMODIFIED;
import org.apache.log4j.Logger;
import org.artifactory.api.common.StatusHolder;
import org.artifactory.api.config.ImportableExportable;
import org.artifactory.api.fs.ItemInfo;
import org.artifactory.api.repo.RepoPath;
import org.artifactory.api.repo.exception.RepositoryRuntimeException;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.jcr.JcrPath;
import org.artifactory.jcr.JcrService;
import org.artifactory.jcr.JcrSession;
import org.artifactory.jcr.md.MetadataAware;
import org.artifactory.jcr.md.MetadataService;
import org.artifactory.jcr.md.MetadataValue;
import org.artifactory.security.AccessLogger;
import org.artifactory.spring.InternalContextHelper;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;

/**
 * Metadata structure:
 * <p/>
 * x.jar/art:md/art.file/
 * <p/>
 * ......................art:xml/
 * <p/>
 * ..............................art.file/
 * <p/>
 * ......................................lastModified/
 * <p/>
 * ......................jcr:content (optional)/
 * <p/>
 * ..................................some-meta-data
 * <p/>
 * x.jar/art:md/art.stats/
 * <p/>
 * ......................art:xml/
 * <p/>
 * ..............................art.stats/
 */
public abstract class JcrFsItem<T extends ItemInfo> extends File
        implements Comparable<File>, ImportableExportable, MetadataAware {
    @SuppressWarnings({"UNUSED_SYMBOL", "UnusedDeclaration"})
    private final static Logger LOGGER = Logger.getLogger(JcrFsItem.class);

    public static final String PROP_ARTIFACTORY_NAME = "artifactory:name";

    private String name;
    private String absPath;
    private RepoPath repoPath;

    private transient MetadataService mdService;

    /**
     * Simple constructor with absolute path. Does not create or read anything in JCR.
     *
     * @param absPath The absolute path of this JCR File System item
     */
    public JcrFsItem(String absPath) {
        super(removeTrailingSlashes(absPath));
        this.absPath = super.getPath().replace('\\', '/');
        setRelativePathFromRepositoryAbsPath(this.absPath);
    }

    /**
     * Simple constructor with parent and child path. Does not create or read anything in JCR.
     *
     * @param parent absolute parent path
     * @param child  a relative to parent path
     */
    public JcrFsItem(String parent, String child) {
        this(parent + "/" + child);
    }

    /**
     * Simple constructor with parent as File and child path. Does not create or read anything in
     * JCR except if parent is a JcrFolder.
     *
     * @param parent a file that will provide absolute path with parent.getAbsolutePath()
     * @param child  a relative to parent path
     */
    public JcrFsItem(File parent, String child) {
        this(parent.getAbsolutePath() + "/" + child);
    }

    /**
     * Constructor used when reading JCR content and creating JCR file system item from it. Will not
     * create anything in JCR but will read the JCR content of the node.
     *
     * @param node the JCR node this item represent
     * @throws RepositoryRuntimeException if the node cannot be read
     */
    public JcrFsItem(Node node) {
        this(getAbsPath(node));
    }

    /**
     * Create a JCR File system item under this parent node. This constructor creates the entry in
     * JCR, and so should be called from a transactional scope.
     *
     * @param parentNode The folder JCR node to create this element in
     * @param name       The name of this new element
     * @throws RepositoryRuntimeException if the parentNode cannot be read or the JCR node elements
     *                                    cannot be created
     */
    protected Node createOrGetFileNode(Node parentNode, String name) {
        try {
            lock();
            //Create the node, unless it already exists (ideally we'd remove the exiting node first,
            //but we can't until JCR-1554 is resolved
            boolean exists = parentNode.hasNode(name);
            if (exists) {
                return parentNode.getNode(name);
            }
            //Create the file node
            Node node = parentNode.addNode(name, JcrFile.NT_ARTIFACTORY_FILE);
            //Create the metadata container
            createMetadataContainer();
            return node;
        } catch (RepositoryException e) {
            throw new RepositoryRuntimeException(
                    "Failed to create node '" + absPath + "'.", e);
        }
    }

    public abstract MetadataValue lock();

    public <MD> MD getXmlMetdataObject(Class<MD> clazz) {
        return getMdService().getXmlMetadataObject(this, clazz, true);
    }

    @SuppressWarnings({"unchecked"})
    public <MD> MD getXmlMetdataObject(Class<MD> clazz, boolean createIfMissing) {
        return getMdService().getXmlMetadataObject(this, clazz, createIfMissing);
    }

    protected void setMandatoryInfoFields(T info) {
        try {
            //Set the name property for indexing and speedy searches
            Node node = getNode();
            node.setProperty(PROP_ARTIFACTORY_NAME, name);
            if (info.getCreated() == 0) {
                info.setCreated(getJcrCreated(node));
            }
        } catch (RepositoryException e) {
            throw new RepositoryRuntimeException("Saving node failed.", e);
        }
        info.setRepoKey(repoPath.getRepoKey());
        info.setRelPath(repoPath.getPath());
    }

    public final void saveBasicInfo(T info) {
        setMandatoryInfoFields(info);
        getMdService().setXmlMetadata(this, info);
    }

    public final void saveModifiedInfo(T info) {
        String userId = getAuthorizationService().currentUsername();
        info.setModifiedBy(userId);
        saveBasicInfo(info);
    }

    public List<String> getXmlMetadataNames() {
        return getMdService().getXmlMetadataNames(this);
    }

    @Override
    public String getName() {
        return name;
    }

    /**
     * Get the absolute path of the item
     */
    @Override
    public String getAbsolutePath() {
        return absPath;
    }

    /**
     * Get the relative path of the item
     */
    public String getRelativePath() {
        return repoPath.getPath();
    }

    public long getCreated() {
        return getInfo().getCreated();
    }

    public RepoPath getRepoPath() {
        return repoPath;
    }

    public String getRepoKey() {
        return repoPath.getRepoKey();
    }

    public String getModifiedBy() {
        return getInfo().getModifiedBy();
    }

    public String getRelPath() {
        return repoPath.getPath();
    }

    protected long getJcrCreated(Node node) throws RepositoryException {
        //This property is auto-populated on node creation
        if (node.hasProperty(JCR_CREATED)) {
            return node.getProperty(JCR_CREATED).getDate().getTimeInMillis();
        }
        return 0;
    }

    protected long getJcrLastModified(Node node) throws RepositoryException {
        //This property is auto-populated on node modification
        if (node.hasProperty(JCR_LASTMODIFIED)) {
            return node.getProperty(JCR_LASTMODIFIED).getDate().getTimeInMillis();
        }
        return 0;
    }

    @Override
    public boolean delete() {
        getMdService().delete(absPath);
        boolean result = getJcrService().delete(absPath);
        if (result) {
            AccessLogger.deleted(repoPath);
        }
        return result;
    }

    public JcrFolder getParentFolder() {
        try {
            Node parent = getNode().getParent();
            JcrFolder parentFolder = new JcrFolder(parent);
            return parentFolder;
        } catch (RepositoryException e) {
            throw new RepositoryRuntimeException("Failed to get node's parent folder.", e);
        }
    }

    public boolean renameTo(final String destAbsPath) {
        JcrSession session = getSession();
        String srcAbsPath = getAbsolutePath();
        session.move(srcAbsPath, destAbsPath);
        this.absPath = destAbsPath;
        setRelativePathFromRepositoryAbsPath(absPath);
        saveBasicInfo(getLockedInfo());
        return true;
    }

    /**
     * OVERIDDEN FROM FILE BEGIN
     */
    @Override
    public String getParent() {
        return getParentFolder().getAbsolutePath();
    }

    @Override
    public File getParentFile() {
        return getParentFolder();
    }

    @Override
    public boolean isAbsolute() {
        return true;
    }

    @Override
    public File getAbsoluteFile() {
        return this;
    }

    @Override
    public String getCanonicalPath() throws IOException {
        return getAbsolutePath();
    }

    @Override
    public File getCanonicalFile() throws IOException {
        return this;
    }

    @Override
    public boolean canRead() {
        return getAuthorizationService().canRead(repoPath);
    }

    @Override
    public boolean canWrite() {
        return getAuthorizationService().canDeploy(repoPath);
    }

    @Override
    public boolean exists() {
        JcrSession session = getSession();
        String absPath = getAbsolutePath();
        return session.itemExists(absPath);
    }

    @Override
    public boolean isFile() {
        return !isDirectory();
    }

    @Override
    public boolean isHidden() {
        return getName().startsWith(".");
    }

    @Override
    public boolean createNewFile() throws IOException {
        return false;
    }

    @Override
    public void deleteOnExit() {
        throw new UnsupportedOperationException("deleteOnExit() is not supported for jcr.");
    }

    @Override
    public boolean renameTo(File dest) {
        String destAbsPath = dest.getAbsolutePath();
        return renameTo(destAbsPath);
    }

    @Override
    public boolean setReadOnly() {
        return false;
    }

    public boolean setWritable(boolean writable, boolean ownerOnly) {
        return false;
    }

    public boolean setWritable(boolean writable) {
        return false;
    }

    public boolean setReadable(boolean readable, boolean ownerOnly) {
        return false;
    }

    public boolean setReadable(boolean readable) {
        return false;
    }

    public boolean setExecutable(boolean executable, boolean ownerOnly) {
        return false;
    }

    public boolean setExecutable(boolean executable) {
        return false;
    }

    public boolean canExecute() {
        return false;
    }

    public long getTotalSpace() {
        throw new UnsupportedOperationException("getTotalSpace() is not supported for jcr.");
    }

    public long getFreeSpace() {
        throw new UnsupportedOperationException("getFreeSpace() is not supported for jcr.");
    }

    public long getUsableSpace() {
        throw new UnsupportedOperationException("getUsableSpace() is not supported for jcr.");
    }

    @Override
    public int compareTo(File item) {
        return getName().compareTo(item.getName());
    }

    @Override
    public String toString() {
        return absPath;
    }

    @Override
    public String getPath() {
        return absPath;
    }

    @SuppressWarnings({"deprecation"})
    @Override
    @Deprecated
    public URL toURL() throws MalformedURLException {
        return new URL("jcr", "", absPath);
    }

    @Override
    public URI toURI() {
        try {
            return new URI("jcr", null, absPath, null);
        } catch (URISyntaxException x) {
            throw new Error(x);// Can't happen
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof JcrFsItem)) {
            return false;
        }
        JcrFsItem item = (JcrFsItem) o;
        return absPath.equals(item.absPath);
    }

    @Override
    public int hashCode() {
        return absPath.hashCode();
    }

    @Override
    public abstract long lastModified();

    @Override
    public abstract long length();

    @Override
    public abstract String[] list();

    @Override
    public abstract String[] list(FilenameFilter filter);

    @Override
    public abstract File[] listFiles();

    @Override
    public abstract File[] listFiles(FilenameFilter filter);

    @Override
    public abstract File[] listFiles(FileFilter filter);

    @Override
    public abstract boolean mkdir();

    @Override
    public abstract boolean mkdirs();

    @Override
    public abstract boolean setLastModified(long time);

    /**
     * OVERIDDEN FROM FILE END
     */

    @Override
    public abstract boolean isDirectory();

    public abstract T getInfo();

    public abstract T getLockedInfo();

    protected Node getNode() {
        JcrSession session = getSession();
        String absPath = getAbsolutePath();
        return (Node) session.getItem(absPath);
    }

    protected JcrSession getSession() {
        JcrService jcr = getJcrService();
        return jcr.getManagedSession();
    }

    protected MetadataService getMdService() {
        if (mdService == null) {
            mdService = InternalContextHelper.get().beanForType(MetadataService.class);
        }
        return mdService;
    }

    protected JcrService getJcrService() {
        // TODO: Analyze the optimization if made as a member
        JcrService jcrService = InternalContextHelper.get().getJcrService();
        return jcrService;
    }

    protected AuthorizationService getAuthorizationService() {
        // TODO: Analyze the optimization if made as a member
        return InternalContextHelper.get().getAuthorizationService();
    }

    protected static String repoKeyFromPath(String absPath) {
        String repoJcrRootPath = JcrPath.get().getRepoJcrRootPath();
        int idx = absPath.indexOf(repoJcrRootPath);
        if (idx == -1) {
            throw new IllegalArgumentException("Path '" + absPath + "' is not a repository path.");
        }
        int repoKeyEnd = absPath.indexOf("/", repoJcrRootPath.length() + 1);
        int repoKeyBegin = repoJcrRootPath.length() + 1;
        String repoKey = repoKeyEnd > 0 ? absPath.substring(repoKeyBegin, repoKeyEnd) :
                absPath.substring(repoKeyBegin);
        return repoKey;
    }

    protected static String getAbsPath(Node node) {
        try {
            return node.getPath();
        } catch (RepositoryException e) {
            throw new RepositoryRuntimeException("Failed to retrieve node's absolute path:" + node,
                    e);
        }
    }

    protected void createMetadataContainer() {
        getJcrService().getOrCreateUnstructuredNode(getNode(), NODE_ARTIFACTORY_METADATA);
    }

    public Node getMetadataContainer() {
        try {
            return getNode().getNode(NODE_ARTIFACTORY_METADATA);
        } catch (RepositoryException e) {
            throw new RepositoryRuntimeException("Failed to get metadata container.", e);
        }
    }

    /**
     * Export all metadata as the real xml content (jcr:data, including commennts etc.) into a
     * {item-name}.artifactory-metadata folder, where each metadata is named {metadata-name}.xml
     */
    protected void exportMetadata(File targetPath, long modified, StatusHolder status) {
        File metadataFolder = getMetadataContainerFolder(targetPath);
        try {
            FileUtils.forceMkdir(metadataFolder);
        } catch (IOException e) {
            throw new RuntimeException(
                    "Failed to create metadata folder '" + metadataFolder.getPath() + "'.",
                    e);
        }
        List<String> metadataNames = getXmlMetadataNames();
        for (String metadataName : metadataNames) {
            File metadataFile = new File(metadataFolder, metadataName + ".xml");
            OutputStream os = null;
            try {
                os = new BufferedOutputStream(new FileOutputStream(metadataFile));
                getMdService().writeRawXmlStream(this, metadataName, os);
            } catch (Exception e) {
                status.setError(
                        "Failed to export xml metadata from '" + metadataFile.getPath() + "'.",
                        e, LOGGER);
            } finally {
                IOUtils.closeQuietly(os);
            }
            if (modified >= 0) {
                metadataFile.setLastModified(modified);
            }
        }
    }

    protected void importMetadata(File sourcePath, StatusHolder status) {
        File metadataFolder = getMetadataContainerFolder(sourcePath);
        if (metadataFolder.exists()) {
            lock();
            //Import all the xml files within the metadata folder
            String[] metadataFileNames = metadataFolder.list(new SuffixFileFilter(".xml"));
            for (String metadataFileName : metadataFileNames) {
                File metadataFile = new File(metadataFolder, metadataFileName);
                if (metadataFile.exists() && metadataFile.isDirectory()) {
                    //Sanity chek
                    LOGGER.warn("Skipping xml metadata import from '" +
                            metadataFile.getAbsolutePath() +
                            "'. Expected a file but encountered a folder.");
                    continue;
                }
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Importing metadata from '" + metadataFile.getPath() + "'.");
                }
                int extBeginIdx = metadataFileName.lastIndexOf('.');
                assert extBeginIdx > 0;
                String metadataName = metadataFileName.substring(0, extBeginIdx);
                InputStream is = null;
                try {
                    is = new BufferedInputStream(new FileInputStream(metadataFile));
                    getMdService().importXmlMetadata(this, metadataName, is);
                } catch (Exception e) {
                    status.setError("Failed to import xml metadata from '" +
                            metadataFile.getAbsolutePath() + "'.", e, LOGGER);
                } finally {
                    IOUtils.closeQuietly(is);
                }
            }
        } else {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("No metadata found for '" + sourcePath.getPath() + "'.");
            }
        }
    }

    private static String removeTrailingSlashes(String absPath) {
        if (absPath.endsWith("/")) {
            String modifiedPath = absPath.substring(0, absPath.length() - 1);
            return removeTrailingSlashes(modifiedPath);
        }
        return absPath;
    }

    private static String removeFrontSlashes(String relPath) {
        if (relPath.startsWith("/")) {
            String modifiedPath = relPath.substring(1);
            return removeFrontSlashes(modifiedPath);
        }
        return relPath;
    }

    private void setRelativePathFromRepositoryAbsPath(String absPath) {
        String relPath;
        String repoKey;
        String repoJcrRootPath = JcrPath.get().getRepoJcrRootPath();
        if ("/".equals(repoJcrRootPath)) {
            relPath = absPath.substring(1);
            repoKey = "";
        } else {
            repoKey = repoKeyFromPath(absPath);
            relPath = removeFrontSlashes(
                    absPath.substring(repoJcrRootPath.length() + repoKey.length() + 1));
        }
        repoPath = new RepoPath(repoKey, relPath);
        int nameStart = relPath.lastIndexOf('/');
        //If there is no name it is the repository root
        this.name = nameStart > 0 ? relPath.substring(nameStart + 1) : relPath;
    }

    private static File getMetadataContainerFolder(File targetFile) {
        return new File(
                targetFile.getParentFile(), targetFile.getName() + ItemInfo.METADATA_FOLDER);
    }

    protected abstract void unlockNoSave();

    public boolean isTransient() {
        return lock().isTransient();
    }
}
