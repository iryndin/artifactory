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
import org.artifactory.ArtifactoryConstants;
import org.artifactory.ArtifactoryHome;
import org.artifactory.common.ResourceStreamHandle;
import org.artifactory.config.CentralConfig;
import org.artifactory.fs.FsItemMetadata;
import org.artifactory.io.SimpleResourceStreamHandle;
import org.artifactory.io.checksum.ChecksumType;
import org.artifactory.jcr.JcrCallback;
import org.artifactory.jcr.JcrPath;
import org.artifactory.jcr.JcrSessionWrapper;
import org.artifactory.jcr.JcrWrapper;
import org.artifactory.jcr.fs.JcrFile;
import org.artifactory.jcr.fs.JcrFolder;
import org.artifactory.jcr.fs.JcrFsItem;
import org.artifactory.maven.MavenUtils;
import org.artifactory.process.StatusHolder;
import org.artifactory.repo.exception.FileExpectedException;
import org.artifactory.repo.interceptor.LocalRepoInterceptor;
import org.artifactory.resource.ArtifactResource;
import org.artifactory.resource.RepoResource;
import org.artifactory.resource.SimpleRepoResource;
import org.artifactory.resource.UnfoundRepoResource;
import org.artifactory.security.AccessLogger;
import org.artifactory.security.ArtifactorySecurityManager;
import org.artifactory.spring.ArtifactoryContext;
import org.artifactory.spring.ContextHelper;
import org.springframework.security.Authentication;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.MalformedURLException;

@XmlType(name = "LocalRepoType")
public class JcrRepo extends RealRepoBase implements LocalRepo {
    private static final Logger LOGGER = Logger.getLogger(JcrRepo.class);

    @SuppressWarnings({"UnusedDeclaration"})
    @XmlElement(defaultValue = "non-unique", required = false)
    private SnapshotVersionBehavior snapshotVersionBehavior = SnapshotVersionBehavior.nonunique;

    @XmlTransient
    private boolean anonAccessEnabled;

    @XmlTransient
    private String tempFileRepoUrl;

    @XmlTransient
    private String repoPath;

    @XmlTransient
    private LocalRepoInterceptor localRepoInterceptor;

    private transient JcrWrapper jcr;

    @XmlTransient
    public String getUrl() {
        return tempFileRepoUrl;
    }

    public void init() {
        ArtifactoryContext context = ContextHelper.get();
        CentralConfig cc = context.getCentralConfig();
        anonAccessEnabled = cc.isAnonAccessEnabled();
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
        localRepoInterceptor = cc.getLocalRepoInterceptor();
        jcr = context.getJcr();
        //Create the repo node if it doesn't exist
        JcrFolder repoFolder = new JcrFolder(repoPath);
        if (!repoFolder.exists()) {
            repoFolder.mkdirs();
        }
    }

    @Override
    public void setKey(String key) {
        super.setKey(key);
        repoPath = JcrPath.get().getRepoJcrPath(key);
    }

    public String getRepoPath() {
        return repoPath;
    }

    public SnapshotVersionBehavior getSnapshotVersionBehavior() {
        return snapshotVersionBehavior;
    }

    public boolean isAnonAccessEnabled() {
        return anonAccessEnabled;
    }

    public JcrFolder getFolder() {
        return new JcrFolder(repoPath);
    }

    /**
     * Given a relative path, returns the file node in the repository.
     */
    @SuppressWarnings({"UnnecessaryLocalVariable"})
    public JcrFsItem getFsItem(final String path) {
        JcrFsItem item = jcr.doInSession(new JcrCallback<JcrFsItem>() {
            public JcrFsItem doInJcr(JcrSessionWrapper session) throws RepositoryException {
                Node repoNode = (Node) session.getItem(repoPath);
                Node node;
                if (path.length() > 0) {
                    try {
                        node = repoNode.getNode(path);
                    } catch (RepositoryException e) {
                        throw new RuntimeException("Failed to get node at '" + path + "'.", e);
                    }
                } else {
                    node = repoNode;
                }
                String typeName;
                try {
                    typeName = node.getPrimaryNodeType().getName();
                } catch (RepositoryException e) {
                    throw new RuntimeException(
                            "Failed to get the primary type for node at '" + path + "'.", e);
                }
                if (typeName.equals(JcrFolder.NT_ARTIFACTORY_FOLDER)) {
                    return new JcrFolder(node);
                } else if (typeName.equals(JcrFile.NT_ARTIFACTORY_FILE)) {
                    return new JcrFile(node);
                } else {
                    throw new RuntimeException(
                            "Did not find a file system item at '" + path + "'.");
                }
            }
        });
        return item;
    }

    @SuppressWarnings({"UnnecessaryLocalVariable"})
    public RepoResource retrieveInfo(final String path) {
        //Check if node exists (no need to recheck that it not a folder, since earlier check in
        //getInfo() would have dealt with it)
        if (itemExists(path)) {
            JcrFile file = (JcrFile) getFsItem(path);
            RepoResource localRes = new SimpleRepoResource(file);
            return localRes;
        }
        return new UnfoundRepoResource(path, this);
    }

    @SuppressWarnings({"UnnecessaryLocalVariable"})
    public RepoResource getInfo(String path) throws FileExpectedException {
        //Skip if in blackout or not accepting or cannot download
        if (isBlackedOut() || !accepts(path) || !allowsDownload(path)) {
            return new UnfoundRepoResource(path, this);
        }
        if (itemExists(path) && getFsItem(path).isDirectory()) {
            RepoPath repoPath = new RepoPath(getKey(), path);
            throw new FileExpectedException(repoPath);
        }
        //No need to access any cache
        RepoResource artifact = retrieveInfo(path);
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
        ArtifactoryContext context = ContextHelper.get();
        ArtifactorySecurityManager security = context.getSecurity();
        RepoPath repoPath = new RepoPath(getKey(), parentPath);
        boolean canRead = security.canRead(repoPath);
        if (!canRead) {
            Authentication authentication =
                    ArtifactorySecurityManager.getAuthentication();
            LOGGER.warn("Download request for repo:path '" + repoPath +
                    "' is forbidden for user '" + authentication.getName() + "'.");
            AccessLogger.downloadDenied(repoPath);
        }
        return canRead;
    }

    @SuppressWarnings({"UnnecessaryLocalVariable"})
    public ResourceStreamHandle getResourceStreamHandle(final RepoResource res) throws IOException {
        String relPath = res.getPath();
        String absPath = repoPath + "/" + relPath;
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Transferring " + absPath + " directly to user from " + this);
        }
        //If resource does not exist throw an IOException
        if (!fileNodeExists(relPath)) {
            throw new IOException(
                    "Could not get resource stream. Path not found: " + absPath + ".");
        }
        JcrFile file = (JcrFile) getFsItem(relPath);
        final InputStream is = file.getStreamForDownload();
        if (is == null) {
            throw new IOException(
                    "Could not get resource stream. Stream not found: " + absPath + ".");
        }
        ResourceStreamHandle handle = new SimpleResourceStreamHandle(is);
        return handle;
    }

    @SuppressWarnings({"UnnecessaryLocalVariable"})
    public String getProperty(String path) throws IOException {
        if (MavenUtils.isChecksum(path)) {
            //For checksums return the property directly
            String resourcePath = path.substring(0, path.lastIndexOf("."));
            if (!fileNodeExists(resourcePath)) {
                throw new IOException(
                        "Could not get resource stream. Path not found: " + resourcePath + ".");
            }
            JcrFile file = (JcrFile) getFsItem(resourcePath);
            ChecksumType checksumType = ChecksumType.forPath(path);
            String checksum = file.getChecksum(checksumType);
            return checksum;
        } else {
            throw new IOException("Cannot determine requested property for path '" + path + "'.");
        }
    }

    @SuppressWarnings({"UnnecessaryLocalVariable"})
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

    @SuppressWarnings({"UnnecessaryLocalVariable"})
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

    /*public Node getRepoJcrNode(Session session) {
        try {
            Node root = session.getRootNode();
            return root.getNode(repoPath);
        } catch (RepositoryException e) {
            throw new RuntimeException("Failed to get local repository node: " + getKey(), e);
        }
    }*/

    public void exportToDir(final File targetDir) {
        exportToDir(targetDir, false, false);
    }

    private void exportToDir(final File targetDir, final boolean abortOnError,
                             final boolean includeMetadata) {
        if (!targetDir.exists() && !targetDir.mkdirs()) {
            throw new RuntimeException(
                    "Failed to create export directory '" + targetDir + "'.");
        }
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Exporting repository '" + getKey() + "' to '" + targetDir + "'.");
        }
        JcrFolder folder = new JcrFolder(repoPath);
        folder.export(targetDir, abortOnError, includeMetadata);
    }

    public void importFromDir(
            final File sourceDir, boolean singleTransaction, boolean ignoreMissingDir) {
        if (sourceDir == null || !sourceDir.isDirectory()) {
            String message = "null, non existent folder or non directory file '" + sourceDir + "'.";
            if (ignoreMissingDir) {
                if (LOGGER.isInfoEnabled()) {
                    LOGGER.info("Skipping import of " + message);
                }
                return;
            } else {
                throw new RuntimeException("Cannot import" + message);
            }
        }
        final String basePath = sourceDir.getPath();
        if (singleTransaction && !ArtifactoryConstants.forceAtomicTransacions) {
            jcr.doInSession(new JcrCallback<JcrFolder>() {
                public JcrFolder doInJcr(JcrSessionWrapper session) throws RepositoryException {
                    JcrFolder repoFolder = new JcrFolder(repoPath);
                    try {
                        importFolder(repoFolder, sourceDir, basePath);
                    } catch (IOException e) {
                        throw new RepositoryException(
                                "Failed to import folder '" + sourceDir.getPath() + "'.", e);
                    }
                    return repoFolder;
                }
            });
        } else {
            JcrFolder repoFolder = new JcrFolder(repoPath);
            importFolderFileByFile(repoFolder, sourceDir, basePath);
        }
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
            JcrFile jcrFile = jcr.doInSession(new JcrCallback<JcrFile>() {
                @SuppressWarnings({"UnnecessaryLocalVariable"})
                public JcrFile doInJcr(JcrSessionWrapper session) throws RepositoryException {
                    //Create the resource path
                    String resPath = res.getPath();
                    int idx = resPath.lastIndexOf("/");
                    String resDirAbsPath =
                            repoPath + (idx > 0 ? "/" + resPath.substring(0, idx) : "");
                    String resName = idx > 0 ? resPath.substring(idx + 1) : resPath;
                    String repoKey = getKey();
                    JcrFolder resFolder = new JcrFolder(resDirAbsPath);
                    resFolder.mkdirs();
                    long lastModified = res.getLastModified();
                    BufferedInputStream bis = new BufferedInputStream(in);
                    jcr.importStream(resFolder, resName, repoKey, lastModified, bis);
                    AccessLogger.deployed(res.getRepoPath());
                    JcrFile resFile = new JcrFile(resFolder, resName);
                    return resFile;
                }
            });
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
                    throw (IOException) cause;
                }
                cause = cause.getCause();
            }
            throw new RuntimeException("Failed to save resource '" + res + "'.", e);
        }
    }

    public String getPomContent(ArtifactResource res) {
        String relativeDirPath = res.getDirPath();
        String relativePath;
        String name = res.getName();
        if (name.endsWith(".pom")) {
            relativePath = relativeDirPath + "/" + name;
        } else {
            relativePath = relativeDirPath + "/" + name.substring(0, name.length() - 4) + ".pom";
        }
        if (fileNodeExists(relativePath)) {
            ResourceStreamHandle handle = null;
            try {
                ArtifactResource pomRes = new ArtifactResource(this, relativePath);
                handle = getResourceStreamHandle(pomRes);
                InputStream is = handle.getInputStream();
                return IOUtils.toString(is, "UTF-8");
            } catch (IOException e) {
                throw new RuntimeException("Failed to read pom from '" + relativePath + "'.", e);
            } finally {
                if (handle != null) {
                    handle.close();
                }
            }
        }
        return null;
    }

    public boolean fileNodeExists(String relPath) {
        return itemExists(relPath) && !getFsItem(relPath).isDirectory();
    }

    @SuppressWarnings({"SimplifiableIfStatement"})
    public boolean itemExists(String relPath) {
        if (relPath.length() > 0) {
            return jcr.itemNodeExists(repoPath + "/" + relPath);
        } else {
            //The repo itself
            return true;
        }
    }

    public boolean isLocal() {
        return true;
    }

    public boolean isCache() {
        return false;
    }

    public void exportTo(File exportDir, StatusHolder status) {
        String key = getKey();
        status.setStatus("Exporting repository " + key + "...");
        File targetDir = JcrPath.get().getRepoExportDir(exportDir, key);
        exportToDir(targetDir, false, true);
    }

    public void importFrom(String basePath, StatusHolder status) {
        String key = getKey();
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Importing repository " + key + "...");
        }
        status.setStatus("Importing repository " + key + "...");
        File sourceDir = new File(basePath, key);
        importFromDir(sourceDir, false, true);
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Repository " + key + " Imported.");
        }
    }

    public void delete() {
        delete("", false);
    }

    /**
     * Delete a relative path
     *
     * @param path         relative path to delete
     * @param childrenOnly never remove the repository root folder itself
     */
    protected void delete(final String path, boolean childrenOnly) {
        // TODO: Cannot use itemExists since always return true for repo itself
        if (!jcr.itemNodeExists(repoPath + "/" + path)) {
            // Already deleted
            return;
        }
        JcrFsItem fsItem = getFsItem(path);
        if (fsItem.isDirectory()) {
            JcrFolder folder = (JcrFolder) fsItem;
            if (childrenOnly) {
                //Do not delete the repo folder itself
                folder.deleteChildren(false);
            } else {
                folder.delete(false);
            }
        } else {
            fsItem.delete();
        }
    }

    protected JcrWrapper getJcr() {
        return jcr;
    }

    private void importFolder(JcrFolder parentFolder, File directory, String basePath)
            throws RepositoryException, IOException {
        File[] dirEntries = directory.listFiles();
        for (File dirEntry : dirEntries) {
            String fileName = dirEntry.getName();
            String key = getKey();
            if (dirEntry.isDirectory()) {
                LOGGER.info("Importing folder '" + fileName + "' into '" + key + "'...");
                JcrFolder jcrFolder = new JcrFolder(parentFolder, fileName);
                //Update the metadata
                jcrFolder.importFrom(basePath, null);
                //Import recursively
                importFolder(jcrFolder, dirEntry, basePath);
            } else if (!fileName.endsWith(FsItemMetadata.SUFFIX)) {
                //Do not import artifactory metadata descriptor files
                LOGGER.info("Importing file '" + fileName + "' into '" + key + "'...");
                importFile(parentFolder, dirEntry, basePath);
            }
        }
    }

    private void importFolderFileByFile(
            final JcrFolder parentFolder, File folder, final String basePath) {
        final String key = getKey();
        File[] dirEntries = folder.listFiles();
        for (final File dirEntry : dirEntries) {
            //Never fail on file by file
            try {
                final String fileName = dirEntry.getName();
                // TODO: This piece of code is copy/pasted too much the perfect one
                // is in JcrFsItem and need to be easily accessible:
                // Like for java.io.File, you create first the JcrFolder or JcrFile and then:
                // - test pattern check
                // - test security
                // - Fill with dtata and Jcr Node
                String targetPath = parentFolder.getPath() + "/" + fileName;
                int relPathStart = targetPath.indexOf(key + "/");
                String relPath = targetPath.substring(relPathStart + key.length() + 1);
                if (!accepts(relPath)) {
                    LOGGER.warn("The repository '" + key + "' rejected the artifact '" + relPath +
                            "' due to its include/exclude patterns settings.");
                    continue;
                }
                if (dirEntry.isDirectory()) {
                    JcrFolder childFolder = jcr.doInSession(new JcrCallback<JcrFolder>() {
                        public JcrFolder doInJcr(JcrSessionWrapper session)
                                throws RepositoryException {
                            //Update the metadata
                            JcrFolder jcrFolder = new JcrFolder(parentFolder, fileName);
                            jcrFolder.mkdirs();
                            jcrFolder.importFrom(basePath, null);
                            if (LOGGER.isDebugEnabled()) {
                                LOGGER.debug(
                                        "Importing directory '" + fileName + "' into '" + key +
                                                "'...");
                            }
                            session.save();
                            return jcrFolder;
                        }
                    });
                    importFolderFileByFile(childFolder, dirEntry, basePath);
                } else if (!fileName.endsWith(FsItemMetadata.SUFFIX)) {
                    if (!handles(relPath)) {
                        LOGGER.warn(
                                "The repository '" + key + "' rejected the artifact '" + relPath +
                                        "' due to its snapshot/release handling policy.");
                        continue;
                    }
                    //Do not import artifactory metadata descriptor files
                    jcr.doInSession(new JcrCallback<String>() {
                        public String doInJcr(JcrSessionWrapper session)
                                throws RepositoryException {
                            if (LOGGER.isDebugEnabled()) {
                                LOGGER.debug(
                                        "Importing file '" + fileName + "' into '" + key + "'...");
                            }
                            try {
                                importFile(parentFolder, dirEntry, basePath);
                                session.save();
                            } catch (IOException e) {
                                throw new RuntimeException(
                                        "Failed to import file '" + dirEntry + "'.", e);
                            }
                            return null;
                        }
                    });

                }
            } catch (Exception e) {
                LOGGER.warn("Fail to import '" + dirEntry + "'.", e);
            }
        }
    }

    private void importFile(JcrFolder parentFolder, File dirEntry, String basePath)
            throws RepositoryException, IOException {
        //Do not import checksums
        if (MavenUtils.isChecksum(basePath)) {
            return;
        }
        String key = getKey();
        BufferedInputStream bis = new BufferedInputStream(new FileInputStream(dirEntry));
        JcrFile jcrFile;
        try {
            jcrFile = jcr.importStream(
                    parentFolder, dirEntry.getName(), key, dirEntry.lastModified(), bis);
        } finally {
            IOUtils.closeQuietly(bis);
        }
        //Update the metadata
        jcrFile.importFrom(basePath, null);
    }
}