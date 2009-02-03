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
import org.artifactory.api.common.StatusHolder;
import org.artifactory.api.config.ExportSettings;
import org.artifactory.api.config.ImportSettings;
import org.artifactory.api.fs.FolderExtraInfo;
import org.artifactory.api.fs.FolderInfo;
import org.artifactory.api.fs.ItemInfo;
import org.artifactory.api.maven.MavenNaming;
import org.artifactory.api.repo.RepoPath;
import org.artifactory.api.repo.exception.RepositoryRuntimeException;
import org.artifactory.jcr.JcrService;
import org.artifactory.jcr.JcrSession;
import org.artifactory.jcr.lock.LockingException;
import org.artifactory.jcr.lock.LockingHelper;
import org.artifactory.jcr.md.MetadataDefinition;
import org.artifactory.repo.LocalRepo;
import org.artifactory.schedule.TaskService;
import org.artifactory.security.AccessLogger;
import org.artifactory.spring.InternalContextHelper;
import org.artifactory.utils.PathMatcher;
import org.artifactory.utils.PathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private static final Logger log = LoggerFactory.getLogger(JcrFolder.class);
    public static final String NT_ARTIFACTORY_FOLDER = "artifactory:folder";

    @Override
    protected FolderInfo createInfo(RepoPath repoPath) {
        return new FolderInfo(repoPath);
    }

    @Override
    protected FolderInfo createInfo(FolderInfo copy) {
        return new FolderInfo(copy);
    }

    /**
     * Constructor used when reading JCR content and creating JCR file system item from it. Will not create anything in
     * JCR but will read the JCR content of the node.
     *
     * @param node the JCR node this item represent
     * @param repo
     * @throws RepositoryRuntimeException if the node cannot be read
     */
    public JcrFolder(Node node, LocalRepo repo) {
        super(node, repo);
    }

    public JcrFolder(RepoPath repoPath, LocalRepo repo) {
        super(repoPath, repo);
    }

    public JcrFolder(JcrFolder copy, LocalRepo repo) {
        super(copy, repo);
    }

    @Override
    protected void setExtraInfoFields(Node node) throws RepositoryException {
        FolderExtraInfo folderExtraInfo = getXmlMetdataObject(FolderExtraInfo.class);
        if (folderExtraInfo != null) {
            getInfo().setExtension(folderExtraInfo);
        }
    }

    public List<JcrFsItem> getItems() {
        return getJcrService().getChildren(this, false);
    }

    /**
     * OVERIDDEN FROM FILE BEGIN
     */

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
        String absPath = getAbsolutePath();
        JcrService jcr = InternalContextHelper.get().getJcrService();
        JcrSession session = jcr.getManagedSession();

        try {
            boolean created = false;
            if (session.itemExists(absPath)) {
                if (log.isDebugEnabled()) {
                    log.debug("Folder node already exists: " + absPath + ".");
                }
            } else {
                if (!isMutable()) {
                    throw new LockingException("Cannot modified immutable " + this);
                }
                String parentPath = PathUtils.getParent(absPath);
                Node parentNode = (Node) session.getItem(parentPath);
                String dir = getRepoPath().getName();
                if (!PathUtils.hasText(dir)) {
                    dir = getRepoKey();
                }
                try {
                    //Add our node
                    parentNode.addNode(dir, JcrFolder.NT_ARTIFACTORY_FOLDER);
                    createMetadataContainer();
                    saveModifiedInfo();
                    if (log.isDebugEnabled()) {
                        log.debug("Created folder node: " + absPath + ".");
                    }
                    RepoPath repoPath = getRepoPath();
                    AccessLogger.deployed(repoPath);
                    if (log.isDebugEnabled()) {
                        log.debug("Folder node created: " + absPath + ".");
                    }
                    created = true;
                } catch (ItemExistsException e) {
                    log.warn(
                            "Attempt to create an already exiting node failed:" + absPath + ".");
                }
            }
            return created;
        } catch (RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    @Override
    public JcrFsItem save() {
        if (isDeleted()) {
            throw new IllegalStateException("Cannot save item " + getRepoPath() + " it is schedule for deletion");
        }
        //mkdir();
        return new JcrFolder(getNode(), getLocalRepo());
    }

    @Override
    public boolean isIdentical(JcrFsItem item) {
        if (!(item instanceof JcrFolder)) {
            return false;
        }
        return super.isIdentical(item);
    }

    @Override
    public int zap(long expiredLastUpdated) {
        int result = 0;
        List<JcrFsItem> children = getJcrService().getChildren(this, true);
        for (JcrFsItem child : children) {
            result += child.zap(expiredLastUpdated);
        }
        return result;
    }

    @Override
    public boolean mkdirs() {
        //Split the path and create each subdir in turn
        String path = getRelativePath();
        int from = 1;
        boolean result;
        int to;
        do {
            to = path.indexOf("/", from);
            String subPath = to > 0 ? path.substring(0, to) : path;
            RepoPath subRepoPath = new RepoPath(getRepoKey(), subPath);
            JcrFolder subFolder = getLocalRepo().getLockedJcrFolder(subRepoPath, true);
            result = subFolder.mkdir();
            from = to + 1;
        } while (to > 0);
        return result;
    }

    @Override
    public boolean setLastModified(long time) {
        Node node = getNode();
        return setLastModified(node, time);
    }

    /**
     * OVERIDDEN FROM FILE END
     */

    public void exportTo(ExportSettings settings, StatusHolder status) {
        try {
            TaskService taskService = InternalContextHelper.get().getTaskService();
            //Check if we need to break/pause
            boolean stop = taskService.blockIfPausedAndShouldBreak();
            if (stop) {
                status.setError("Export was stopped on " + this, log);
                return;
            }
            String relPath = getRelativePath();
            File targetDir = new File(settings.getBaseDir(), relPath);
            status.setDebug("Exporting directory '" + getAbsolutePath() + "'...", log);
            FileUtils.forceMkdir(targetDir);
            FolderInfo folderInfo = getInfo();
            long lm = folderInfo.getLastModified();
            if (lm <= 0) {
                lm = folderInfo.getCreated();
            }
            targetDir.setLastModified(lm);
            if (settings.isIncludeMetadata()) {
                exportMetadata(targetDir, lm, status);
            }
            List<JcrFsItem> list = getItems();
            for (JcrFsItem item : list) {
                //Check if we need to break/pause
                stop = taskService.blockIfPausedAndShouldBreak();
                if (stop) {
                    status.setError("Export was stopped on " + this, log);
                    return;
                }
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
                        getJcrService().exportFile(jcrFile, settings, status);
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
            status.setError(msg, e, log);
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
            status.setError("Error Import", ex, log);
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
            if (settings.isIncludeMetadata()) {
                importMetadata(folder, status, settings);
                setLastModified(getInfo().getLastModified());
            }
            saveBasicInfo();
        } catch (Exception e) {
            //Just log an error and continue
            String msg =
                    "Failed to import folder " + folder.getAbsolutePath() + " into '" + getRepoPath() + "'.";
            status.setError(msg, e, log);
        }
    }

    public void importChildren(
            ImportSettings settings, StatusHolder status, LinkedList<RepoPath> foldersToScan) {
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
                            repoKey + "'...", log);
                    foldersToScan.add(new RepoPath(getRepoPath(), fileName));
                } else if (JcrFile.isStorable(fileName)) {
                    final String msg =
                            "Importing file '" + dirEntry.getAbsolutePath() + "' into '" +
                                    repoKey + "'";
                    status.setDebug(msg + "...", log);
                    try {
                        JcrFile jcrFile = getJcrService().importFileViaWorkingCopy(this, dirEntry, settings, status);
                        if (jcrFile != null) {
                            LockingHelper.removeLockEntry(jcrFile.getRepoPath());
                        }
                    } catch (Exception e) {
                        //Just log an error and continue
                        status.setError("Error while " + msg, e, log);
                    }
                }
            }
        } catch (Exception e) {
            String msg =
                    "Failed to import folder children from '" + folder.getAbsolutePath() + "'.";
            status.setError(msg, e, log);
        }
    }

    @Override
    public boolean isDirectory() {
        return true;
    }

    @Override
    public boolean delete() {
        setDeleted(true);
        deleteChildren();
        return super.delete();
    }

    public boolean deleteChildren() {
        List<JcrFsItem> children = getJcrService().getChildren(this, true);
        for (JcrFsItem child : children) {
            child.delete();
            LockingHelper.removeLockEntry(child.getRepoPath());
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
                !MavenNaming.NEXUS_INDEX_DIR.equals(name);
    }

    public void importInternalMetadata(MetadataDefinition definition, Object md) {
        // For the moment we support only FolderInfo as transient MD
        if (definition.getMetadataName().equals(FolderInfo.ROOT) && md instanceof FolderInfo) {
            FolderInfo importedFolderInfo = (FolderInfo) md;
            FolderInfo info = getInfo();
            info.setExtension(importedFolderInfo.getExtension());
            updateTimestamps(importedFolderInfo, info);
        } else {
            throw new IllegalStateException("Metadata " + definition + " for object " + md +
                    " is not supported has transient!");
        }
    }
}
