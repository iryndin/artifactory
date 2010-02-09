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
import org.apache.log4j.Logger;
import org.artifactory.api.common.StatusHolder;
import org.artifactory.api.config.ExportSettings;
import org.artifactory.api.config.ImportSettings;
import org.artifactory.api.fs.FolderInfo;
import org.artifactory.api.fs.ItemInfo;
import org.artifactory.api.repo.RepoPath;
import org.artifactory.api.repo.exception.RepositoryRuntimeException;
import org.artifactory.jcr.JcrPath;
import org.artifactory.jcr.JcrService;
import org.artifactory.jcr.JcrSession;
import org.artifactory.jcr.lock.SessionLockManager;
import org.artifactory.jcr.md.MetadataValue;
import org.artifactory.maven.MavenUtils;
import org.artifactory.security.AccessLogger;
import org.artifactory.spring.InternalContextHelper;
import org.artifactory.utils.PathMatcher;

import javax.jcr.ItemExistsException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public class JcrFolder extends JcrFsItem<FolderInfo> {
    private final static Logger LOGGER = Logger.getLogger(JcrFolder.class);
    public static final String NT_ARTIFACTORY_FOLDER = "artifactory:folder";

    /**
     * Constructor used when reading JCR content and creating JCR file system item from it. Will not
     * create anything in JCR but will read the JCR content of the node.
     *
     * @param node the JCR node this item represent
     * @throws RepositoryRuntimeException if the node cannot be read
     */
    public JcrFolder(Node node) {
        super(node);
    }

    /**
     * Simple constructor with absolute path. Does not create or read anything in JCR.
     *
     * @param absPath The absolute path of this JCR File System item
     */
    public JcrFolder(String absPath) {
        super(absPath);
    }

    /**
     * Simple constructor with parent and child path. Does not create or read anything in JCR.
     *
     * @param parent absolute parent path
     * @param child  a relative to parent path
     */
    public JcrFolder(String parent, String child) {
        super(parent, child);
    }

    /**
     * Simple constructor with parent as File and child path. Does not create or read anything in
     * JCR except if parent is a JcrFolder.
     *
     * @param parent a file that will provide absolute path with parent.getAbsolutePath()
     * @param child  a relative to parent path
     */
    public JcrFolder(File parent, String child) {
        super(parent, child);
    }

    @Override
    protected void unlockNoSave() {
        getMdService().unlockNoSave(FolderInfo.class, getAbsolutePath());
    }

    @Override
    public MetadataValue lock() {
        return getMdService().lockCreateIfEmpty(FolderInfo.class, getAbsolutePath());
    }

    @Override
    public FolderInfo getInfo() {
        return getMdService().getXmlMetadataObject(this, FolderInfo.class, false);
    }

    @Override
    public FolderInfo getLockedInfo() {
        return getMdService().getLockedXmlMetadataObject(this, FolderInfo.class);
    }

    public List<JcrFsItem> getItems() {
        return getJcrService().getChildren(this);
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
        JcrService jcr = InternalContextHelper.get().getJcrService();
        JcrSession session = jcr.getManagedSession();
        String absPath = getAbsolutePath();
        int slashIdx = absPath.lastIndexOf("/");
        String dir = absPath.substring(slashIdx + 1);
        String parentPath = absPath.substring(0, slashIdx);
        MetadataValue value = lock();
        Node parentNode = (Node) session.getItem(parentPath);
        try {
            boolean created = false;
            if (parentNode.hasNode(dir)) {
                SessionLockManager sessionLockManager = session.getLockManager();
                if (value.isTransient()) {
                    // Fill the Cache
                    sessionLockManager.removeEntry(value.getKey());
                    value.setNormal();
                    getInfo();
                }
                value.unlock(sessionLockManager, false);
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Folder node already exists: " + absPath + ".");
                }
            } else {
                try {
                    //Add our node
                    parentNode.addNode(dir, JcrFolder.NT_ARTIFACTORY_FOLDER);
                    createMetadataContainer();
                    FolderInfo folderInfo = getLockedInfo();
                    saveModifiedInfo(folderInfo);
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("Created folder node: " + absPath + ".");
                    }
                    RepoPath repoPath = getRepoPath();
                    AccessLogger.deployed(repoPath);
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("Folder node created: " + absPath + ".");
                    }
                    created = true;
                } catch (ItemExistsException e) {
                    LOGGER.warn(
                            "Attempt to create an already exiting node failed:" + absPath + ".");
                }
            }
            return created;
        } catch (RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    @Override
    public boolean mkdirs() {
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
                result = subFolder.mkdir();
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

    public void exportTo(ExportSettings settings, StatusHolder status) {
        try {
            String relPath = getRelativePath();
            File targetDir = new File(settings.getBaseDir(), relPath);
            status.setDebug("Exporting directory '" + getAbsolutePath() + "'...", LOGGER);
            FileUtils.forceMkdir(targetDir);
            // TODO: Make created and lastModified in ItemInfo
            // Then sync correctly with JCR props so we can use them
            long createdTime = getCreated();
            if (createdTime > 0) {
                targetDir.setLastModified(createdTime);
            } else {
                targetDir.setLastModified(getJcrCreated(getNode()));
            }
            exportMetadata(targetDir, createdTime, status);
            List<JcrFsItem> list = getItems();
            for (JcrFsItem item : list) {
                String itemName = item.getName();
                if (item.isDirectory()) {
                    if (isStorable(itemName)) {
                        JcrFolder jcrFolder = ((JcrFolder) item);
                        jcrFolder.exportTo(settings, status);
                    }
                } else {
                    //Do not export checksums
                    if (JcrFile.isStorable(itemName)) {
                        JcrFile jcrFile = ((JcrFile) item);
                        jcrFile.exportTo(settings, status);
                    }
                }
            }
        } catch (Exception e) {
            File exportDir = settings.getBaseDir();
            String msg;
            if (exportDir != null) {
                msg = "Failed to export '" + getAbsolutePath() + "' to dir '" +
                        exportDir.getPath() + "'.";
            } else {
                msg = "Failed to export '" + getAbsolutePath() + "' to a null dir";
            }
            status.setError(msg, e, LOGGER);
        }
    }

    /**
     * Shallow folder import, creating all dirs and settings the folder metadata
     *
     * @param settings
     * @param status
     */
    @SuppressWarnings({"ThrowableInstanceNeverThrown"})
    public void importFrom(ImportSettings settings, StatusHolder status) {
        File baseDir = settings.getBaseDir();
        if (baseDir == null || !baseDir.isDirectory()) {
            String message = "Cannot import null, non existent folder or non directory file '" +
                    baseDir + "'.";
            IllegalArgumentException ex = new IllegalArgumentException(message);
            status.setError("Error Import", ex, LOGGER);
            return;
        }
        File folder = new File(baseDir, getRelativePath());
        if (PathMatcher.isInDefaultExcludes(folder)) {
            //Nothing to do
            return;
        }
        //Create the folder and import its the metadata
        try {
            //First create the folder in jcr
            mkdir();
            //Read metadata into the node
            importMetadata(folder, status);
            saveBasicInfo(getLockedInfo());
        } catch (Exception e) {
            //Just log an error and continue
            String msg =
                    "Failed to import folder into '" + getRelativePath() + "'.";
            status.setError(msg, e, LOGGER);
        }
    }

    public void importChildren(
            ImportSettings settings, StatusHolder status, LinkedList<JcrFolder> foldersToScan) {
        File folder = new File(settings.getBaseDir(), getRelativePath());
        try {
            File[] dirEntries = folder.listFiles();
            for (File dirEntry : dirEntries) {
                //Do not import metadata,index folders and checksums
                if (PathMatcher.isInDefaultExcludes(dirEntry)) {
                    continue;
                }
                String fileName = dirEntry.getName();
                String repoKey = getRepoKey();
                if (dirEntry.isDirectory() && isStorable(fileName)) {
                    status.setDebug("Importing folder '" + dirEntry.getAbsolutePath() + "' into '" +
                            repoKey + "'...", LOGGER);
                    JcrFolder childFolder = new JcrFolder(this, fileName);
                    foldersToScan.add(childFolder);
                } else if (JcrFile.isStorable(fileName)) {
                    final String msg =
                            "Importing file '" + dirEntry.getAbsolutePath() + "' into '" +
                                    repoKey + "'";
                    status.setDebug(msg + "...", LOGGER);
                    JcrService jcr = InternalContextHelper.get().getJcrService();
                    try {
                        jcr.importFileViaWorkingCopy(this, dirEntry, settings, status);
                    } catch (IllegalArgumentException iae) {
                        status.setError("Error while " + msg, iae, LOGGER);
                    } catch (Exception e) {
                        //Just log an error and continue
                        status.setError("Error while " + msg, e, LOGGER);
                    }
                }
            }
        } catch (Exception e) {
            String msg =
                    "Failed to import folder children from '" + folder.getAbsolutePath() + "'.";
            status.setError(msg, e, LOGGER);
        }
    }

    @Override
    public boolean isDirectory() {
        return true;
    }

    @Override
    public boolean delete() {
        deleteChildren();
        return super.delete();
    }

    public boolean deleteChildren() {
        List<JcrFsItem> children = getItems();
        for (JcrFsItem child : children) {
            child.delete();
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

    public static boolean isStorable(String name) {
        return !name.endsWith(ItemInfo.METADATA_FOLDER) && !name.startsWith(".svn") &&
                !MavenUtils.NEXUS_INDEX_DIR.equals(name);
    }
}
