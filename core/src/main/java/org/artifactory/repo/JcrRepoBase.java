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
package org.artifactory.repo;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.artifactory.api.common.StatusHolder;
import org.artifactory.api.config.ExportSettings;
import org.artifactory.api.config.ImportSettings;
import org.artifactory.api.fs.FileInfo;
import org.artifactory.api.fs.ItemInfo;
import org.artifactory.api.repo.RepoPath;
import org.artifactory.api.repo.exception.FileExpectedException;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.common.ArtifactoryHome;
import org.artifactory.common.ResourceStreamHandle;
import org.artifactory.descriptor.repo.LocalRepoDescriptor;
import org.artifactory.descriptor.repo.SnapshotVersionBehavior;
import org.artifactory.io.SimpleResourceStreamHandle;
import org.artifactory.io.checksum.ChecksumType;
import org.artifactory.jcr.JcrPath;
import org.artifactory.jcr.JcrService;
import org.artifactory.jcr.fs.JcrFile;
import org.artifactory.jcr.fs.JcrFolder;
import org.artifactory.jcr.fs.JcrFsItem;
import org.artifactory.maven.MavenUtils;
import org.artifactory.repo.interceptor.LocalRepoInterceptor;
import org.artifactory.repo.service.InternalRepositoryService;
import org.artifactory.resource.ArtifactResource;
import org.artifactory.resource.RepoResource;
import org.artifactory.resource.SimpleRepoResource;
import org.artifactory.resource.UnfoundRepoResource;
import org.artifactory.security.AccessLogger;
import org.artifactory.spring.InternalContextHelper;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.util.LinkedList;
import java.util.List;

public abstract class JcrRepoBase<T extends LocalRepoDescriptor> extends RealRepoBase<T>
        implements LocalRepo<T> {
    private static final Logger LOGGER = Logger.getLogger(JcrRepoBase.class);

    private boolean anonAccessEnabled;
    private String tempFileRepoUrl;
    private String repoRootPath;
    private LocalRepoInterceptor localRepoInterceptor;

    protected JcrRepoBase(InternalRepositoryService repositoryService) {
        super(repositoryService);
    }

    protected JcrRepoBase(InternalRepositoryService repositoryService, T descriptor) {
        this(repositoryService);
        setDescriptor(descriptor);
    }

    @Override
    public void setDescriptor(T descriptor) {
        super.setDescriptor(descriptor);
        repoRootPath = JcrPath.get().getRepoJcrPath(getKey());
    }

    public String getUrl() {
        return tempFileRepoUrl;
    }

    public void init() {
        anonAccessEnabled = getRepositoryService().isAnonAccessEnabled();
        //Purge and recreate the (temp) repo dir
        final String key = getKey();
        File repoTmpDir = new File(ArtifactoryHome.getDataDir(), "tmp/" + key);
        try {
            tempFileRepoUrl = repoTmpDir.toURI().toURL().toString();
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Temporary directory for repo " + key +
                    " has a Malformed URL.", e);
        }
        if (repoTmpDir.exists()) {
            try {
                FileUtils.deleteDirectory(repoTmpDir);
            } catch (IOException e) {
                //Ignore
            }
        }
        boolean result = repoTmpDir.mkdirs();
        //Sanity check
        if (!result) {
            throw new IllegalArgumentException(
                    "Failed to create local repository directory: " + repoTmpDir.getAbsolutePath());
        }
        localRepoInterceptor = getRepositoryService().getLocalRepoInterceptor();
        //Create the repo node if it doesn't exist
        JcrFolder repoFolder = new JcrFolder(repoRootPath);
        repoFolder.mkdirs();
    }

    public String getRepoRootPath() {
        return repoRootPath;
    }

    public SnapshotVersionBehavior getSnapshotVersionBehavior() {
        return getDescriptor().getSnapshotVersionBehavior();
    }

    public boolean isAnonAccessEnabled() {
        return anonAccessEnabled;
    }

    public JcrFolder getRootFolder() {
        return new JcrFolder(repoRootPath);
    }

    /**
     * Given a relative path, returns the file node in the repository.
     */
    public JcrFsItem getFsItem(final String relPath) {
        JcrService jcr = InternalContextHelper.get().getJcrService();
        return jcr.getFsItem(repoRootPath, relPath);
    }

    public JcrFile getJcrFile(String relPath) throws FileExpectedException {
        JcrService jcr = InternalContextHelper.get().getJcrService();
        JcrFile jcrFile = jcr.getJcrFile(this, relPath);
        return jcrFile;
    }

    public JcrFile getLockedJcrFile(String relPath) throws FileExpectedException {
        JcrService jcr = InternalContextHelper.get().getJcrService();
        JcrFile jcrFile = jcr.getJcrFile(this, relPath);
        if (jcrFile == null) {
            return null;
        }
        jcrFile.lock();
        return jcrFile;
    }

    public FileInfo getFileInfo(String relPath) throws FileExpectedException {
        JcrService jcr = InternalContextHelper.get().getJcrService();
        return jcr.getFileInfo(this, relPath);
    }

    public RepoResource getInfo(String path) throws FileExpectedException {
        InternalRepositoryService service = getRepositoryService();
        StatusHolder statusHolder = service.assertValidPath(this, path);
        if (statusHolder.isError() || !allowsDownload(path)) {
            return new UnfoundRepoResource(this, path);
        }
        //No need to access any cache
        RepoResource artifact = service.retrieveInfo(this, path);
        return artifact;
    }

    public boolean allowsDownload(String path) {
        //Check download permissions
        if (anonAccessEnabled) {
            return true;
        }
        String parentPath;
        int parentPathEndIdx = path.lastIndexOf('/');
        if (parentPathEndIdx > 1) {
            parentPath = path.substring(0, parentPathEndIdx);
        } else {
            LOGGER.warn("Cannot determine parent path from request path '" + path + "'.");
            parentPath = path;
        }
        RepoPath repoPath = new RepoPath(getKey(), parentPath);
        AuthorizationService authService = getAuthorizationService();
        boolean canRead = authService.canRead(repoPath);
        if (!canRead) {
            LOGGER.warn("Download request for repo:path '" + repoPath +
                    "' is forbidden for user '" + authService.currentUsername() + "'.");
            AccessLogger.downloadDenied(repoPath);
        }
        return canRead;
    }

    public ResourceStreamHandle getResourceStreamHandle(final RepoResource res) throws IOException {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Transferring " + res + " directly to user from " + this);
        }
        String relPath = res.getPath();
        JcrFile file;
        try {
            file = getJcrFile(relPath);
        } catch (FileExpectedException e) {
            IOException ioEx = new IOException(
                    "Cannot extract resource stream from a folder: " + res + ".");
            ioEx.initCause(e);
            throw ioEx;
        }
        //If resource does not exist throw an IOException
        if (file == null) {
            throw new IOException(
                    "Could not get resource stream. Path not found: " + res + ".");
        }
        final InputStream is = file.getStreamForDownload();
        if (is == null) {
            throw new IOException(
                    "Could not get resource stream. Stream not found: " + file + ".");
        }
        ResourceStreamHandle handle = new SimpleResourceStreamHandle(is);
        return handle;
    }

    public String getProperty(String path) throws IOException {
        if (MavenUtils.isChecksum(path)) {
            //For checksums return the property directly
            String resourcePath = path.substring(0, path.lastIndexOf("."));
            FileInfo info = getFileInfo(resourcePath);
            if (info == null) {
                throw new IOException(
                        "Could not get resource stream. Path not found: " + resourcePath + ".");
            }
            ChecksumType checksumType = ChecksumType.forPath(path);
            if (checksumType.equals(ChecksumType.sha1)) {
                return info.getSha1();
            } else if (checksumType.equals(ChecksumType.md5)) {
                return info.getMd5();
            } else {
                LOGGER.warn("Skipping unknown checksum type: " + checksumType + ".");
                return null;
            }
        } else {
            throw new IOException("Cannot determine requested property for path '" + path + "'.");
        }
    }

    public void undeploy(final String path) {
        //TODO: [by yl] Replace with real undeploy
        /**
         * Undeploy rules:
         * jar - remove pom, all jar classifiers and update metadata
         * pom - if packaging is jar remove pom and jar, else remove pom and update metadata
         * metadata - remove pom, jar and classifiers
         * version dir - update versions metadata in containing dir
         * plugin pom - update plugins in maven-metadata.xml in the directory above
         */
        delete(path, path.length() == 0);
    }

    public Model getModel(ArtifactResource res) {
        String pom = getPomContent(res);
        if (pom == null) {
            return null;
        }
        MavenXpp3Reader reader = new MavenXpp3Reader();
        try {
            Model model = reader.read(new StringReader(pom));
            return model;
        } catch (Exception e) {
            LOGGER.warn("Failed to read pom from '" + pom + "'.", e);
            return null;
        }
    }

    public void exportTo(ExportSettings settings, StatusHolder status) {
        File dir = settings.getBaseDir();
        String msg = "Exporting repository '" + getKey() + "' to '" + dir + "'.";
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(msg);
        }
        status.setStatus(msg);
        try {
            FileUtils.forceMkdir(dir);
        } catch (IOException e) {
            throw new RuntimeException(
                    "Failed to create export directory '" + dir + "'.", e);
        }
        JcrFolder folder = new JcrFolder(repoRootPath);
        folder.exportTo(settings, status);
    }

    /**
     * Create the resource in the local repository
     *
     * @param res the destination resource definition
     * @param in  the stream to save at the location
     */
    public void saveResource(final RepoResource res, final InputStream in) throws IOException {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Saving resource '" + res + "' into repository '" + this + "'.");
        }
        try {
            //Create the resource path
            String resPath = res.getPath();
            int idx = resPath.lastIndexOf("/");
            String resDirAbsPath = repoRootPath + (idx > 0 ? "/" + resPath.substring(0, idx) : "");
            String resName = idx > 0 ? resPath.substring(idx + 1) : resPath;
            JcrFolder resFolder = new JcrFolder(resDirAbsPath);
            resFolder.mkdirs();
            long lastModified = res.getLastModified();
            BufferedInputStream bis = new BufferedInputStream(in);
            JcrService jcr = InternalContextHelper.get().getJcrService();
            JcrFile jcrFile = jcr.importStream(resFolder, resName, lastModified, bis);
            AccessLogger.deployed(res.getRepoPath());
            //If the resource has no size specified, update the size
            //(this can happen if we established the resource based on a HEAD request that failed to
            //return the content-length).
            if (!res.hasSize()) {
                long size = jcrFile.getSize();
                ((SimpleRepoResource) res).setSize(size);
            }
            localRepoInterceptor.afterResourceSave(res, this);
        } catch (Exception e) {
            //Unwrap any IOException and throw it
            Throwable cause = e.getCause();
            while (cause != null) {
                if (cause instanceof IOException) {
                    LOGGER.warn("IO error while trying to save resource '" + res.getPath() + "': " +
                            cause.getMessage());
                    throw (IOException) cause;
                }
                cause = cause.getCause();
            }
            throw new RuntimeException("Failed to save resource '" + res.getPath() + "'.", e);
        }
    }

    public String getPomContent(ArtifactResource res) {
        return getPomContent(res.getPath());
    }

    public String getPomContent(ItemInfo itemInfo) {
        if (itemInfo.isFolder()) {
            // TODO: Try to extract a POM from the folder
            throw new IllegalArgumentException(
                    "Item " + itemInfo + " is not a maven artifact with a POM");
        }
        String relPath = itemInfo.getRelPath();
        return getPomContent(relPath);
    }

    public boolean shouldProtectPathDeletion(String path) {
        //Snapshots should generally be overridable, except for unique ones
        return (!MavenUtils.isSnapshot(path) || !MavenUtils.isNonUniqueSnapshot(path)) &&
                !(MavenUtils.isChecksum(path) || MavenUtils.isMetadata(path));
    }

    private String getPomContent(String relPath) {
        String relativePath;
        if (!relPath.endsWith(".pom")) {
            File file = new File(relPath);
            String fileName = file.getName();
            int dotIdx = fileName.lastIndexOf(".");
            if (dotIdx < 0) {
                return "No content found.";
            }
            String pomFileName = fileName.substring(0, dotIdx) + ".pom";
            relativePath = new File(file.getParent(), pomFileName).getPath();
        } else {
            relativePath = relPath;
        }
        JcrFile jcrFile;
        try {
            jcrFile = getLockedJcrFile(relativePath);
        } catch (FileExpectedException e) {
            throw new RuntimeException("Cannot read a POM from a folder name " + relPath, e);
        }
        if (jcrFile != null) {
            InputStream is = null;
            try {
                is = jcrFile.getStream();
                return IOUtils.toString(is, "utf-8");
            } catch (IOException e) {
                throw new RuntimeException("Failed to read pom from '" + relativePath + "'.", e);
            } finally {
                IOUtils.closeQuietly(is);
            }
        }
        return null;
    }

    public boolean itemExists(String relPath) {
        if (relPath.length() > 0) {
            JcrService jcr = InternalContextHelper.get().getJcrService();
            return jcr.itemNodeExists(repoRootPath + "/" + relPath);
        } else {
            //The repo itself
            return true;
        }
    }

    public List<String> getChildrenNames(String relPath) {
        JcrService jcr = InternalContextHelper.get().getJcrService();
        return jcr.getChildrenNames(repoRootPath + "/" + relPath);
    }

    public boolean isCache() {
        return false;
    }

    public void delete() {
        delete("", false);
    }

    public void importFrom(ImportSettings settings, StatusHolder status) {
        File baseDir = settings.getBaseDir();
        JcrFolder repoFolder = new JcrFolder(repoRootPath);
        status.setStatus("Importing repository '" + getKey() + "' from " + baseDir + ".");
        JcrService jcr = InternalContextHelper.get().getJcrService();
        LinkedList<JcrFolder> foldersToScan = new LinkedList<JcrFolder>();
        foldersToScan.add(repoFolder);
        jcr.importFolders(foldersToScan, settings, status);
        status.setStatus("Repository '" + getKey() + "' imported from " + baseDir + ".");
    }

    /**
     * Delete a relative path
     *
     * @param path         relative path to delete
     * @param childrenOnly never remove the repository root folder itself
     */
    protected void delete(final String path, boolean childrenOnly) {
        // TODO: Find the locking system for delete?
        // Cannot use itemExists since always return true for repo itself
        JcrService jcr = InternalContextHelper.get().getJcrService();
        if (!jcr.itemNodeExists(repoRootPath + "/" + path)) {
            // Already deleted
            return;
        }

        JcrFsItem fsItem = getFsItem(path);
        if (childrenOnly && fsItem.isDirectory()) {
            //Do not delete the repo folder itself
            ((JcrFolder) fsItem).deleteChildren();
        } else {
            fsItem.delete();
        }
    }
}