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

import com.thoughtworks.xstream.XStream;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.artifactory.ArtifactoryConstants;
import org.artifactory.fs.FolderMetadata;
import org.artifactory.fs.FsItemMetadata;
import org.artifactory.jcr.JcrCallback;
import org.artifactory.jcr.JcrPath;
import org.artifactory.jcr.JcrSessionWrapper;
import org.artifactory.jcr.JcrWrapper;
import org.artifactory.jcr.NodeLock;
import org.artifactory.process.StatusHolder;
import org.artifactory.repo.RepoPath;
import org.artifactory.security.AccessLogger;
import org.artifactory.security.ArtifactorySecurityManager;
import org.artifactory.spring.ContextHelper;

import javax.jcr.ItemExistsException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public class JcrFolder extends JcrFsItem {
    @SuppressWarnings({"UNUSED_SYMBOL", "UnusedDeclaration"})
    private final static Logger LOGGER = Logger.getLogger(JcrFolder.class);
    public static final String NT_ARTIFACTORY_FOLDER = "artifactory:folder";
    private static XStream xStreamParser;


    public JcrFolder(Node node) {
        super(node);
    }

    public JcrFolder(String absPath) {
        super(absPath);
    }

    public JcrFolder(String parent, String child) {
        super(parent, child);
    }

    public JcrFolder(File parent, String child) {
        super(parent, child);
    }

    public List<JcrFsItem> getItems() {
        List<JcrFsItem> items = new ArrayList<JcrFsItem>();
        try {
            NodeIterator nodes = getNode().getNodes();
            while (nodes.hasNext()) {
                Node node = nodes.nextNode();
                String typeName = node.getPrimaryNodeType().getName();
                if (typeName.equals(NT_ARTIFACTORY_FOLDER)) {
                    items.add(new JcrFolder(node));
                } else if (typeName.equals(JcrFile.NT_ARTIFACTORY_FILE)) {
                    items.add(new JcrFile(node));
                }
            }
        } catch (RepositoryException e) {
            throw new RuntimeException("Failed to retrieve folder node items.", e);
        }
        return items;
    }

    public void export(File targetDir, boolean abortOnError, boolean includeMetadata) {
        try {
            List<JcrFsItem> list = getItems();
            for (JcrFsItem item : list) {
                String relPath = item.getRelativePath();
                File targetFile = new File(targetDir, relPath);
                if (item.isDirectory()) {
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("Exporting directory '" + relPath + "'...");
                    }
                    boolean res = targetFile.exists() || targetFile.mkdirs();
                    if (res) {
                        long createdTime = getCreated();
                        if (createdTime >= 0) {
                            targetFile.setLastModified(createdTime);
                        }
                        JcrFolder jcrFolder = ((JcrFolder) item);
                        jcrFolder.export(targetDir, abortOnError, includeMetadata);
                        if (includeMetadata) {
                            File metadataFile = exportMetadata(targetFile, abortOnError);
                            if (createdTime >= 0) {
                                metadataFile.setLastModified(createdTime);
                            }
                        }
                    } else {
                        throw new IOException(
                                "Failed to create directory '" + targetFile.getPath() + "'.");
                    }
                } else {
                    JcrFile jcrFile = ((JcrFile) item);
                    jcrFile.export(targetFile, includeMetadata);
                }
            }
        } catch (Exception e) {
            String msg = "Failed to export to dir '" + targetDir.getPath() + "'.";
            if (abortOnError) {
                throw new RuntimeException(msg, e);
            } else {
                LOGGER.warn(msg, e);
            }
        }
    }

    /**
     * OVERIDDEN FROM FILE BEGIN
     */

    @Override
    public long lastModified() {
        return 0;
    }

    @Override
    public long length() {
        return 0;
    }

    @Override
    public String[] list() {
        return list(null);
    }

    @Override
    public String[] list(FilenameFilter filter) {
        File[] files = listFiles(filter);
        String[] paths = new String[files.length];
        for (int i = 0; i < files.length; i++) {
            File file = files[i];
            paths[i] = file.getAbsolutePath();
        }
        return paths;
    }

    @Override
    public File[] listFiles() {
        return listFiles((FilenameFilter) null);
    }

    @Override
    public File[] listFiles(FilenameFilter filter) {
        ArrayList<File> files = new ArrayList<File>();
        List<JcrFsItem> children = getItems();
        for (JcrFsItem child : children) {
            if (filter == null || filter.accept(this, child.getPath())) {
                files.add(child);
            }
        }
        return files.toArray(new File[files.size()]);
    }

    @Override
    public File[] listFiles(FileFilter filter) {
        ArrayList<File> files = new ArrayList<File>();
        List<JcrFsItem> children = getItems();
        for (JcrFsItem child : children) {
            if (filter != null && filter.accept(child)) {
                files.add(child);
            }
        }
        return files.toArray(new File[files.size()]);
    }

    @Override
    public boolean mkdir() {
        return mkdir(true);
    }

    @SuppressWarnings({"UnnecessaryLocalVariable"})
    public boolean mkdir(final boolean useLocks) {
        final JcrWrapper jcr = ContextHelper.get().getJcr();
        boolean result = jcr.doInSession(new JcrCallback<Boolean>() {
            public Boolean doInJcr(JcrSessionWrapper session) throws RepositoryException {
                String absPath = getAbsolutePath();
                int slashIdx = absPath.lastIndexOf("/");
                String dir = absPath.substring(slashIdx + 1);
                String parentPath = absPath.substring(0, slashIdx);
                Node parentNode = (Node) session.getItem(parentPath);
                if (parentNode.hasNode(dir)) {
                    //Do not lock to check if the node exists. Eventhough checking existense with no
                    //locking compromises atomicity, it results in a too-wide lock scope. Since JCR
                    //serializes readers (JCR-314) and since multi-module projects upload artifacts
                    //concurrently onto adjacent paths, this would cause a lock failure. Here we
                    //risk getting an item does not exist exception from time to time.
                    //http://www.nabble.com/Jackrabbit-Performance-Tuning---Large-%22Transaction%22---Concurrent-Access-to-Repository-tf3095196.html#a8647811
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug(
                                "Folder node already exists (no parent locking): " + absPath + ".");
                    }
                    return false;
                } else {
                    //We check node existence again, this time with locking, just before creation,
                    //since it is much more probable that the node (path) has already been created
                    //by another deployer request
                    if (useLocks) {
                        NodeLock.lock(parentNode);
                    }
                    if (parentNode.hasNode(dir)) {
                        if (LOGGER.isDebugEnabled()) {
                            LOGGER.debug(
                                    "Folder node exists (with parent locking): " + absPath + ".");
                        }
                        return false;
                    } else {
                        if (jcr.isReadOnly()) {
                            throw new RepositoryException("Cannot create dir " + dir +
                                    " with a read only session!");
                        }
                        try {
                            //Add our node
                            parentNode.addNode(dir, JcrFolder.NT_ARTIFACTORY_FOLDER);
                            String repoKey = repoKeyFromPath(absPath);
                            setRepoKey(repoKey);
                            setArtifactoryName(dir);
                            String userId = ArtifactorySecurityManager.getUsername();
                            setModifiedBy(userId);
                            if (LOGGER.isDebugEnabled()) {
                                LOGGER.debug("Created folder node: " + absPath + ".");
                            }
                            //Flush changes so that we can release the lock early
                            session.save();
                            if (useLocks) {
                                NodeLock.unlock(parentNode);
                            }
                            RepoPath repoPath = getRepoPath();
                            AccessLogger.deployed(repoPath);
                            if (LOGGER.isDebugEnabled()) {
                                LOGGER.debug("Folder node created: " + absPath + ".");
                            }
                            return true;
                        } catch (ItemExistsException e) {
                            //Sanity check
                            throw new RepositoryException("Folder node exists eventough it was " +
                                    "determined as non-exitent with parent locking!", e);
                        }
                    }
                }
            }
        });
        return result;
    }

    public boolean mkdirs() {
        return mkdirs(true);
    }

    public boolean mkdirs(boolean useLocks) {
        //Split the path and create each subdir in turn
        String absPath = getAbsolutePath();
        String repoJcrRootPath = JcrPath.get().getRepoJcrRootPath();
        boolean result = false;
        int from = 1;
        int to;
        do {
            to = absPath.indexOf("/", from);
            String subPath = to > 0 ? absPath.substring(0, to) : absPath;
            //Skip the repositories root folder (and the root itself)
            if (!subPath.equals(repoJcrRootPath)) {
                JcrFolder subFolder = new JcrFolder(subPath);
                result = subFolder.mkdir(useLocks);
            }
            from = to + 1;
        } while (to > 0);
        return result;
    }

    @Override
    public boolean setLastModified(long time) {
        return false;
    }

    /**
     * OVERIDDEN FROM FILE END
     */

    private File exportMetadata(File targetFile, boolean abortOnError)
            throws FileNotFoundException {
        try {
            FolderMetadata metadata;
            if (abortOnError) {
                // Just get metadata normaly
                metadata = getMetadata();
            } else {
                // Get metadata in the safest way possible
                String name = targetFile.getName();
                Node node = getNode();
                if (node.hasProperty(PROP_ARTIFACTORY_NAME)) {
                    name = getArtifactoryName();
                }
                String modifiedBy = "export";
                if (node.hasProperty(PROP_ARTIFACTORY_MODIFIED_BY)) {
                    modifiedBy = getModifiedBy();
                }
                metadata = new FolderMetadata(getRepoKey(), getRelativePath(), name, getCreated(),
                        modifiedBy);
            }
            File parentFile = targetFile.getParentFile();
            File metadataFile =
                    new File(parentFile,
                            targetFile.getName() + FsItemMetadata.SUFFIX);
            //Reuse the output stream
            FileOutputStream os = null;
            try {
                os = new FileOutputStream(metadataFile);
                XStream xstream = getXStreamParser();
                xstream.toXML(metadata, os);
            } finally {
                IOUtils.closeQuietly(os);
            }
            return metadataFile;
        } catch (Exception e) {
            String msg = "Failed to export metadata of '" + getRelativePath() + "'.";
            if (abortOnError) {
                throw new RuntimeException(msg, e);
            } else {
                LOGGER.warn(msg, e);
            }
            return null;
        }
    }

    private static synchronized XStream getXStreamParser() {
        if (xStreamParser == null) {
            xStreamParser = new XStream();
            xStreamParser.processAnnotations(FolderMetadata.class);
        }
        return xStreamParser;
    }

    public void exportTo(File exportDir, StatusHolder status) {
        File targetFile = new File(exportDir, getRelativePath());
        export(targetFile, true, true);
    }

    public void importFrom(String basePath, StatusHolder status) {
        FileInputStream is = null;
        try {
            //Read metadata into the node
            File file = new File(basePath, getRelativePath());
            File parentFile = file.getParentFile();
            File metadataFile = new File(parentFile, file.getName() + FsItemMetadata.SUFFIX);
            if (metadataFile.exists()) {
                LOGGER.debug("Importing metadata from '" + metadataFile.getPath() + "'.");
                //Reuse the input stream
                IOUtils.closeQuietly(is);
                is = new FileInputStream(metadataFile);
                XStream xStream = getXStreamParser();
                FolderMetadata metadata = (FolderMetadata) xStream.fromXML(is);
                String name = metadata.getArtifactoryName();
                setArtifactoryName(name != null ? name : file.getName());
                Node node = getNode();
                if (!node.hasProperty(PROP_ARTIFACTORY_REPO_KEY)) {
                    //Do not override the repo key (when importing to a repo with a different key)
                    node.setProperty(PROP_ARTIFACTORY_REPO_KEY, metadata.getRepoKey());
                }
                node.setProperty(PROP_ARTIFACTORY_MODIFIED_BY, metadata.getModifiedBy());
            } else {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("No metadata found for '" + file.getPath() + "'.");
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to file import into '" + getRelativePath() + "'.",
                    e);
        } finally {
            IOUtils.closeQuietly(is);
        }
    }

    public boolean isDirectory() {
        return true;
    }

    @Override
    public boolean delete() {
        return delete(true);
    }

    public boolean delete(boolean singleTransaction) {
        if (!singleTransaction && !ArtifactoryConstants.forceAtomicTransacions) {
            deleteChildren(singleTransaction);
        }
        return super.delete(true);
    }

    public boolean deleteChildren(boolean singleTransaction) {
        if (!exists()) {
            return false;
        }
        Node node = getNode();
        //Lock the parent to avoid possible pending changes exception on save
        NodeLock.lock(node);
        List<JcrFsItem> children = getItems();
        for (JcrFsItem child : children) {
            if (child.isDirectory()) {
                child.delete(singleTransaction);
            } else {
                child.delete();
            }
            if (!singleTransaction && !ArtifactoryConstants.forceAtomicTransacions) {
                try {
                    Session session = node.getSession();
                    session.save();
                } catch (RepositoryException e) {
                    throw new RuntimeException("Failed to save jcr session.", e);
                }
            }
        }
        return true;
    }

    public List<JcrFolder> withEmptyChildren() {
        JcrFolder parent = this;
        List<JcrFolder> result = new ArrayList<JcrFolder>();
        while (true) {
            List<JcrFsItem> children = parent.getItems();
            result.add(parent);
            if (children.size() == 1 && children.get(0).isDirectory()) {
                parent = (JcrFolder) children.get(0);
            } else {
                break;
            }
        }
        return result;
    }

    public FolderMetadata getMetadata() {
        return new FolderMetadata(getRepoKey(), getRelativePath(), getArtifactoryName(),
                getCreated(),
                getModifiedBy());
    }
}
