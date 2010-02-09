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
import static org.apache.jackrabbit.JcrConstants.JCR_CREATED;
import static org.apache.jackrabbit.JcrConstants.JCR_LASTMODIFIED;
import org.artifactory.api.common.StatusHolder;
import org.artifactory.api.config.ImportSettings;
import org.artifactory.api.config.ImportableExportable;
import org.artifactory.api.fs.ChecksumsInfo;
import org.artifactory.api.fs.ItemAdditionalInfo;
import org.artifactory.api.fs.ItemInfo;
import org.artifactory.api.fs.MetadataInfo;
import org.artifactory.api.md.MetadataEntry;
import org.artifactory.api.md.MetadataReader;
import org.artifactory.api.repo.RepoPath;
import org.artifactory.api.repo.exception.RepositoryRuntimeException;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.jcr.JcrPath;
import org.artifactory.jcr.JcrRepoService;
import org.artifactory.jcr.JcrSession;
import org.artifactory.jcr.lock.LockingException;
import org.artifactory.jcr.md.MetadataAware;
import org.artifactory.jcr.md.MetadataDefinition;
import org.artifactory.jcr.md.MetadataDefinitionService;
import org.artifactory.jcr.md.MetadataService;
import org.artifactory.repo.LocalRepo;
import org.artifactory.repo.service.InternalRepositoryService;
import org.artifactory.spring.InternalContextHelper;
import org.artifactory.update.md.MetadataVersion;
import org.artifactory.util.PathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    private static final Logger log = LoggerFactory.getLogger(JcrFsItem.class);

    public static final String PROP_ARTIFACTORY_NAME = "artifactory:name";

    private final String absPath;
    private final T info;
    private final boolean createdFromJcr;
    private boolean mutable;
    private boolean deleted;
    private Map<MetadataDefinition, Object> metadata;

    private transient LocalRepo repo;
    private transient MetadataService mdService;
    private transient JcrRepoService jcrService;
    private transient MetadataDefinitionService mdDefService;

    public JcrFsItem(JcrFsItem<T> copy, LocalRepo repo) {
        super(copy.getPath());
        this.absPath = copy.absPath;
        this.repo = repo;
        this.createdFromJcr = copy.createdFromJcr;
        this.mutable = true;
        this.info = createInfo(copy.info);
        if (metadata != null) {
            this.metadata = new HashMap<MetadataDefinition, Object>(copy.metadata);
        } else {
            this.metadata = null;
        }
    }

    public JcrFsItem(RepoPath repoPath, LocalRepo repo) {
        super(JcrPath.get().getAbsolutePath(repoPath));
        this.repo = repo;
        this.absPath = PathUtils.formatPath(super.getPath());
        this.info = createInfo(repoPath);
        this.createdFromJcr = false;
        this.mutable = true;
    }

    /**
     * Constructor used when reading JCR content and creating JCR file system item from it. Will not create anything in
     * JCR but will read the JCR content of the node.
     *
     * @param node the JCR node this item represent
     * @param repo
     * @throws RepositoryRuntimeException if the node cannot be read
     */
    public JcrFsItem(Node node, LocalRepo repo) {
        super(repo.getAbsolutePath(node));
        this.repo = repo;
        this.absPath = PathUtils.formatPath(super.getPath());
        this.info = createInfo(JcrPath.get().getRepoPath(absPath));
        this.createdFromJcr = true;
        this.mutable = false;
        setInfoFields(node);
    }

    protected abstract T createInfo(RepoPath repoPath);

    protected abstract T createInfo(T copy);

    /**
     * Tells if created from jcr data without having to query the underlying jcr (like exists())
     */
    public boolean isCreatedFromJcr() {
        return createdFromJcr;
    }

    public boolean isMutable() {
        return mutable;
    }

    public void setMutable(boolean mutable) {
        this.mutable = mutable;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    /**
     * Create a JCR File system item under this parent node. This constructor creates the entry in JCR, and so should be
     * called from a transactional scope.
     *
     * @param parentNode The folder JCR node to create this element in
     * @param name       The name of this new element
     * @throws RepositoryRuntimeException if the parentNode cannot be read or the JCR node elements cannot be created
     */
    protected Node createOrGetFileNode(Node parentNode, String name) {
        try {
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

    public final LocalRepo getLocalRepo() {
        if (repo == null) {
            repo = InternalContextHelper.get().beanForType(InternalRepositoryService.class)
                    .localOrCachedRepositoryByKey(getRepoKey());
        }
        return repo;
    }

    public <MD> MD getXmlMetdataObject(Class<MD> clazz) {
        return getMdService().getXmlMetadataObject(this, clazz, true);
    }

    public <MD> MD getXmlMetdataObject(Class<MD> clazz, boolean createIfMissing) {
        return getMdService().getXmlMetadataObject(this, clazz, createIfMissing);
    }

    public String getXmlMetdata(String metadataName) {
        return getMdService().getXmlMetadata(this, metadataName);
    }

    public boolean hasXmlMetdata(String metadataName) {
        return getMdService().hasXmlMetdata(this, metadataName);
    }

    public final void setXmlMetadata(String metadataName, Object xstreamable) {
        getMdService().setXmlMetadata(this, xstreamable);
    }

    public final void setXmlMetadata(String metadataName, String value) {
        try {
            getMdService().setXmlMetadata(this, metadataName, new ByteArrayInputStream(value.getBytes("utf-8")));
        } catch (UnsupportedEncodingException e) {
            throw new RepositoryRuntimeException("Cannot set xml metadata.", e);
        }
    }

    protected void setInfoFields(Node node) {
        try {
            //Set the name property for indexing and speedy searches
            if (node.hasProperty(PROP_ARTIFACTORY_NAME)) {
                String artifactoryName = node.getProperty(PROP_ARTIFACTORY_NAME).getString();
                if (artifactoryName.equals(getName())) {
                    // Everyhting is OK
                } else {
                    //Oups
                    // TODO: Send an Asynch message to resave this
                    log.warn("Item " + this + " does not have a valid name " + artifactoryName);
                }
            } else {
                //Oups
                // TODO: Send an Asynch message to resave this
                log.warn("Item " + this + " does not have a name");
            }
            info.setCreated(getJcrCreated(node));
            info.setLastModified(getJcrLastModified(node));
            setAdditionalInfoFields(node);
        } catch (RepositoryException e) {
            throw new RepositoryRuntimeException("Saving node failed.", e);
        }
    }

    protected abstract void setAdditionalInfoFields(Node node) throws RepositoryException;

    protected void setMandatoryInfoFields() {
        try {
            //Set the name property for indexing and speedy searches
            Node node = getNode();
            node.setProperty(PROP_ARTIFACTORY_NAME, getName());
            if (info.getCreated() == 0) {
                info.setCreated(getJcrCreated(node));
            }
        } catch (RepositoryException e) {
            throw new RepositoryRuntimeException("Saving node failed.", e);
        }
    }

    public final void saveBasicInfo() {
        setMandatoryInfoFields();
        getMdService().setXmlMetadata(this, info.getInernalXmlInfo());
    }

    public final void saveModifiedInfo() {
        String userId = getAuthorizationService().currentUsername();
        ItemAdditionalInfo xmlInfo = info.getInernalXmlInfo();
        if (xmlInfo.getCreatedBy() == null) {
            xmlInfo.setCreatedBy(userId);
        }
        xmlInfo.setModifiedBy(userId);
        saveBasicInfo();
    }

    public List<String> getXmlMetadataNames() {
        return getMdService().getXmlMetadataNames(this);
    }

    @Override
    public String getName() {
        return info.getName();
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
        return getRepoPath().getPath();
    }

    public long getCreated() {
        return getInfo().getCreated();
    }

    public RepoPath getRepoPath() {
        return getInfo().getRepoPath();
    }

    public String getRepoKey() {
        return getRepoPath().getRepoKey();
    }

    public String getModifiedBy() {
        return getInfo().getInernalXmlInfo().getModifiedBy();
    }

    public String getCreatedBy() {
        return getInfo().getInernalXmlInfo().getCreatedBy();
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
        if (!isMutable()) {
            throw new LockingException("Cannot modify an immutable item: " + this);
        }
        deleted = true;
        boolean result = getJcrService().delete(this);
        return result;
    }

    public JcrFolder getParentFolder() {
        RepoPath parentRepoPath = getParentRepoPath();
        return getLocalRepo().getJcrFolder(parentRepoPath);
    }

    public RepoPath getParentRepoPath() {
        RepoPath myRepoPath = getRepoPath();
        RepoPath parentRepoPath = new RepoPath(myRepoPath.getRepoKey(), PathUtils.getParent(myRepoPath.getPath()));
        return parentRepoPath;
    }

    /**
     * OVERIDDEN FROM FILE BEGIN
     */
    @Override
    public String getParent() {
        return PathUtils.getParent(getAbsolutePath());
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
        return getAuthorizationService().canRead(getRepoPath());
    }

    @Override
    public boolean canWrite() {
        return getAuthorizationService().canDeploy(getRepoPath());
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
        throw new UnsupportedOperationException("renameTo() is not supported for jcr.");
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
    public final long lastModified() {
        return info.getLastModified();
    }

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

    public boolean isFolder() {
        return isDirectory();
    }

    public final T getInfo() {
        return info;
    }

    public final MetadataInfo getMetadataInfo(String metadataName) {
        return getMdService().getMetadataInfo(this, metadataName);
    }

    protected Node getNode() {
        JcrSession session = getSession();
        String absPath = getAbsolutePath();
        return (Node) session.getItem(absPath);
    }

    protected Node getParentNode() {
        JcrSession session = getSession();
        String absPath = PathUtils.getParent(getAbsolutePath());
        return (Node) session.getItem(absPath);
    }

    protected JcrSession getSession() {
        return getJcrService().getManagedSession();
    }

    protected MetadataService getMdService() {
        if (mdService == null) {
            mdService = InternalContextHelper.get().beanForType(MetadataService.class);
        }
        return mdService;
    }

    protected JcrRepoService getJcrService() {
        if (jcrService == null) {
            jcrService = InternalContextHelper.get().getJcrRepoService();
        }
        return jcrService;
    }

    protected MetadataDefinitionService getMetadataDefinitionService() {
        if (mdDefService == null) {
            mdDefService = InternalContextHelper.get().beanForType(MetadataDefinitionService.class);
        }
        return mdDefService;
    }

    protected AuthorizationService getAuthorizationService() {
        return InternalContextHelper.get().getAuthorizationService();
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
    protected void exportMetadata(File targetPath, StatusHolder status, boolean incremental) {
        File metadataFolder = getMetadataContainerFolder(targetPath);
        try {
            FileUtils.forceMkdir(metadataFolder);
        } catch (IOException e) {
            throw new RuntimeException(
                    "Failed to create metadata folder '" + metadataFolder.getPath() + "'.", e);
        }
        // Save the info manually
        saveInfoManually(status, metadataFolder, getInfo(), incremental);
        // Save all other metadata
        saveXml(status, metadataFolder, incremental);
    }

    private void saveInfoManually(StatusHolder status, File folder, ItemInfo itemInfo, boolean incremental) {
        MetadataDefinition definition = getMetadataDefinitionService().getMetadataDefinition(itemInfo.getClass());
        String metadataName = definition.getMetadataName();
        File metadataFile = new File(folder, metadataName + ".xml");
        long lastModified = itemInfo.getLastModified();
        if (incremental && isFileNewerOrEquals(metadataFile, lastModified, metadataName)) {
            // incremental export and file system is current. skip
            return;
        }
        writeFile(status, metadataFile, itemInfo, lastModified);
    }

    private void saveXml(StatusHolder status, File metadataFolder, boolean incremental) {
        File metadataFile;
        List<String> metadataNames = getXmlMetadataNames();
        for (String metadataName : metadataNames) {
            if (!hasXmlMetdata(metadataName)) {
                continue;
            }
            // add .xml prefix to all metadata files
            String fileName = metadataName + ".xml";
            metadataFile = new File(metadataFolder, fileName);
            MetadataInfo metadataInfo = getMdService().getMetadataInfo(this, metadataName);
            long lastModified = metadataInfo.getLastModified();
            if (incremental && isFileNewerOrEquals(metadataFile, lastModified, metadataName)) {
                // incremental export and file system is current. skip
                return;
            }
            writeFile(status, metadataFile, metadataName, lastModified);
        }
    }

    /**
     * @param metadataFile The metadata file to check if newer or equals (file might not exist)
     * @param lastModified The last modified timestamp of the metadata
     * @param metadataName The metadata name
     * @return True if the metadata file exists and its lsatModified is newer or equals to the input
     */
    private boolean isFileNewerOrEquals(File metadataFile, long lastModified, String metadataName) {
        if (metadataFile.exists() && lastModified <= metadataFile.lastModified()) {
            log.debug("Skipping not modified metadata {} of {}", metadataName, getPath());
            return true;
        }
        return false;
    }

    protected void writeFile(StatusHolder status, File metadataFile, Object xstreamObj, long modified) {
        if (xstreamObj == null) {
            return;
        }
        OutputStream os = null;
        try {
            os = new BufferedOutputStream(new FileOutputStream(metadataFile));
            getMetadataDefinitionService().getXstream().toXML(xstreamObj, os);
            if (modified >= 0) {
                metadataFile.setLastModified(modified);
            }
        } catch (Exception e) {
            status.setError("Failed to export xml metadata from '" + metadataFile.getPath() + "'.", e, log);
        } finally {
            IOUtils.closeQuietly(os);
        }
    }

    protected void writeFile(StatusHolder status, File metadataFile, String metadataName, long modified) {
        OutputStream os = null;
        try {
            os = new BufferedOutputStream(new FileOutputStream(metadataFile));
            getMdService().writeRawXmlStream(this, metadataName, os);
            if (modified >= 0) {
                metadataFile.setLastModified(modified);
            }
        } catch (Exception e) {
            status.setError("Failed to export xml metadata from '" + metadataFile.getPath() + "'.", e, log);
        } finally {
            IOUtils.closeQuietly(os);
        }
    }

    protected void writeChecksums(File targetPath, ChecksumsInfo checksumsInfo, String fileName, long modified) {
        File sha1 = new File(targetPath, fileName + ".sha1");
        File md5 = new File(targetPath, fileName + ".md5");
        try {
            FileUtils.writeStringToFile(sha1, checksumsInfo.getSha1(), "utf-8");
            FileUtils.writeStringToFile(md5, checksumsInfo.getMd5(), "utf-8");
            if (modified > 0) {
                sha1.setLastModified(modified);
                md5.setLastModified(modified);
            }
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    protected void importMetadata(File sourcePath, StatusHolder status, ImportSettings settings) {
        File metadataFolder = getMetadataContainerFolder(sourcePath);
        if (metadataFolder.exists()) {
            MetadataReader metadataReader = settings.getMetadataReader();
            if (metadataReader == null) {
                if (settings.getExportVersion() != null) {
                    metadataReader = MetadataVersion.findVersion(settings.getExportVersion());
                } else {
                    //try to find the version from the format of the metadata folder
                    metadataReader = MetadataVersion.findVersion(metadataFolder);
                }
                settings.setMetadataReader(metadataReader);
            }
            List<MetadataEntry> metadataEntries = metadataReader.getMetadataEntries(metadataFolder, settings, status);
            for (MetadataEntry entry : metadataEntries) {
                ByteArrayInputStream is = new ByteArrayInputStream(entry.getXmlContent().getBytes());
                getMdService().setXmlMetadata(this, entry.getMetadataName(), is, status);
            }
        } else {
            String msg = "No metadata files found for '" + sourcePath.getAbsolutePath() +
                    "' during inport into " + getRepoPath();
            if (settings.isIncludeMetadata()) {
                status.setWarning(msg, log);
            } else {
                status.setDebug(msg, log);
            }
        }
    }

    protected File getMetadataContainerFolder(File targetFile) {
        return new File(targetFile.getParentFile(), targetFile.getName() + ItemInfo.METADATA_FOLDER);
    }

    public void updateCache() {
        // TODO: Nullify all transient fields before inserting in cache
        getLocalRepo().updateCache(this);
    }

    public abstract JcrFsItem save();

    protected boolean setLastModified(Node node, long time) {
        Calendar lastModifiedCalendar = Calendar.getInstance();
        lastModifiedCalendar.setTimeInMillis(time);
        try {
            node.setProperty(JCR_LASTMODIFIED, lastModifiedCalendar);
        } catch (RepositoryException e) {
            throw new RepositoryRuntimeException("Failed to set file node's last modified time.", e);
        }
        return true;
    }

    protected void updateTimestamps(ItemInfo importedInfo, ItemInfo info) {
        long created = importedInfo.getCreated();
        if (created <= 0) {
            created = System.currentTimeMillis();
        }
        long lm = importedInfo.getLastModified();
        if (lm <= 0) {
            lm = created;
        }
        long lu = importedInfo.getInernalXmlInfo().getLastUpdated();
        if (lu <= 0) {
            lu = lm;
        }

        info.setCreated(created);
        info.setLastModified(lm);
        info.getInernalXmlInfo().setLastUpdated(lu);
    }

    /**
     * Sets the last updated value of snapshot files and folders to a value that will make them expired.
     *
     * @param expiredLastUpdated The time to set (will cause the expiration of the cached snapshots)
     * @return Number of files and folders affected
     */
    public abstract int zap(long expiredLastUpdated);

    public abstract void setLastUpdated(long lastUpdated);

    public boolean isIdentical(JcrFsItem item) {
        return absPath.equals(item.absPath) && info.isIdentical(item.info);
    }
}
